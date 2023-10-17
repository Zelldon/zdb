package io.zell.zdb.state.instance

import io.camunda.zeebe.msgpack.property.EnumProperty
import io.camunda.zeebe.msgpack.property.LongProperty
import io.camunda.zeebe.msgpack.property.ObjectProperty
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
class ElementDetails constructor(val key : Long,
                                        val state: ProcessInstanceIntent,
                                        val processInstanceRecord: ProcessInstanceRecordDetails)
