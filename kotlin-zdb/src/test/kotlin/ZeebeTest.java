import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.db.impl.rocksdb.transaction.ZeebeTransactionDb;
import io.camunda.zeebe.engine.state.DefaultZeebeDbFactory;
import io.camunda.zeebe.engine.state.ZbColumnFamilies;
import io.camunda.zeebe.engine.state.ZeebeDbState;
import io.camunda.zeebe.engine.state.immutable.ZeebeState;
import io.camunda.zeebe.engine.state.mutable.MutableZeebeState;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.containers.ZeebeContainer;
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.rocksdb.DBOptions;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.RocksDB;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ZeebeTest {

  public File tempDir = new File("/tmp/", "data-" + ThreadLocalRandom.current().nextLong());

  @Container
  public ZeebeContainer zeebeContainer = new ZeebeContainer().withFileSystemBind(tempDir.getPath(), "/usr/local/zeebe/data/", BindMode.READ_WRITE);

  public void readAll() throws Exception {
    final var partitionsPath = tempDir.toPath().resolve("raft-partition").resolve("partitions")
        .resolve("1").resolve("runtime");

    final var readonlyTransactionDb = ReadonlyTransactionDb.Companion
        .openReadonlyDb(partitionsPath);
    var zeebeState = new ZeebeDbState(readonlyTransactionDb, readonlyTransactionDb.createContext());

  }

  @Test
  public void shouldRunProcessInstanceUntilEnd() throws Exception {
    // given
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .gatewayAddress(zeebeContainer.getExternalGatewayAddress())
            .usePlaintext()
            .build();
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process").startEvent().endEvent().done();

    // when
    // do something (e.g. deploy a process)
    final DeploymentEvent deploymentEvent =
        client.newDeployCommand().addProcessModel(process, "process.bpmn").send().join();
readAll();
    // then
    // verify (e.g. we can create an instance of the deployed process)
    final ProcessInstanceResult processInstanceResult =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId("process")
            .latestVersion()
            .withResult()
            .send()
            .join();
    assertThat(processInstanceResult.getProcessDefinitionKey())
        .isEqualTo(deploymentEvent.getProcesses().get(0).getProcessDefinitionKey());
  }

  @Test
  public void shouldCreateAndUseVolume() {
    // given

    // when

    // then
    assertThat(tempDir).exists();
    assertThat(tempDir.listFiles()).extracting(File::getName).containsExactly("raft-partition");
  }
}
