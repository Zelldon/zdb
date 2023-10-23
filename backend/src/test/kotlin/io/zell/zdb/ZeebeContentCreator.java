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
package io.zell.zdb;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import java.time.Duration;
import java.util.Map;

public class ZeebeContentCreator {

    private final String gatewayAddress;
    private final BpmnModelInstance processModel;


    public ZeebeContentCreator(String gatewayAddress, BpmnModelInstance processModel) {
        this.gatewayAddress = gatewayAddress;
        this.processModel = processModel;
    }

    private static final BpmnModelInstance SIMPLE_PROCESS = Bpmn.createExecutableProcess("simple")
        .startEvent()
        .endEvent()
        .done();

    public DeploymentEvent deploymentEvent;
    public ProcessInstanceEvent processInstanceEvent;

   public void createContent() {
       final var client = ZeebeClient.newClientBuilder()
           .gatewayAddress(gatewayAddress)
           .usePlaintext()
           .build();

       deploymentEvent = client.newDeployResourceCommand()
           .addProcessModel(processModel, "process.bpmn")
           .addProcessModel(SIMPLE_PROCESS, "simple.bpmn")
           .send()
           .join();

       processInstanceEvent = client
           .newCreateInstanceCommand()
           .bpmnProcessId("process")
           .latestVersion()
           .variables(Map.of("var1", "1", "var2", "12", "var3", "123"))
           .send()
           .join();

       client.newPublishMessageCommand().messageName("msg").correlationKey("123")
           .timeToLive(Duration.ofSeconds(1)).send().join();
       client.newPublishMessageCommand().messageName("msg12").correlationKey("123")
           .timeToLive(Duration.ofHours(1)).send().join();

       // Small hack to ensure we reached the task and job of the previous PI
       // If the process instance we start after, ended we can be sure that
       // we reached also the wait-state in the other PI.
       client
           .newCreateInstanceCommand()
           .bpmnProcessId("simple")
           .latestVersion()
           .withResult()
           .send()
           .join();

       var responseJobKey = 0L;
       do {
           final var activateJobsResponse = client.newActivateJobsCommand().jobType("type")
               .maxJobsToActivate(1).send()
               .join();
           if (activateJobsResponse != null && !activateJobsResponse.getJobs().isEmpty()) {
               responseJobKey = activateJobsResponse.getJobs().get(0).getKey();
           }
       } while (responseJobKey <= 0);

       client.close();
    }

}
