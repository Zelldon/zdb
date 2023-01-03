package io.zell.zdb.log

import io.atomix.raft.storage.log.RaftLogReader
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
