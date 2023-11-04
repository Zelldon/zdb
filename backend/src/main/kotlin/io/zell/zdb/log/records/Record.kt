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
package io.zell.zdb.log.records

import io.camunda.zeebe.protocol.record.RecordType
import io.camunda.zeebe.protocol.record.RejectionType
import io.camunda.zeebe.protocol.record.ValueType
import io.camunda.zeebe.protocol.record.intent.Intent
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class Record(val position: Long,
                  val sourceRecordPosition: Long,
                  val timestamp: Long,
                  val key: Long,
                  val recordType: RecordType,
                  val valueType: ValueType,
                  @Serializable(with = IntentSerializer::class)
                  val intent: Intent,
                  val rejectionType: RejectionType? = RejectionType.NULL_VAL,
                  val rejectionReason: String? = "",
                  val requestId: Long? = 0,
                  val requestStreamId: Int = 0,
                  val protocolVersion: Int,
                  val brokerVersion: String,
                  val recordVersion: Int ? = 0,
                  val authData: String ? = "",
                  val recordValue: JsonElement,
                  /*Transient marks to ignore the property during serialization */
                  @Transient val piRelatedValue: ProcessInstanceRelatedValue? = null
) {
    override fun toString(): String {
        return Json.encodeToString(this)
    }
}