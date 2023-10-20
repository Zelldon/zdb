package io.zell.zdb.log.records

import io.camunda.zeebe.protocol.record.value.BpmnElementType
import kotlinx.serialization.Serializable

@Serializable
data class ProcessInstanceRelatedValue(
    val processInstanceKey: Long? = null,
    val bpmnElementType: BpmnElementType? = null,
    val processDefinitionKey: Long? = null)