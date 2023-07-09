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
