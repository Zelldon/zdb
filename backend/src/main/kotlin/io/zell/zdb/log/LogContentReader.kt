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

import io.atomix.raft.storage.log.IndexedRaftLogEntry
import io.atomix.raft.storage.log.RaftLogReader
import io.atomix.raft.storage.log.entry.SerializedApplicationEntry
import io.camunda.zeebe.logstreams.impl.log.LoggedEventImpl
import io.camunda.zeebe.protocol.impl.record.RecordMetadata
import org.agrona.concurrent.UnsafeBuffer
import java.nio.file.Path

class LogContentReader(logPath: Path) : Iterator<PersistedRecord> {

    private val reader: RaftLogReader = LogFactory.newReader(logPath)

    override fun hasNext(): Boolean {
        return reader.hasNext();
    }

    override fun next(): PersistedRecord {
        val next = reader.next()
        return convertToPersistedRecord(next)
    }

    private fun convertToPersistedRecord(
        entry: IndexedRaftLogEntry,
    ) : PersistedRecord {
        if (entry.isApplicationEntry) {
            val applicationEntry = entry.applicationEntry as SerializedApplicationEntry
            val applicationRecord = ApplicationRecord(entry.index(), entry.term(),
                applicationEntry.highestPosition, applicationEntry.lowestPosition)

            val readBuffer = UnsafeBuffer(applicationEntry.data());

            var offset = 0;
            do {
                val loggedEvent = LoggedEventImpl();
                val metadata = RecordMetadata();

                loggedEvent.wrap(readBuffer, offset)
                loggedEvent.readMetadata(metadata)

                val typedEvent = convertToTypedEvent(loggedEvent, metadata)

                applicationRecord.entries.add(typedEvent)

                offset += loggedEvent.getLength();
            } while (offset < readBuffer.capacity());
            return applicationRecord
        } else {
            return RaftRecord(entry.index(), entry.term())
        }
    }

    fun readAll(): LogContent {
        val logContent = LogContent()
        this.forEach {
            logContent.records.add(it)
        }
        return logContent
    }

    fun seekToPosition(position: Long) {
        reader.seekToAsqn(position);
    }

    fun seekToIndex(index: Long) {
        reader.seek(index)
    }
}
