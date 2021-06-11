package io.zell.zdb

import io.camunda.zeebe.engine.state.ZeebeDbState
import io.camunda.zeebe.util.buffer.BufferUtil
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb.Companion.openReadonlyDb
import java.nio.file.Path

class ProcessInspection(var statePath: Path) {

    fun listProcesses() : List<String> {
        openReadonlyDb(statePath).use {
            val zeebeDbState = ZeebeDbState(it, it.createContext())
            return zeebeDbState
                .processState
                .processes.map {
                    "Process ${BufferUtil.bufferAsString(it.resourceName)}," +
                            " key: ${it.key}," +
                            " processId: ${BufferUtil.bufferAsString(it.bpmnProcessId)}," +
                            " version: ${it.version}"
                }
        }
    }
}
