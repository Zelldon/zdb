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

import io.camunda.zeebe.db.impl.ZeebeDbConstants
import io.camunda.zeebe.protocol.ZbColumnFamilies
import io.zell.zdb.state.JsonElementVisitor
import io.zell.zdb.state.ZeebeDbReader
import org.agrona.concurrent.UnsafeBuffer
import java.nio.file.Path

class IncidentState(statePath: Path) {

    private var zeebeDbReader: ZeebeDbReader

    init {
        zeebeDbReader = ZeebeDbReader(statePath)
    }

    fun listIncidents(visitor: JsonElementVisitor) {
        zeebeDbReader.visitDBWithPrefix(
            ZbColumnFamilies.INCIDENTS
        ) { key: ByteArray?, valueJson: String? ->
            val incidentKey = UnsafeBuffer(key).getLong(java.lang.Long.BYTES, ZeebeDbConstants.ZB_DB_BYTE_ORDER).toString()
            visitor.visit("""{"key": $incidentKey, "value": $valueJson}""")
        }
    }

    fun incidentDetails(incidentKey : Long): String {
        return zeebeDbReader.getValueAsJson(ZbColumnFamilies.INCIDENTS, incidentKey)
    }
}
