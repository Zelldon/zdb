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
package io.zell.zdb.log

import io.camunda.zeebe.logstreams.impl.log.LoggedEventImpl
import io.camunda.zeebe.protocol.Protocol
import io.camunda.zeebe.protocol.impl.record.RecordMetadata
import io.camunda.zeebe.stream.impl.TypedEventRegistry
import io.camunda.zeebe.stream.impl.records.TypedRecordImpl
import io.camunda.zeebe.util.ReflectUtil


/**
 * Reads the given log event details and converts it to a TypedEvent, which can be further processed.
 *
 * Based on the value type the typed event is created. If the value type doesn't exist, e.g. if a new version
 * is read by zdb then a TypedEvent without a record value is returned.
 *
 */
fun convertToTypedEvent(
    loggedEvent: LoggedEventImpl,
    metadata: RecordMetadata
): TypedRecordImpl {
    val typedEvent = TypedRecordImpl(Protocol.decodePartitionId(loggedEvent.key))

    val recordValueClass = TypedEventRegistry.EVENT_REGISTRY.get(metadata.getValueType())
    if (recordValueClass == null) {
        // it is likely that the value type is a new type, which is not yet supported by zdb
        typedEvent.wrap(loggedEvent, metadata, null)
        return typedEvent
    }

    val unifiedRecordValue =
        ReflectUtil.newInstance(recordValueClass)
    loggedEvent.readValue(unifiedRecordValue)

    typedEvent.wrap(loggedEvent, metadata, unifiedRecordValue)
    return typedEvent
}
