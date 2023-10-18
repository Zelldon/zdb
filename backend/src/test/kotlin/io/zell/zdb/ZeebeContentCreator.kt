package io.zell.zdb

import io.camunda.zeebe.client.ZeebeClient
import io.camunda.zeebe.model.bpmn.Bpmn
import io.camunda.zeebe.model.bpmn.BpmnModelInstance
import java.time.Duration
import java.util.Map

class ZeebeContentCreator(val gatewayAddress: String, val processModel: BpmnModelInstance) {

    private val SIMPLE_PROCESS = Bpmn.createExecutableProcess("simple")
        .startEvent()
        .endEvent()
        .done()
   fun createContent() {
       val client = ZeebeClient.newClientBuilder()
           .gatewayAddress(gatewayAddress)
           .usePlaintext()
           .build()

       client.newDeployResourceCommand()
           .addProcessModel(processModel, "process.bpmn")
           .addProcessModel(SIMPLE_PROCESS, "simple.bpmn")
           .send()
           .join()

       client
           .newCreateInstanceCommand()
           .bpmnProcessId("process")
           .latestVersion()
           .variables(Map.of<String, Any>("var1", "1", "var2", "12", "var3", "123"))
           .send()
           .join()

       client.newPublishMessageCommand().messageName("msg").correlationKey("123")
           .timeToLive(Duration.ofSeconds(1)).send().join()
       client.newPublishMessageCommand().messageName("msg12").correlationKey("123")
           .timeToLive(Duration.ofHours(1)).send().join()

       // Small hack to ensure we reached the task and job of the previous PI
       // If the process instance we start after, ended we can be sure that
       // we reached also the wait-state in the other PI.
       client
           .newCreateInstanceCommand()
           .bpmnProcessId("simple")
           .latestVersion()
           .withResult()
           .send()
           .join()

       var responseJobKey = 0L
       do {
           val activateJobsResponse = client.newActivateJobsCommand().jobType("type")
               .maxJobsToActivate(1).send()
               .join()
           if (activateJobsResponse != null && !activateJobsResponse.jobs.isEmpty()) {
               responseJobKey = activateJobsResponse.jobs[0].key
           }
       } while (responseJobKey <= 0)

       client.close()
    }

}
