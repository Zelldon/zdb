package io.zell.zdb.log

import io.atomix.raft.storage.log.RaftLog
import io.atomix.raft.storage.log.RaftLogReader
import java.nio.file.Path

class LogFactory {

    companion object {

        /**
         * Necessary for the file names, log files.
         */
        private const val PARTITION_NAME_FORMAT = "raft-partition-partition-%d"
        private const val MAX_SEGMENT_SIZE = 128 * 1024 * 1024

        fun newReader(logPath: Path): RaftLogReader {
            val partitionName = extractPartitionNameFromPath(logPath)

            val raftLog = RaftLog.builder()
                .withDirectory(logPath.toFile())
                .withName(partitionName)
                .withMaxSegmentSize(MAX_SEGMENT_SIZE).build()

            return raftLog.openUncommittedReader()
        }

        private fun extractPartitionNameFromPath(logPath: Path): String {
            return try {
                val partitionId = logPath.fileName.toString().toInt()
                String.format(PARTITION_NAME_FORMAT, partitionId)
            } catch (nfe: NumberFormatException) {
                val errorMsg = String.format(
                    "Expected to extract partition as integer from path, but path was '%s'.",
                    logPath
                )
                throw IllegalArgumentException(errorMsg, nfe)
            }
        }
    }
}
