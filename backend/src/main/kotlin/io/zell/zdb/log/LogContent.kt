package io.zell.zdb.log

import io.atomix.raft.storage.log.IndexedRaftLogEntry
import io.camunda.zeebe.engine.processing.streamprocessor.*
import io.camunda.zeebe.logstreams.impl.log.LoggedEventImpl
import io.camunda.zeebe.protocol.impl.record.RecordMetadata
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord
import io.camunda.zeebe.protocol.record.ValueType
import io.camunda.zeebe.util.ReflectUtil
import org.agrona.concurrent.UnsafeBuffer

class LogContent {

    companion object {
        fun addEntryToContent(
            entry: IndexedRaftLogEntry,
            logContent: LogContent
        ) {
            if (entry.isApplicationEntry) {
                val applicationRecord = ApplicationRecord(entry.index(), entry.term())
                val applicationEntry = entry.applicationEntry

                val readBuffer = UnsafeBuffer(applicationEntry.data());

                var offset = 0;
                do {
                    val loggedEvent = LoggedEventImpl();
                    val metadata = RecordMetadata();

                    loggedEvent.wrap(readBuffer, offset)
                    loggedEvent.readMetadata(metadata)

                    val unifiedRecordValue =
                        ReflectUtil.newInstance(TypedEventRegistry.EVENT_REGISTRY.get(metadata.getValueType()))
                    loggedEvent.readValue(unifiedRecordValue)

                    val typedEvent = TypedEventImpl(1)
                    typedEvent.wrap(loggedEvent, metadata, unifiedRecordValue)

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
        val content = StringBuilder("digraph log {\n")
            .append("rankdir=\"RL\"")
            .append(";\n")

        for (record in records) {
            if (record is ApplicationRecord) {
                for (entry in record.entries) {

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
                        .append("\"]").append(";\n")
                    if (entry.sourceRecordPosition != -1L) {
                        content.append(entry.position).append(" -> ").append(entry.sourceRecordPosition).append(";\n")
                    }

                }
            }
        }
        content.append("\n}")
        return content.toString()
    }
}
