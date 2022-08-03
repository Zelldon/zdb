import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.dispatcher.impl.log.DataFrameDescriptor;
import io.camunda.zeebe.logstreams.impl.log.LogEntryDescriptor;
import io.camunda.zeebe.logstreams.impl.log.LoggedEventImpl;
import io.camunda.zeebe.protocol.Protocol;
import io.camunda.zeebe.protocol.impl.record.RecordMetadata;
import io.camunda.zeebe.protocol.record.ValueType;
import io.zell.zdb.log.LogHelperKt;
import org.agrona.ExpandableArrayBuffer;
import org.junit.jupiter.api.Test;

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
