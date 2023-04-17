package io.zell.zdb.state.process

import io.camunda.zeebe.engine.state.ProcessingDbState
import io.camunda.zeebe.engine.state.deployment.DeployedProcess
import io.camunda.zeebe.engine.state.immutable.ProcessingState
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import java.nio.file.Path

class ProcessState(statePath: Path) {

    private var zeebeDbState: ProcessingState

    init {
        val readonlyDb = ReadonlyTransactionDb.openReadonlyDb(statePath)
        zeebeDbState = ProcessingDbState(1, readonlyDb, readonlyDb.createContext(), { 1 })
    }

    fun listProcesses(): List<ProcessMeta> {
        return zeebeDbState
            .processState
            .processes.map { ProcessMeta(it) }
    }

    fun processDetails(processDefinitionKey : Long): ProcessDetails? {
        val deployedProcess : DeployedProcess? = zeebeDbState
            .processState
            .getProcessByKey(processDefinitionKey)
        return if (deployedProcess != null) ProcessDetails(deployedProcess) else null
    }
}
