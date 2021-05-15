import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.engine.state.ZeebeDbState;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.containers.ZeebeContainer;
import io.zell.zdb.ProcessInspection;
import io.zell.zdb.ZeebePaths;
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb;
import io.zell.zdb.state.process.ProcessState;
import java.io.File;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ProcessStateTest {

  public File tempDir = new File("/tmp/", "data-" + ThreadLocalRandom.current().nextLong());

  @Container
  public ZeebeContainer zeebeContainer = new ZeebeContainer().withFileSystemBind(tempDir.getPath(), "/usr/local/zeebe/data/", BindMode.READ_WRITE);

  @Test
  public void shouldListProcesses() {
    // given
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .gatewayAddress(zeebeContainer.getExternalGatewayAddress())
            .usePlaintext()
            .build();
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process").startEvent().endEvent().done();
    final DeploymentEvent deploymentEvent =
        client.newDeployCommand().addProcessModel(process, "process.bpmn").send().join();

    // when
    final var processState = new ProcessState(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
    final var processes = processState.listProcesses();

    // then
    final var processMeta = processes.get(0);
    final var returnedProcess = deploymentEvent.getProcesses().get(0);
    assertThat(processMeta.getBpmnProcessId()).isEqualTo(returnedProcess.getBpmnProcessId());
    assertThat(processMeta.getProcessDefinitionKey()).isEqualTo(returnedProcess.getProcessDefinitionKey());
    assertThat(processMeta.getResourceName()).isEqualTo(returnedProcess.getResourceName());
    assertThat(processMeta.getVersion()).isEqualTo(returnedProcess.getVersion());

    assertThat(processMeta.toString())
        .contains(returnedProcess.getBpmnProcessId())
        .contains(returnedProcess.getResourceName())
        .contains("" + returnedProcess.getVersion())
        .contains("" + returnedProcess.getProcessDefinitionKey());
  }


  @Test
  public void shouldGetProcessesDetails() {
    // given
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .gatewayAddress(zeebeContainer.getExternalGatewayAddress())
            .usePlaintext()
            .build();
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process").startEvent().endEvent().done();
    final DeploymentEvent deploymentEvent =
        client.newDeployCommand().addProcessModel(process, "process.bpmn").send().join();

    // when
    final var processState = new ProcessState(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
    final var returnedProcess = deploymentEvent.getProcesses().get(0);
    final var processDetails = processState.processDetails(returnedProcess.getProcessDefinitionKey());

    // then
    assertThat(processDetails.getBpmnProcessId()).isEqualTo(returnedProcess.getBpmnProcessId());
    assertThat(processDetails.getProcessDefinitionKey()).isEqualTo(returnedProcess.getProcessDefinitionKey());
    assertThat(processDetails.getResourceName()).isEqualTo(returnedProcess.getResourceName());
    assertThat(processDetails.getVersion()).isEqualTo(returnedProcess.getVersion());
    assertThat(processDetails.getResource()).isEqualTo(Bpmn.convertToString(process));

    assertThat(processDetails.toString())
        .contains(returnedProcess.getBpmnProcessId())
        .contains(returnedProcess.getResourceName())
        .contains("" + returnedProcess.getVersion())
        .contains("" + returnedProcess.getProcessDefinitionKey())
        .contains("xml");
  }
}
