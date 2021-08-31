package io.zell.zdb.log

import io.atomix.raft.storage.log.IndexedRaftLogEntry
import io.atomix.raft.storage.log.RaftLogReader
import io.camunda.zeebe.engine.processing.streamprocessor.TypedEventImpl
import io.camunda.zeebe.engine.processing.streamprocessor.TypedEventRegistry
import io.camunda.zeebe.logstreams.impl.log.LoggedEventImpl
import io.camunda.zeebe.protocol.impl.record.RecordMetadata
import io.camunda.zeebe.protocol.record.Record
import io.camunda.zeebe.util.ReflectUtil
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
                val applicationEntry = entry.applicationEntry

                val readBuffer = UnsafeBuffer(applicationEntry.data());
                val loggedEvent = LoggedEventImpl();
                val metadata = RecordMetadata();

                var offset = 0;
                do {
                    loggedEvent.wrap(readBuffer, offset)

                    if (loggedEvent.position == position) {

                        loggedEvent.readMetadata(metadata)
                        val unifiedRecordValue =
                            ReflectUtil.newInstance(TypedEventRegistry.EVENT_REGISTRY.get(metadata.getValueType()))
                        loggedEvent.readValue(unifiedRecordValue)

                        val typedEvent = TypedEventImpl(1)
                        typedEvent.wrap(loggedEvent, metadata, unifiedRecordValue)
                        return typedEvent
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