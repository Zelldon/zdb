/*
 * Copyright © 2021 Christopher Kujawa (zelldon91@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
