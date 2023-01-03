package io.zell.zdb.log

import io.camunda.zeebe.engine.processing.streamprocessor.TypedEventRegistry
import io.camunda.zeebe.logstreams.impl.log.LoggedEventImpl
import io.camunda.zeebe.protocol.Protocol
import io.camunda.zeebe.protocol.impl.record.RecordMetadata
import io.camunda.zeebe.streamprocessor.TypedRecordImpl
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
