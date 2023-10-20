package io.zell.zdb.log.records

import io.camunda.zeebe.protocol.record.RecordType
import io.camunda.zeebe.protocol.record.ValueType
import io.camunda.zeebe.protocol.record.intent.Intent

data class Record(val position: Long,
                  val sourceRecordPosition: Long,
                  val timestamp: Long,
                  val key: Long,
                  val recordType: RecordType,
                  val valueType: ValueType,
                  val intent: Intent,
                  val recordValue: RecordValue
)