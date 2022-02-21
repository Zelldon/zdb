package io.zell.zdb.state.incident

import io.camunda.zeebe.protocol.impl.record.value.incident.IncidentRecord
import io.camunda.zeebe.protocol.record.value.ErrorType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class IncidentDetails private constructor(val key : Long,
                                          val bpmnProcessId : String,
                                          val processDefinitionKey : Long,
                                          val processInstanceKey : Long,
                                          val elementInstanceKey : Long,
                                          val elementId : String,
                                          val jobKey : Long,
                                          val variablesScopeKey : Long,
                                          val errorType: ErrorType,
                                          val errorMessage : String) {

    constructor(key: Long, incident: IncidentRecord) :
            this(
                key,
                incident.bpmnProcessId,
                incident.processDefinitionKey,
                incident.processInstanceKey,
                incident.elementInstanceKey,
                incident.elementId,
                incident.jobKey,
                incident.variableScopeKey,
                incident.errorType,
                incident.errorMessage)

    override fun toString(): String {
        return Json.encodeToString(this)
    }
}
