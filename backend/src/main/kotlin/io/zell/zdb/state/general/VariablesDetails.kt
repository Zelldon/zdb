package io.zell.zdb.state.general

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class VariablesDetails constructor(val variablesCount: Long, val maxSize: Long, val minSize: Long, val avgSize: Double) {
    override fun toString(): String {
        return Json.encodeToString(this)
    }
}
