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

import io.camunda.zeebe.protocol.ZbColumnFamilies
import io.camunda.zeebe.protocol.record.value.BpmnElementType
import io.zell.zdb.state.ZeebeDbReader
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.util.function.Predicate

class InstanceState(statePath: Path) {

    private var json: Json = Json { ignoreUnknownKeys = true}
    private var zeebeDbReader: ZeebeDbReader

    init {
        zeebeDbReader = ZeebeDbReader(statePath)
    }

    fun getInstance(elementInstanceKey: Long): String {
        return zeebeDbReader.getValueAsJson(ZbColumnFamilies.ELEMENT_INSTANCE_KEY, elementInstanceKey)
    }

    fun listInstances( visitor: ZeebeDbReader.JsonValueWithKeyPrefixVisitor) {
        zeebeDbReader.visitDBWithPrefix(ZbColumnFamilies.ELEMENT_INSTANCE_KEY, visitor)
    }

    fun listProcessInstances(predicate: Predicate<ProcessInstanceRecordDetails>, visitor: ZeebeDbReader.JsonValueWithKeyPrefixVisitor) {
        zeebeDbReader.visitDBWithPrefix(ZbColumnFamilies.ELEMENT_INSTANCE_KEY) {
            key: ByteArray, value: String ->

            val instanceDetails = json.decodeFromString<InstanceDetails>(value)
            val processInstanceRecord = instanceDetails.elementRecord.processInstanceRecord
            if (processInstanceRecord.bpmnElementType == BpmnElementType.PROCESS
                && predicate.test(processInstanceRecord)) {
                visitor.visit(key, value)
            }
        }
    }
}
