package io.zell.zdb.log

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class LogStatusDetails constructor() {
    var scannedEntries: Long = 0
    var maxEntrySizeBytes = Int.MIN_VALUE
    var minEntrySizeBytes = Int.MAX_VALUE
    var avgEntrySizeBytes = 0.0
    var lowestRecordPosition = Long.MAX_VALUE
    var highestRecordPosition = Long.MIN_VALUE
    var lowestIndex = Long.MAX_VALUE
    var highestIndex = Long.MIN_VALUE
    var highestTerm = Long.MIN_VALUE

    override fun toString(): String {
        return Json.encodeToString(this)
    }
}
