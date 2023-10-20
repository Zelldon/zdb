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

import io.atomix.raft.storage.log.entry.SerializedApplicationEntry
import io.camunda.zeebe.logstreams.impl.log.LoggedEventImpl
import io.camunda.zeebe.protocol.impl.encoding.MsgPackConverter
import io.camunda.zeebe.protocol.impl.record.RecordMetadata
import io.zell.zdb.log.records.*
import kotlinx.serialization.json.Json
import org.agrona.concurrent.UnsafeBuffer
import java.nio.file.Path
import kotlin.streams.asStream

class LogContentReader(logPath: Path) : Iterator<PersistedRecord> {

    private val json = Json { ignoreUnknownKeys = true }
    private val reader: RaftLogReader = LogFactory.newReader(logPath)
    private var isInLimit: (PersistedRecord) -> Boolean = { true }
    private var applicationRecordFilter: ((ApplicationRecord) -> Boolean)? = null
    private lateinit var next: PersistedRecord

    override fun hasNext(): Boolean {
        val hasNext = reader.hasNext()

        if (!hasNext) {
            return false
        }

        next = convertToPersistedRecord(reader.next())
        if (!isInLimit(next)) {
            return false
        }

        if (applicationRecordFilter == null) {
            return true
        }

        return applyFiltering()
    }

    private fun applyFiltering(): Boolean {
        // when application record filter is given, we don't want to see RaftLogRecords
        // they are filtered out implicitly here as well
        val filterMatches = next is ApplicationRecord && applicationRecordFilter!!(next as ApplicationRecord)

        if (filterMatches) {
            return true
        }

        // we want to skip this record, since it doesn't apply to our filter
        // we need to find the next matching record
        var foundNext = false
        while (!foundNext && reader.hasNext()) {
            next = convertToPersistedRecord(reader.next())
            if (!isInLimit(next)) {
                break
            }

            if (next is ApplicationRecord) {
                foundNext = applicationRecordFilter!!(next as ApplicationRecord)
            }
        }
        return foundNext;
    }

    override fun next(): PersistedRecord {
        return next
    }


    private fun convertToPersistedRecord(
        entry: IndexedRaftLogEntryImpl,
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

                val valueJson = MsgPackConverter.convertToJson(
                    UnsafeBuffer(loggedEvent.valueBuffer, loggedEvent.valueOffset, loggedEvent.valueLength))

               val parsedRecord = Record(
                    loggedEvent.position,
                    loggedEvent.sourceEventPosition,
                    loggedEvent.timestamp,
                    loggedEvent.key,
                    metadata.recordType,
                    metadata.valueType,
                    metadata.intent,
                    RecordValue(valueJson,
                        json.decodeFromString<ProcessInstanceRelatedValue>(valueJson) )
                    )

                applicationRecord.entries.add(parsedRecord)

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

    fun limitToPosition(toPosition: Long) {
        isInLimit = { record: PersistedRecord ->
            record is RaftRecord || (record is ApplicationRecord && record.lowestPosition < toPosition)
        }
    }

    fun filterForProcessInstance(instanceKey : Long) {
        applicationRecordFilter = {
            record : ApplicationRecord ->
                record.entries.asSequence()
                    .map { it.recordValue.piRelatedValue }
                    .filter { it.processInstanceKey != null }
                    .asStream()
                    .map { it.processInstanceKey }
                    .anyMatch(instanceKey::equals)
        }
    }
}
