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
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ErrorType;
import io.camunda.zeebe.util.FileUtil;
import io.zeebe.containers.ZeebeContainer;
import io.zell.zdb.ZeebePaths;
import io.zell.zdb.state.Experimental;
import io.zell.zdb.state.banned.BannedInstanceState;
import io.zell.zdb.state.general.GeneralState;
import io.zell.zdb.state.incident.IncidentState;
import io.zell.zdb.state.instance.InstanceDetails;
import io.zell.zdb.state.instance.InstanceState;
import io.zell.zdb.state.process.ProcessState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class ZeebeStateTest {

  private static File tempDir = new File("/tmp/", "data-" + ThreadLocalRandom.current().nextLong());

  static {
    // for the Zeebe container the folder need to exist
    tempDir.mkdirs();
  }

  private static final BpmnModelInstance process =
      Bpmn.createExecutableProcess("process")
          .startEvent()
          .parallelGateway("gw")
            .serviceTask("task")
            .zeebeJobType("type")
            .endEvent()
          .moveToLastGateway()
            .serviceTask("incidentTask")
            .zeebeInputExpression("=foo", "bar")
            .zeebeJobType("type")
            .endEvent()
          .done();
  private static final String CONTAINER_PATH = "/usr/local/zeebe/data/";


  @Container
  public static ZeebeContainer zeebeContainer = new ZeebeContainer()
      /* Enable WAL to ensure tests can read from open RocksDB */
      .withEnv("ZEEBE_BROKER_EXPERIMENTAL_ROCKSDB_DISABLEWAL", "false")
      /* run the container with the current user, in order to access the data and delete it later */
      .withCreateContainerCmdModifier(cmd -> cmd.withUser(TestUtils.getRunAsUser()))
      .withFileSystemBind(tempDir.getPath(), CONTAINER_PATH, BindMode.READ_WRITE);

  private static DeploymentEvent deploymentEvent;
  private static ProcessInstanceEvent returnedProcessInstance;
  private static CountDownLatch jobLatch;
  private static final AtomicLong jobKey = new AtomicLong();

  @BeforeAll
  public static void setup() throws Exception {
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .gatewayAddress(zeebeContainer.getExternalGatewayAddress())
            .usePlaintext()
            .build();
    deploymentEvent = client.newDeployCommand().addProcessModel(process, "process.bpmn").send().join();

    returnedProcessInstance = client
        .newCreateInstanceCommand()
        .bpmnProcessId("process")
        .latestVersion()
        .variables(Map.of("var1", "1", "var2", "12", "var3", "123"))
        .send()
        .join();

    client.newPublishMessageCommand().messageName("msg").correlationKey("123").timeToLive(Duration.ofSeconds(1)).send().join();
    client.newPublishMessageCommand().messageName("msg12").correlationKey("123").timeToLive(Duration.ofHours(1)).send().join();

    jobLatch = new CountDownLatch(1);
    final var jobWorker = client.newWorker().jobType("type").handler((jobClient, job) -> {
      jobKey.set(job.getKey());
      jobLatch.countDown();
    }).open();
    jobLatch.await();

    jobWorker.close();
    client.close();
  }

  @AfterAll
  public static void cleanup() throws Exception {
    FileUtil.deleteFolderIfExists(tempDir.toPath());
  }

  @Test
  public void shouldCreateStatsForCompleteState() {
    // given
    final var experimental = new Experimental(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));

    // when
    final var cfMap = experimental.stateStatistics();

    // then
    assertThat(cfMap).containsEntry(ZbColumnFamilies.JOBS, 1)
            .containsEntry(ZbColumnFamilies.VARIABLES, 3)
            .containsEntry(ZbColumnFamilies.INCIDENTS, 1)
            .containsEntry(ZbColumnFamilies.ELEMENT_INSTANCE_KEY, 3);
  }

  @Test
  public void shouldVisitValuesAsJson() {
    // given
    final var experimental = new Experimental(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
    final var incidentMap = new HashMap<String, String>();
    Experimental.JsonVisitor jsonVisitor = (cf, k, v) -> {
      if (cf == ZbColumnFamilies.INCIDENTS) {
        incidentMap.put(new String(k), v);
      }
    };

    // when
    experimental.visitDBWithJsonValues(jsonVisitor);

    // then
    assertThat(incidentMap).containsValue("{\"incidentRecord\":{\"errorType\":\"IO_MAPPING_ERROR\",\"errorMessage\":\"failed to evaluate expression '{bar:foo}': no variable found for name 'foo'\",\"bpmnProcessId\":\"process\",\"processDefinitionKey\":2251799813685249,\"processInstanceKey\":2251799813685251,\"elementId\":\"incidentTask\",\"elementInstanceKey\":2251799813685260,\"jobKey\":-1,\"variableScopeKey\":2251799813685260}}");
  }

  @Test
  public void shouldListProcesses() {
    // given

    Experimental experimental = new Experimental(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));

    experimental.visitDB(((cf, key, value) ->
    {

      System.out.printf("\nColumnFamily?: '%s'", cf);
      System.out.printf("\nKey: '%s'", new String(key));
      System.out.printf("\nValue: '%s'", new String(value));
    }));
  }

  @Test
  public void shouldGetProcessDetails() {
    // given

    // when
    final var processState = new ProcessState(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
    final var returnedProcess = deploymentEvent.getProcesses().get(0);
    final var processDetails = processState
        .processDetails(returnedProcess.getProcessDefinitionKey());

    // then
    assertThat(processDetails.getBpmnProcessId()).isEqualTo(returnedProcess.getBpmnProcessId());
    assertThat(processDetails.getProcessDefinitionKey())
        .isEqualTo(returnedProcess.getProcessDefinitionKey());
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


  @Test
  public void shouldNotFailOnNonExistingProcess() {
    // given

    // when
    final var processState = new ProcessState(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
    final var processDetails = processState
        .processDetails(0xCAFE);

    // then
    assertThat(processDetails).isNull();
  }

  @Test
  public void shouldGetProcessInstanceDetails() {
    // given

    // when
    final var processState = new InstanceState(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
    final var actualInstanceDetails = processState
        .instanceDetails(returnedProcessInstance.getProcessInstanceKey());

    // then
    assertThat(actualInstanceDetails).isNotNull();
    assertThat(actualInstanceDetails.getKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
    assertThat(actualInstanceDetails.getState()).isEqualTo(ProcessInstanceIntent.ELEMENT_ACTIVATED.toString());
    assertThat(actualInstanceDetails.getBpmnProcessId()).isEqualTo(returnedProcessInstance.getBpmnProcessId());
    assertThat(actualInstanceDetails.getProcessDefinitionKey())
        .isEqualTo(returnedProcessInstance.getProcessDefinitionKey());
    assertThat(actualInstanceDetails.getVersion()).isEqualTo(returnedProcessInstance.getVersion());
    assertThat(actualInstanceDetails.getProcessInstanceKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
    assertThat(actualInstanceDetails.getElementId()).isEqualTo("process");
    assertThat(actualInstanceDetails.getElementType()).isEqualTo(BpmnElementType.PROCESS);
    assertThat(actualInstanceDetails.getParentProcessInstanceKey()).isEqualTo(-1);
    assertThat(actualInstanceDetails.getParentElementInstanceKey()).isEqualTo(-1);
    assertThat(actualInstanceDetails.getFlowScopeKey()).isEqualTo(-1);
    assertThat(actualInstanceDetails.getChildren()).hasSize(2);

    assertThat(actualInstanceDetails.toString())
        .contains(returnedProcessInstance.getBpmnProcessId())
        .contains("" + returnedProcessInstance.getVersion())
        .contains("" + returnedProcessInstance.getProcessDefinitionKey())
        .contains(BpmnElementType.PROCESS.toString())
        .contains("process");
  }


  @Test
  public void shouldGetElementInstanceDetails() {
    // given
    final var processState = new InstanceState(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
    final var processInstance = processState
        .instanceDetails(returnedProcessInstance.getProcessInstanceKey());

    // when
    InstanceDetails actualElementInstance = processState
        .instanceDetails(processInstance.getChildren().get(1));

    // then
    assertThat(actualElementInstance).isNotNull();
    assertThat(actualElementInstance.getState()).isEqualTo(ProcessInstanceIntent.ELEMENT_ACTIVATED.toString());
    assertThat(actualElementInstance.getBpmnProcessId()).isEqualTo(returnedProcessInstance.getBpmnProcessId());
    assertThat(actualElementInstance.getProcessDefinitionKey())
        .isEqualTo(returnedProcessInstance.getProcessDefinitionKey());
    assertThat(actualElementInstance.getVersion()).isEqualTo(returnedProcessInstance.getVersion());
    assertThat(actualElementInstance.getProcessInstanceKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
    assertThat(actualElementInstance.getElementId()).isEqualTo("task");
    assertThat(actualElementInstance.getElementType()).isEqualTo(BpmnElementType.SERVICE_TASK);
    assertThat(actualElementInstance.getParentProcessInstanceKey()).isEqualTo(-1);
    assertThat(actualElementInstance.getParentElementInstanceKey()).isEqualTo(-1);
    assertThat(actualElementInstance.getJobKey()).isEqualTo(jobKey.get());
    assertThat(actualElementInstance.getFlowScopeKey()).isEqualTo(processInstance.getKey());
    assertThat(actualElementInstance.getChildren()).isEmpty();

    assertThat(actualElementInstance.toString())
        .contains(returnedProcessInstance.getBpmnProcessId())
        .contains("" + returnedProcessInstance.getVersion())
        .contains("" + returnedProcessInstance.getProcessDefinitionKey())
        .contains(BpmnElementType.SERVICE_TASK.toString())
        .contains("task");
  }


  @Test
  public void shouldListElementInstanceDetails() {
    // given
    final var processState = new InstanceState(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));

    // when
    final var detailsList = processState.listInstances();

    // then
    assertThat(detailsList).hasSize(3);

    final var actualInstanceDetails = detailsList.get(0);
    assertThat(actualInstanceDetails).isNotNull();
    assertThat(actualInstanceDetails.getKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
    assertThat(actualInstanceDetails.getBpmnProcessId()).isEqualTo(returnedProcessInstance.getBpmnProcessId());
    assertThat(actualInstanceDetails.getProcessDefinitionKey())
        .isEqualTo(returnedProcessInstance.getProcessDefinitionKey());
    assertThat(actualInstanceDetails.getVersion()).isEqualTo(returnedProcessInstance.getVersion());
    assertThat(actualInstanceDetails.getProcessInstanceKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
    assertThat(actualInstanceDetails.getElementId()).isEqualTo("process");
    assertThat(actualInstanceDetails.getElementType()).isEqualTo(BpmnElementType.PROCESS);
    assertThat(actualInstanceDetails.getParentProcessInstanceKey()).isEqualTo(-1);
    assertThat(actualInstanceDetails.getParentElementInstanceKey()).isEqualTo(-1);
    assertThat(actualInstanceDetails.getFlowScopeKey()).isEqualTo(-1);
    assertThat(actualInstanceDetails.getChildren()).hasSize(2);

    assertThat(actualInstanceDetails.toString())
        .contains(returnedProcessInstance.getBpmnProcessId())
        .contains("" + returnedProcessInstance.getVersion())
        .contains("" + returnedProcessInstance.getProcessDefinitionKey())
        .contains(BpmnElementType.PROCESS.toString())
        .contains("process");

    var actualElementInstance = detailsList.get(1);
    assertThat(actualElementInstance).isNotNull();
    assertThat(actualElementInstance.getBpmnProcessId()).isEqualTo(returnedProcessInstance.getBpmnProcessId());
    assertThat(actualElementInstance.getProcessDefinitionKey())
        .isEqualTo(returnedProcessInstance.getProcessDefinitionKey());
    assertThat(actualElementInstance.getVersion()).isEqualTo(returnedProcessInstance.getVersion());
    assertThat(actualElementInstance.getProcessInstanceKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
    assertThat(actualElementInstance.getElementId()).isEqualTo("incidentTask");
    assertThat(actualElementInstance.getElementType()).isEqualTo(BpmnElementType.SERVICE_TASK);
    assertThat(actualElementInstance.getParentProcessInstanceKey()).isEqualTo(-1);
    assertThat(actualElementInstance.getParentElementInstanceKey()).isEqualTo(-1);
    assertThat(actualElementInstance.getJobKey()).isZero(); // because of incident
    assertThat(actualElementInstance.getFlowScopeKey()).isEqualTo(actualInstanceDetails.getKey());
    assertThat(actualElementInstance.getChildren()).isEmpty();

    assertThat(actualElementInstance.toString())
        .contains(returnedProcessInstance.getBpmnProcessId())
        .contains("" + returnedProcessInstance.getVersion())
        .contains("" + returnedProcessInstance.getProcessDefinitionKey())
        .contains(BpmnElementType.SERVICE_TASK.toString())
        .contains("incidentTask");

    actualElementInstance = detailsList.get(2);
    assertThat(actualElementInstance).isNotNull();
    assertThat(actualElementInstance.getBpmnProcessId()).isEqualTo(returnedProcessInstance.getBpmnProcessId());
    assertThat(actualElementInstance.getProcessDefinitionKey())
        .isEqualTo(returnedProcessInstance.getProcessDefinitionKey());
    assertThat(actualElementInstance.getVersion()).isEqualTo(returnedProcessInstance.getVersion());
    assertThat(actualElementInstance.getProcessInstanceKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
    assertThat(actualElementInstance.getElementId()).isEqualTo("task");
    assertThat(actualElementInstance.getElementType()).isEqualTo(BpmnElementType.SERVICE_TASK);
    assertThat(actualElementInstance.getParentProcessInstanceKey()).isEqualTo(-1);
    assertThat(actualElementInstance.getParentElementInstanceKey()).isEqualTo(-1);
    assertThat(actualElementInstance.getJobKey()).isEqualTo(jobKey.get());
    assertThat(actualElementInstance.getFlowScopeKey()).isEqualTo(actualInstanceDetails.getKey());
    assertThat(actualElementInstance.getChildren()).isEmpty();

    assertThat(actualElementInstance.toString())
        .contains(returnedProcessInstance.getBpmnProcessId())
        .contains("" + returnedProcessInstance.getVersion())
        .contains("" + returnedProcessInstance.getProcessDefinitionKey())
        .contains(BpmnElementType.SERVICE_TASK.toString())
        .contains("task");
  }

  @Test
  public void shouldNotFailOnNonExistingProcessInstance() {
    // given

    // when
    final var processState = new InstanceState(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
    final var actualInstanceDetails = processState
        .instanceDetails(0xCAFE);

    // then
    assertThat(actualInstanceDetails).isNull();
  }

  @Test
  public void shouldGetIncidentDetails() {
    // given
    final var runtimePath = ZeebePaths.Companion.getRuntimePath(tempDir, "1");
    final var processState = new InstanceState(runtimePath);
    final var processInstance = processState
        .instanceDetails(returnedProcessInstance.getProcessInstanceKey());
    InstanceDetails actualElementInstance = processState
        .instanceDetails(processInstance.getChildren().get(0));
    final var incidentState = new IncidentState(runtimePath);
    final var incidentKey = incidentState.processInstanceIncidentKey(actualElementInstance.getKey());

    // when
    final var incidentDetails = incidentState.incidentDetails(incidentKey);

    // then
    assertThat(incidentDetails).isNotNull();
    assertThat(incidentDetails.getKey()).isEqualTo(incidentKey);
    assertThat(incidentDetails.getBpmnProcessId()).isEqualTo(returnedProcessInstance.getBpmnProcessId());
    assertThat(incidentDetails.getProcessDefinitionKey())
        .isEqualTo(returnedProcessInstance.getProcessDefinitionKey());
    assertThat(incidentDetails.getProcessInstanceKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
    assertThat(incidentDetails.getElementInstanceKey()).isEqualTo(actualElementInstance.getKey());
    assertThat(incidentDetails.getElementId()).isEqualTo("incidentTask");
    assertThat(incidentDetails.getErrorMessage()).isEqualTo("failed to evaluate expression '{bar:foo}': no variable found for name 'foo'");
    assertThat(incidentDetails.getErrorType()).isEqualTo(ErrorType.IO_MAPPING_ERROR);
    assertThat(incidentDetails.getVariablesScopeKey()).isEqualTo(actualElementInstance.getKey());
    assertThat(incidentDetails.getJobKey()).isEqualTo(-1);

    assertThat(incidentDetails.toString())
        .contains(returnedProcessInstance.getBpmnProcessId())
        .contains("incidentTask")
        .contains("" + returnedProcessInstance.getProcessDefinitionKey())
        .contains(ErrorType.IO_MAPPING_ERROR.toString())
        .contains("failed to evaluate expression '{bar:foo}': no variable found for name 'foo'");
  }

  @Test
  public void shouldListIncidentDetails() {
    // given
    final var runtimePath = ZeebePaths.Companion.getRuntimePath(tempDir, "1");
    final var processState = new InstanceState(runtimePath);
    final var processInstance = processState
        .instanceDetails(returnedProcessInstance.getProcessInstanceKey());
    InstanceDetails actualElementInstance = processState
        .instanceDetails(processInstance.getChildren().get(0));
    final var incidentState = new IncidentState(runtimePath);
    final var incidentKey = incidentState.processInstanceIncidentKey(actualElementInstance.getKey());

    // when
    final var incidents = incidentState.listIncidents();

    // then
    assertThat(incidents).hasSize(1);

    final var incidentDetails = incidents.get(0);
    assertThat(incidentDetails).isNotNull();
    assertThat(incidentDetails.getKey()).isEqualTo(incidentKey);
    assertThat(incidentDetails.getBpmnProcessId()).isEqualTo(returnedProcessInstance.getBpmnProcessId());
    assertThat(incidentDetails.getProcessDefinitionKey())
        .isEqualTo(returnedProcessInstance.getProcessDefinitionKey());
    assertThat(incidentDetails.getProcessInstanceKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
    assertThat(incidentDetails.getElementInstanceKey()).isEqualTo(actualElementInstance.getKey());
    assertThat(incidentDetails.getElementId()).isEqualTo("incidentTask");
    assertThat(incidentDetails.getErrorMessage()).isEqualTo("failed to evaluate expression '{bar:foo}': no variable found for name 'foo'");
    assertThat(incidentDetails.getErrorType()).isEqualTo(ErrorType.IO_MAPPING_ERROR);
    assertThat(incidentDetails.getVariablesScopeKey()).isEqualTo(actualElementInstance.getKey());
    assertThat(incidentDetails.getJobKey()).isEqualTo(-1);

    assertThat(incidentDetails.toString())
        .contains(returnedProcessInstance.getBpmnProcessId())
        .contains("incidentTask")
        .contains("" + returnedProcessInstance.getProcessDefinitionKey())
        .contains(ErrorType.IO_MAPPING_ERROR.toString())
        .contains("failed to evaluate expression '{bar:foo}': no variable found for name 'foo'");
  }


  @Test
  public void shouldReturnEmptyListOnNonExistingBannedInstances() {
    // given
    final var runtimePath = ZeebePaths.Companion.getRuntimePath(tempDir, "1");
    final var bannedInstanceState = new BannedInstanceState(runtimePath);

    // when
    final var bannedInstances = bannedInstanceState.listBannedInstances();

    // then
    assertThat(bannedInstances).isNotNull().isEmpty();
  }

  @Test
  public void shouldGetGeneralState() {
    // given
    final var runtimePath = ZeebePaths.Companion.getRuntimePath(tempDir, "1");
    final var generalState = new GeneralState(runtimePath);

    // when
    final var generalDetails = generalState.generalDetails();

    // then
    assertThat(generalDetails).isNotNull();

    final var exportingDetails = generalDetails.getExportingDetails();
    assertThat(exportingDetails).isNotNull();
    assertThat(exportingDetails.getExporters()).isEmpty();
    assertThat(exportingDetails.getLowestExportedPosition()).isZero();

    final var processingDetails = generalDetails.getProcessingDetails();
    assertThat(processingDetails).isNotNull();
    assertThat(processingDetails.getLastProcessedPosition()).isGreaterThan(1);

    final var incidentDetails = generalDetails.getIncidentDetails();
    assertThat(incidentDetails).isNotNull();
    assertThat(incidentDetails.getIncidents()).isEqualTo(1);
    assertThat(incidentDetails.getBannedInstances()).isZero();

    final var processInstancesDetails = generalDetails.getProcessInstancesDetails();
    assertThat(processInstancesDetails).isNotNull();
    assertThat(processInstancesDetails.getProcessInstanceCount()).isEqualTo(1);
    assertThat(processInstancesDetails.getElementInstanceCount()).isEqualTo(2);

    final var variablesDetails = generalDetails.getVariablesDetails();
    assertThat(variablesDetails).isNotNull();
    assertThat(variablesDetails.getVariablesCount()).isEqualTo(3);
    assertThat(variablesDetails.getMinSize()).isEqualTo(2);
    assertThat(variablesDetails.getMaxSize()).isEqualTo(4);
    assertThat(variablesDetails.getAvgSize()).isEqualTo(3);

    final var messageDetails = generalDetails.getMessageDetails();
    assertThat(messageDetails).isNotNull();
    assertThat(messageDetails.getCount()).isEqualTo(2);
    assertThat(messageDetails.getLastDeadline()).isNotZero();
    assertThat(messageDetails.getLastDeadline()).isGreaterThan(messageDetails.getNextDeadline());
    assertThat(messageDetails.getNextDeadline()).isNotZero();
    assertThat(messageDetails.getNextDeadline()).isLessThan(messageDetails.getLastDeadline());
  }

  @Test
  public void shouldProduceJsonOnGeneralState() {
    // given
    final var runtimePath = ZeebePaths.Companion.getRuntimePath(tempDir, "1");
    final var generalState = new GeneralState(runtimePath);
    final var generalDetails = generalState.generalDetails();

    // when
    final var jsonOutput = generalDetails.toString();

    // then
    assertThat(jsonOutput)
        .contains("processingDetails")
        .contains("lastProcessedPosition")
        .contains("exportingDetails")
        .contains("exporters")
        .contains("lowestExportedPosition")
        .contains("incidentDetails")
        .contains("messageDetails")
        .contains("processInstancesDetails")
        .contains("variablesDetails");
  }
}
