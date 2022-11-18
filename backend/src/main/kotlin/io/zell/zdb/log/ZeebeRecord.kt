package io.zell.zdb.log

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.camunda.zeebe.logstreams.log.LoggedEvent
import io.camunda.zeebe.protocol.impl.encoding.MsgPackConverter
import io.camunda.zeebe.protocol.impl.record.RecordMetadata
import io.camunda.zeebe.protocol.record.JsonSerializable
import io.camunda.zeebe.protocol.record.RecordType
import io.camunda.zeebe.protocol.record.RejectionType
import io.camunda.zeebe.protocol.record.ValueType
import io.camunda.zeebe.protocol.record.intent.Intent
import org.agrona.concurrent.UnsafeBuffer

class ZeebeRecord(val partitionId : Int, private val rawEvent : LoggedEvent, private val metadata: RecordMetadata) : JsonSerializable {
    override fun toString(): String {
        return toJson()!!
    }

    fun getPosition(): Long {
        return rawEvent.position
    }

    fun getSourceRecordPosition(): Long {
        return rawEvent.sourceEventPosition
    }

    fun getTimestamp(): Long {
        return rawEvent.timestamp
    }

    fun getIntent(): Intent {
        return metadata.intent
    }

    fun getRecordType(): RecordType? {
        return metadata.recordType
    }

    fun getRejectionType(): RejectionType? {
        return metadata.rejectionType
    }

    fun getRejectionReason(): String? {
        return metadata.rejectionReason
    }

    fun getBrokerVersion(): String {
        return metadata.brokerVersion.toString()
    }

    fun getValueType(): ValueType {
        return metadata.valueType
    }

    fun getKey(): Long {
        return rawEvent.key
    }

    fun getValue(): JsonNode {
        val jsonString = MsgPackConverter.convertToJson(
            UnsafeBuffer(
                rawEvent.valueBuffer,
                rawEvent.valueOffset,
                rawEvent.valueLength
            )
        )

        return ObjectMapper().readTree(jsonString)
    }

    override fun toJson(): String? {
        return MsgPackConverter.convertJsonSerializableObjectToJson(this)
    }
}
