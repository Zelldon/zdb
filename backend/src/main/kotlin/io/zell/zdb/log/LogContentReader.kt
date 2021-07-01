package io.zell.zdb.log

import io.atomix.raft.storage.log.RaftLogReader
import io.camunda.zeebe.engine.processing.streamprocessor.RecordValues
import io.camunda.zeebe.engine.processing.streamprocessor.TypedEventImpl
import io.camunda.zeebe.engine.processing.streamprocessor.TypedEventRegistry
import io.camunda.zeebe.logstreams.impl.log.LoggedEventImpl
import io.camunda.zeebe.protocol.impl.record.RecordMetadata
import io.camunda.zeebe.util.ReflectUtil
import org.agrona.concurrent.UnsafeBuffer
import java.nio.file.Path

class LogContentReader(logPath: Path) {

    private val RECORD_VALUES = RecordValues();
    private val reader: RaftLogReader = LogFactory.newReader(logPath)

    fun content(): LogContent {
        val logContent = LogContent()

        reader.forEach {
            if (it.isApplicationEntry) {
                val applicationRecord = ApplicationRecord(it.index(), it.term())
                val applicationEntry = it.applicationEntry

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
            } else
            {
                val raftRecord = RaftRecord(it.index(), it.term())
                logContent.records.add(raftRecord)
            }
        }
        return logContent
    }
}
