package io.zell.zdb.state.general

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class ProcessingDetails constructor(val lastProcessedPosition: Long) {
    override fun toString(): String {
        return Json.encodeToString(this)
    }
}
