package io.zell.zdb.state.general

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class GeneralDetails constructor(val processingDetails: ProcessingDetails,
                                 val exportingDetails: ExportingDetails,
                                 val incidentDetails: IncidentDetails,
                                 val messageDetails: MessagesDetails,
                                 val processInstancesDetails: ProcessInstancesDetails,
                                 val variablesDetails: VariablesDetails) {
    override fun toString(): String {
        return Json.encodeToString(this)
    }
}
