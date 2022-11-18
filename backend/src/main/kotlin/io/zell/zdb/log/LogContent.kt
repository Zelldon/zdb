package io.zell.zdb.log

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.atomix.raft.storage.log.IndexedRaftLogEntry
import io.camunda.zeebe.engine.processing.streamprocessor.TypedEventImpl
import io.camunda.zeebe.engine.processing.streamprocessor.TypedRecord
import io.camunda.zeebe.logstreams.impl.log.LoggedEventImpl
import io.camunda.zeebe.protocol.Protocol
import io.camunda.zeebe.protocol.impl.record.RecordMetadata
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord
import io.camunda.zeebe.protocol.jackson.ZeebeProtocolModule
import io.camunda.zeebe.protocol.record.ValueType
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

                    val zeebeRecord = ZeebeRecord(Protocol.decodePartitionId(loggedEvent.key), loggedEvent, metadata)
                    applicationRecord.entries.add(zeebeRecord)

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
        entry: ZeebeRecord,
        content: java.lang.StringBuilder
    ) {
        content.append(entry.getPosition())
            .append(" [label=\"")
            .append("\\n").append(entry.getRecordType())
            .append("\\n").append(entry.getValueType().name)
            .append("\\n").append(entry.getIntent().name())

        if (entry.getValueType() == ValueType.PROCESS_INSTANCE) {
            // todo potential fix
//            val mapper = ObjectMapper().registerModule(ZeebeProtocolModule());
//            val processInstanceRecord = mapper.readValue(entry.getValue().toString(), object: TypeReference<io.camunda.zeebe.protocol.record.Record<ProcessInstanceRecord>>() {});
//            val processInstanceValue = processInstanceRecord.value
//            content.append("\\n").append(processInstanceValue.bpmnElementType)
//            content.append("\\nPI Key: ").append(processInstanceValue.processInstanceKey)
//            content.append("\\nPD Key: ").append(processInstanceValue.processDefinitionKey)
        }

        content
            .append("\\nKey: ").append(entry.getKey())
            .append("\"]")
            .append(";")
            .append(System.lineSeparator())
        if (entry.getSourceRecordPosition() != -1L) {
            content.append(entry.getPosition())
                .append(" -> ")
                .append(entry.getSourceRecordPosition())
                .append(";")
                .append(System.lineSeparator())
        }
    }
}
