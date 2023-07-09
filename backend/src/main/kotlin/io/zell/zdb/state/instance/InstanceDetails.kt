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
package io.zell.zdb.state.instance

import io.camunda.zeebe.engine.state.deployment.DeployedProcess
import io.camunda.zeebe.engine.state.instance.ElementInstance
import io.camunda.zeebe.protocol.record.value.BpmnElementType
import io.camunda.zeebe.util.buffer.BufferUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class InstanceDetails private constructor(val key : Long,
                                          val state : String,
                                          val bpmnProcessId : String,
                                          val processDefinitionKey : Long,
                                          val version : Int,
                                          val processInstanceKey : Long,
                                          val parentElementInstanceKey : Long,
                                          val flowScopeKey : Long,
                                          val parentProcessInstanceKey : Long,
                                          val elementType : BpmnElementType,
                                          val elementId : String,
                                          val jobKey: Long,
                                          val children: List<Long>) {

    constructor(elementInstance: ElementInstance, children: List<ElementInstance>?) :
            this (elementInstance.key,
                elementInstance.state.name,
                elementInstance.value.bpmnProcessId,
                elementInstance.value.processDefinitionKey,
                elementInstance.value.version,
                elementInstance.value.processInstanceKey,
                elementInstance.value.parentElementInstanceKey,
                elementInstance.value.flowScopeKey,
                elementInstance.value.parentProcessInstanceKey,
                elementInstance.value.bpmnElementType,
                elementInstance.value.elementId,
                elementInstance.jobKey,
                children?.map { it.key } ?: listOf())


    override fun toString(): String {
        return Json.encodeToString(this)
    }
}
