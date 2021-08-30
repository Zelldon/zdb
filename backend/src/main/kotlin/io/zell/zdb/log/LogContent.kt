package io.zell.zdb.log

import io.atomix.raft.storage.log.IndexedRaftLogEntry
import io.camunda.zeebe.engine.processing.streamprocessor.TypedEventImpl
import io.camunda.zeebe.engine.processing.streamprocessor.TypedEventRegistry
import io.camunda.zeebe.logstreams.impl.log.LoggedEventImpl
import io.camunda.zeebe.protocol.impl.record.RecordMetadata
import io.camunda.zeebe.util.ReflectUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
                val loggedEvent = LoggedEventImpl();
                val metadata = RecordMetadata();

                var offset = 0;
                do {
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
}
