package io.zell.zdb.state.general

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class ProcessInstancesDetails constructor(val processInstanceCount : Long, val elementInstanceCount: Long) {
    override fun toString(): String {
        return Json.encodeToString(this)
    }
}
