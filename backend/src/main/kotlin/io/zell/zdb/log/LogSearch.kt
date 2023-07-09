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

import io.atomix.raft.storage.log.RaftLogReader
import io.atomix.raft.storage.log.entry.SerializedApplicationEntry
import io.camunda.zeebe.logstreams.impl.log.LoggedEventImpl
import io.camunda.zeebe.protocol.impl.record.RecordMetadata
import io.camunda.zeebe.protocol.record.Record
import org.agrona.concurrent.UnsafeBuffer
import java.nio.file.Path

class LogSearch (logPath: Path) {


    private val reader: RaftLogReader = LogFactory.newReader(logPath)

    fun searchPosition(position: Long) : Record<*>? {
        if (position <= 0) {
            return null
        }

        reader.seekToAsqn(position);

        if (reader.hasNext()) {
            val entry = reader.next()

            if (entry.isApplicationEntry) {
                val applicationEntry = entry.applicationEntry as SerializedApplicationEntry

                val readBuffer = UnsafeBuffer(applicationEntry.data());
                val loggedEvent = LoggedEventImpl();
                val metadata = RecordMetadata();

                var offset = 0;
                do {
                    loggedEvent.wrap(readBuffer, offset)

                    if (loggedEvent.position == position) {
                        loggedEvent.readMetadata(metadata)
                        return convertToTypedEvent(loggedEvent, metadata)
                    }
                    offset += loggedEvent.getLength();
                } while (offset < readBuffer.capacity());
            }
        }

        return null
    }

    fun searchIndex(index: Long): LogContent? {
        if (index <= 0) {
            return null
        }

        reader.seek(index);

        if (reader.hasNext()) {
            val logContent = LogContent()
            val entry = reader.next()

            LogContent.addEntryToContent(entry, logContent)
            return logContent
        }
        return null
    }

}
