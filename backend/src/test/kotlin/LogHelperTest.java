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
import io.camunda.zeebe.logstreams.impl.log.LogEntryDescriptor;
import io.camunda.zeebe.logstreams.impl.log.LoggedEventImpl;
import io.camunda.zeebe.logstreams.impl.serializer.DataFrameDescriptor;
import io.camunda.zeebe.protocol.Protocol;
import io.camunda.zeebe.protocol.impl.record.RecordMetadata;
import io.camunda.zeebe.protocol.record.ValueType;
import io.zell.zdb.log.LogHelperKt;
import org.agrona.ExpandableArrayBuffer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LogHelperTest {

  @Test
  public void shouldHandleUnknownValueTypes() {
    // given
    final var buffer = new ExpandableArrayBuffer();
    final var key = Protocol.encodePartitionId(1, 2);
    // would be nice if we could create such log entries easier...
    LogEntryDescriptor.setKey(buffer, DataFrameDescriptor.messageOffset(0), key);
    final var loggedEvent = new LoggedEventImpl();
    loggedEvent.wrap(buffer, 0);
    final var recordMetadata = new RecordMetadata();
    recordMetadata.valueType(ValueType.SBE_UNKNOWN);

    // when
    final var typedEvent = LogHelperKt.convertToTypedEvent(loggedEvent, recordMetadata);

    // then
    assertThat(typedEvent).isNotNull();
    assertThat(typedEvent.getValue()).isNull();
    assertThat(typedEvent.getPartitionId()).isEqualTo(1);
    assertThat(typedEvent.getKey()).isEqualTo(key);
    assertThat(typedEvent.toJson()).isNotEmpty();
  }
}
