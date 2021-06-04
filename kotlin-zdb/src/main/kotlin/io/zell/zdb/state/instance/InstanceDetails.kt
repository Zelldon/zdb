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
