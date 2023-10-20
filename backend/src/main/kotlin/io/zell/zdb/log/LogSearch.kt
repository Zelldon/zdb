/*
 * Copyright © 2021 Christopher Kujawa (zelldon91@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zell.zdb.log

import io.zell.zdb.log.records.ApplicationRecord
import io.zell.zdb.log.records.PersistedRecord
import io.zell.zdb.log.records.Record
import java.nio.file.Path

class LogSearch(logPath: Path) {


    private val reader: LogContentReader = LogContentReader(logPath)

    fun searchPosition(position: Long): Record? {
        if (position <= 0) {
            return null
        }

        reader.seekToPosition(position);

        while (reader.hasNext()) {
            val entry = reader.next()

            if (entry is ApplicationRecord) {
                if (entry.lowestPosition > position) {
                    // nothing can be found in this entry and the entries afterwards
                    return null;
                } else if (entry.highestPosition < position) {
                    // nothing in this batch will match with the search position, check the next
                    continue
                } else {
                    // here there might be the position
                    entry.entries.forEach {
                        if (it.position == position) {
                            // found!
                            return it;
                        }
                    }
                }
            }
        }
        // nothing found too bad
        return null
    }

    fun searchIndex(index: Long): PersistedRecord? {
        if (index <= 0) {
            return null
        }

        reader.seekToIndex(index)

        while (reader.hasNext()) {
            val entry = reader.next()

            if (entry.index() == index) {
                return entry
            }
        }

        return null
    }

}
