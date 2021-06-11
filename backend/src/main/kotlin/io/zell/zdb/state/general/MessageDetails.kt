package io.zell.zdb.state.general

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable
class MessageDetails constructor(val count: Long, val nextDeadline: Long, val lastDeadline: Long) {
    override fun toString(): String {
        return Json.encodeToString(this)
    }
}
