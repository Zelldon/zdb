package io.zell.zdb.state.general

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class IncidentDetails constructor(val bannedInstances: Long, val incidents: Long) {
    override fun toString(): String {
        return Json.encodeToString(this)
    }
}
