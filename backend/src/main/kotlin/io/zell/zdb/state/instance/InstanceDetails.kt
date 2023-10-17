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

import io.camunda.zeebe.engine.state.deployment.DeployedProcess
import io.camunda.zeebe.engine.state.instance.ElementInstance
import io.camunda.zeebe.engine.state.instance.IndexedRecord
import io.camunda.zeebe.protocol.record.value.BpmnElementType
import io.camunda.zeebe.util.buffer.BufferUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Serializable
class InstanceDetails constructor(val parentKey : Long,
                                          val multiInstanceLoopCounter: Int,
                                          val childTerminatedCount: Int,
                                          val childCompletedCount: Int,
                                          val childActivatedCount: Int,
                                          val childCount: Int,
                                          val interruptingEventKeyProp: String,
                                          val calledChildInstanceKeyProp: Long,
                                          val elementRecord: ElementDetails)
