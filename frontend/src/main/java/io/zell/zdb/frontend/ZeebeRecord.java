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
package io.zell.zdb.frontend;

import javafx.beans.property.*;

public class ZeebeRecord {
  //                        Record(val position: Long,
  //                                val sourceRecordPosition: Long,
  //                                val timestamp: Long,
  //                                val key: Long,
  //                                val recordType: RecordType,
  //                                val valueType: ValueType,
  //                                @Serializable(with = IntentSerializer::class)
  //                                        val intent: Intent,
  //                                val rejectionType: RejectionType? = RejectionType.NULL_VAL,
  //                                val rejectionReason: String? = "",
  //                                val requestId: Long? = 0,
  //                                val requestStreamId: Int = 0,
  //                                val protocolVersion: Int,
  //                                val brokerVersion: String,
  //                                val recordVersion: Int ? = 0,
  //                                val authData: String ? = "",
  //                                val recordValue: JsonElement,
  //                                /*Transient marks to ignore the property during serialization */
  //                                @Transient val piRelatedValue: ProcessInstanceRelatedValue? =
  // null
  private final LongProperty position;
  private final LongProperty sourceRecordPosition;
  private final LongProperty timestamp;
  private final LongProperty key;
  private final StringProperty recordType;
  private final StringProperty ValueType;
  private final StringProperty intent;

  private final StringProperty rejectionType;
  private final StringProperty rejectionReason;
  private final LongProperty requestId;
  private final IntegerProperty requestStreamId;
  private final IntegerProperty protocolVersion;
  private final StringProperty brokerVersion;
  private final IntegerProperty recordVersion;
  private final StringProperty authData;
  private final StringProperty recordValue;

  public ZeebeRecord(
      final long position,
      final long sourceRecordPostion,
      final long timestamp,
      final long key,
      final String recordType,
      final String valueType,
      final String intent,
      final String rejectionType,
      final String rejectionReason,
      final long requestId,
      final int requestStreamId,
      final int protocolVersion,
      final String brokerVersion,
      final int recordVersion,
      final String authData,
      final String recordValue) {
    this.position = new SimpleLongProperty(position);
    this.sourceRecordPosition = new SimpleLongProperty(sourceRecordPostion);
    this.timestamp = new SimpleLongProperty(timestamp);
    this.key = new SimpleLongProperty(key);
    this.recordType = new SimpleStringProperty(recordType);
    this.ValueType = new SimpleStringProperty(valueType);
    this.intent = new SimpleStringProperty(intent);
    this.rejectionType = new SimpleStringProperty(rejectionType);
    this.rejectionReason = new SimpleStringProperty(rejectionReason);
    this.requestId = new SimpleLongProperty(requestId);
    this.requestStreamId = new SimpleIntegerProperty(requestStreamId);
    this.protocolVersion = new SimpleIntegerProperty(protocolVersion);
    this.brokerVersion = new SimpleStringProperty(brokerVersion);
    this.recordVersion = new SimpleIntegerProperty(recordVersion);
    this.authData = new SimpleStringProperty(authData);
    this.recordValue = new SimpleStringProperty(recordValue);
  }

  public String getRejectionType() {
    return this.rejectionType.get();
  }

  public StringProperty rejectionTypeProperty() {
    return this.rejectionType;
  }

  public void setRejectionType(final String rejectionType) {
    this.rejectionType.set(rejectionType);
  }

  public String getRejectionReason() {
    return this.rejectionReason.get();
  }

  public StringProperty rejectionReasonProperty() {
    return this.rejectionReason;
  }

  public void setRejectionReason(final String rejectionReason) {
    this.rejectionReason.set(rejectionReason);
  }

  public long getRequestId() {
    return this.requestId.get();
  }

  public LongProperty requestIdProperty() {
    return this.requestId;
  }

  public void setRequestId(final long requestId) {
    this.requestId.set(requestId);
  }

  public int getRequestStreamId() {
    return this.requestStreamId.get();
  }

  public IntegerProperty requestStreamIdProperty() {
    return this.requestStreamId;
  }

  public void setRequestStreamId(final int requestStreamId) {
    this.requestStreamId.set(requestStreamId);
  }

  public int getProtocolVersion() {
    return this.protocolVersion.get();
  }

  public IntegerProperty protocolVersionProperty() {
    return this.protocolVersion;
  }

  public void setProtocolVersion(final int protocolVersion) {
    this.protocolVersion.set(protocolVersion);
  }

  public String getBrokerVersion() {
    return this.brokerVersion.get();
  }

  public StringProperty brokerVersionProperty() {
    return this.brokerVersion;
  }

  public void setBrokerVersion(final String brokerVersion) {
    this.brokerVersion.set(brokerVersion);
  }

  public int getRecordVersion() {
    return this.recordVersion.get();
  }

  public IntegerProperty recordVersionProperty() {
    return this.recordVersion;
  }

  public void setRecordVersion(final int recordVersion) {
    this.recordVersion.set(recordVersion);
  }

  public String getAuthData() {
    return this.authData.get();
  }

  public StringProperty authDataProperty() {
    return this.authData;
  }

  public void setAuthData(final String authData) {
    this.authData.set(authData);
  }

  public String getRecordValue() {
    return this.recordValue.get();
  }

  public StringProperty recordValueProperty() {
    return this.recordValue;
  }

  public void setRecordValue(final String recordValue) {
    this.recordValue.set(recordValue);
  }

  public long getPosition() {
    return this.position.get();
  }

  public LongProperty positionProperty() {
    return this.position;
  }

  public void setPosition(final long position) {
    this.position.set(position);
  }

  public long getSourceRecordPosition() {
    return this.sourceRecordPosition.get();
  }

  public LongProperty sourceRecordPositionProperty() {
    return this.sourceRecordPosition;
  }

  public void setSourceRecordPosition(final long sourceRecordPosition) {
    this.sourceRecordPosition.set(sourceRecordPosition);
  }

  public long getTimestamp() {
    return this.timestamp.get();
  }

  public LongProperty timestampProperty() {
    return this.timestamp;
  }

  public void setTimestamp(final long timestamp) {
    this.timestamp.set(timestamp);
  }

  public long getKey() {
    return this.key.get();
  }

  public LongProperty keyProperty() {
    return this.key;
  }

  public void setKey(final long key) {
    this.key.set(key);
  }

  public String getRecordType() {
    return this.recordType.get();
  }

  public StringProperty recordTypeProperty() {
    return this.recordType;
  }

  public void setRecordType(final String recordType) {
    this.recordType.set(recordType);
  }

  public String getValueType() {
    return this.ValueType.get();
  }

  public StringProperty valueTypeProperty() {
    return this.ValueType;
  }

  public void setValueType(final String valueType) {
    this.ValueType.set(valueType);
  }

  public String getIntent() {
    return this.intent.get();
  }

  public StringProperty intentProperty() {
    return this.intent;
  }

  public void setIntent(final String intent) {
    this.intent.set(intent);
  }
}
