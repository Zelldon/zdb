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
import io.atomix.raft.storage.log.entry.SerializedApplicationEntry
import io.camunda.zeebe.logstreams.impl.log.LoggedEventImpl
import io.camunda.zeebe.protocol.impl.record.RecordMetadata
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord
import io.camunda.zeebe.protocol.record.ValueType
import io.camunda.zeebe.stream.api.records.TypedRecord
import io.camunda.zeebe.stream.impl.records.TypedRecordImpl
import org.agrona.concurrent.UnsafeBuffer

class LogContent {

    companion object {
        fun addEntryToContent(
            entry: IndexedRaftLogEntry,
            logContent: LogContent
        ) {
            if (entry.isApplicationEntry) {
                val applicationRecord = ApplicationRecord(entry.index(), entry.term())
                val applicationEntry = entry.applicationEntry as SerializedApplicationEntry

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
                logContent.records.add(applicationRecord)
            } else {
                val raftRecord = RaftRecord(entry.index(), entry.term())
                logContent.records.add(raftRecord)
            }
        }
    }

    val records = mutableListOf<PersistedRecord>()

    override fun toString(): String {
        return "{ \"records\": [${records.joinToString()}] } "
    }

    fun asDotFile(): String {
        val content = StringBuilder("digraph log {")
            .append(System.lineSeparator())
            .append("rankdir=\"RL\"")
            .append(";")
            .append(System.lineSeparator())

        records
            .filterIsInstance<ApplicationRecord>()
            .flatMap { it.entries }
            .forEach{
                addEventAsDotNode(it, content)
            }
        content.append(System.lineSeparator())
            .append("}")
        return content.toString()
    }

    private fun addEventAsDotNode(
        entry: TypedRecordImpl,
        content: java.lang.StringBuilder
    ) {
        content.append(entry.position)
            .append(" [label=\"")
            .append("\\n").append(entry.recordType)
            .append("\\n").append(entry.valueType.name)
            .append("\\n").append(entry.intent.name())

        if (entry.valueType == ValueType.PROCESS_INSTANCE) {
            val processInstanceRecord = entry as TypedRecord<ProcessInstanceRecord>
            val processInstanceValue = processInstanceRecord.value
            content.append("\\n").append(processInstanceValue.bpmnElementType)
            content.append("\\nPI Key: ").append(processInstanceValue.processInstanceKey)
            content.append("\\nPD Key: ").append(processInstanceValue.processDefinitionKey)
        }

        content
            .append("\\nKey: ").append(entry.key)
            .append("\"]")
            .append(";")
            .append(System.lineSeparator())
        if (entry.sourceRecordPosition != -1L) {
            content.append(entry.position)
                .append(" -> ")
                .append(entry.sourceRecordPosition)
                .append(";")
                .append(System.lineSeparator())
        }
    }
}
