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

    private val reader: RaftLogReader = LogFactory.newReader(logPath)

    fun content(): LogContent {
        val logContent = LogContent()
        reader.forEach {
            LogContent.addEntryToContent(it, logContent)
        }
        return logContent
    }
}
