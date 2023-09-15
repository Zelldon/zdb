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
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import io.camunda.zeebe.engine.EngineConfiguration;
import io.camunda.zeebe.engine.state.ProcessingDbState;
import io.camunda.zeebe.engine.state.deployment.DeployedProcess;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.util.buffer.BufferUtil;
import io.zeebe.containers.ZeebeContainer;
import io.zell.zdb.ZeebePaths;
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class ZeebeTest {

  public File tempDir = new File("/tmp/", "data-" + ThreadLocalRandom.current().nextLong());

  @Container
  public ZeebeContainer zeebeContainer = new ZeebeContainer()
          .withEnv("ZEEBE_BROKER_EXPERIMENTAL_ROCKSDB_DISABLEWAL", "false")
          .withFileSystemBind(tempDir.getPath(), "/usr/local/zeebe/data/", BindMode.READ_WRITE);

  /**
   * Just ot verify whether test container works with Zeebe Client.
   */
  @Test
  public void shouldRunProcessInstanceUntilEnd() {
    // given
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .gatewayAddress(zeebeContainer.getExternalGatewayAddress())
            .usePlaintext()
            .build();
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process").startEvent().endEvent().done();

    // when
    final DeploymentEvent deploymentEvent =
        client.newDeployCommand().addProcessModel(process, "process.bpmn").send().join();

    // then
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

  @Test
  public void shouldOpenAndReadState() {
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
    final var readonlyTransactionDb = ReadonlyTransactionDb.Companion
        .openReadonlyDb(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
    var zeebeState = new ProcessingDbState(1, readonlyTransactionDb, readonlyTransactionDb.createContext(), () -> 1, new EngineConfiguration());

    // then
    final var processState = zeebeState.getProcessState();
    final var processes = processState.getProcesses();
    assertThat(processes).hasSize(1);
    final var deployedProcesses = new ArrayList<DeployedProcess>(processes);
    final var deployedProcess = deployedProcesses.get(0);
    assertThat(deployedProcess.getVersion()).isEqualTo(1);
    assertThat(deployedProcess.getBpmnProcessId()).isEqualTo(BufferUtil.wrapString("process"));
    assertThat(deployedProcess.getResourceName()).isEqualTo(BufferUtil.wrapString("process.bpmn"));
    assertThat(deployedProcess.getKey()).isEqualTo(deploymentEvent.getProcesses().get(0).getProcessDefinitionKey());
  }
}
