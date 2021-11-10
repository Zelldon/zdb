package io.zell.zdb.log

import io.atomix.raft.storage.log.RaftLogReader
import java.nio.file.Path


class LogStatus(logPath: Path) {

    private val reader: RaftLogReader = LogFactory.newReader(logPath)

    fun status(): LogStatusDetails {
        val logStatusDetails = LogStatusDetails()

        reader.forEach {
            logStatusDetails.scannedEntries++

            val persistedRaftRecord = it.persistedRaftRecord

            if (logStatusDetails.highestTerm < persistedRaftRecord.term()) {
                logStatusDetails.highestTerm = persistedRaftRecord.term()
            }

            val currentEntryIndex = persistedRaftRecord.index()
            if (logStatusDetails.highestIndex < currentEntryIndex) {
                logStatusDetails.highestIndex = currentEntryIndex
            }

            if (logStatusDetails.lowestIndex > currentEntryIndex) {
                logStatusDetails.lowestIndex = currentEntryIndex
            }

            val approxEntrySize = persistedRaftRecord.approximateSize()
            if (logStatusDetails.maxEntrySizeBytes < approxEntrySize) {
                logStatusDetails.maxEntrySizeBytes = approxEntrySize
            }

            if (logStatusDetails.minEntrySizeBytes > approxEntrySize) {
                logStatusDetails.minEntrySizeBytes = approxEntrySize
            }
            logStatusDetails.avgEntrySizeBytes += approxEntrySize

            if (it.isApplicationEntry) {
                val applicationEntry = it.applicationEntry
                if (logStatusDetails.highestRecordPosition < applicationEntry.highestPosition()) {
                    logStatusDetails.highestRecordPosition = applicationEntry.highestPosition()
                }

                if (logStatusDetails.lowestRecordPosition > applicationEntry.lowestPosition()) {
                    logStatusDetails.lowestRecordPosition = applicationEntry.lowestPosition()
                }
            }
        }

        if (logStatusDetails.scannedEntries > 0) {
            logStatusDetails.avgEntrySizeBytes /= logStatusDetails.scannedEntries
        }

        return logStatusDetails
    }


}
