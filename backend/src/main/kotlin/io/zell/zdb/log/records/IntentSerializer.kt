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

import io.camunda.zeebe.protocol.record.intent.Intent
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class IntentSerializer : KSerializer<Intent> {
    override fun deserialize(decoder: Decoder): Intent {
        // there is currently not easy way to deserialize, and we also don't need
        TODO("Not yet implemented")
    }

    override val descriptor: SerialDescriptor
        = buildClassSerialDescriptor("Intent") {
        element<String>("intent")
    }

    override fun serialize(encoder: Encoder, value: Intent) {
        encoder.encodeString(value.name())
    }
}
