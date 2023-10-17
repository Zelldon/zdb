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

import io.camunda.zeebe.protocol.ZbColumnFamilies
import io.zell.zdb.state.ZeebeDbReader
import org.agrona.concurrent.UnsafeBuffer
import java.nio.file.Path

class ProcessState(statePath: Path) {

    private var zeebeDbReader: ZeebeDbReader

    init {
        zeebeDbReader = ZeebeDbReader(statePath)
    }

    fun listProcesses(visitor: ZeebeDbReader.JsonValueWithKeyPrefixVisitor) {
        zeebeDbReader.visitDBWithPrefix(ZbColumnFamilies.PROCESS_CACHE, visitor)
    }

    fun processDetails(processDefinitionKey : Long, visitor: ZeebeDbReader.JsonValueWithKeyPrefixVisitor) {
        zeebeDbReader.visitDBWithPrefix(ZbColumnFamilies.PROCESS_CACHE) { key, value ->
            val keyBuffer = UnsafeBuffer(key)
            // due to the recent multi tenancy changes, the process definition key moved to the end
            val currentProcessDefinitionKey = keyBuffer.getLong(keyBuffer.capacity() - Long.SIZE_BYTES)

            if (currentProcessDefinitionKey == processDefinitionKey) {
                visitor.visit(key, value)
            }
        }
    }
}
