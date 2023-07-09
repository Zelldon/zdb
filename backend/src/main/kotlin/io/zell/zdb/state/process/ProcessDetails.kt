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
