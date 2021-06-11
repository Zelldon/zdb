package io.zell.zdb.state.process

import io.camunda.zeebe.engine.state.deployment.DeployedProcess
import io.camunda.zeebe.util.buffer.BufferUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
// primary ctor - private constructor as work-around for json serialization
// https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/basic-serialization.md#constructor-properties-requirement
class ProcessDetails private constructor(val bpmnProcessId : String,
                                         val resourceName : String,
                                         val processDefinitionKey : Long,
                                         val version : Int,
                                         val resource: String) {

    constructor(deployedProcess: DeployedProcess) :
            this (
                BufferUtil.bufferAsString(deployedProcess.bpmnProcessId),
                BufferUtil.bufferAsString(deployedProcess.resourceName),
                deployedProcess.key,
                deployedProcess.version,
                BufferUtil.bufferAsString(deployedProcess.resource))

    override fun toString(): String {
        return Json.encodeToString(this)
    }
}
