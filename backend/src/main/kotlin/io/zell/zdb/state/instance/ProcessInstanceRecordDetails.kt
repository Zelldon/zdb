package io.zell.zdb.state.instance

import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent
import io.camunda.zeebe.protocol.record.value.BpmnElementType
import io.camunda.zeebe.protocol.record.value.BpmnEventType
import kotlinx.serialization.Serializable

@Serializable
class ProcessInstanceRecordDetails constructor(val bpmnProcessId : String,
                                               val version: Int,
                                               val tenantId: String,
                                               val processDefinitionKey: Long,
                                               val processInstanceKey: Long,
                                               val elementId: String,
                                               val flowScopeKey: Long,
                                               val bpmnElementType: BpmnElementType,
                                               val bpmnEventType: BpmnEventType,
                                               val parentProcessInstanceKey: Long,
                                               val parentElementInstanceKey: Long)