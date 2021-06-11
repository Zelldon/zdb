package io.zell.zdb.state.instance

import io.camunda.zeebe.engine.state.ZeebeDbState
import io.camunda.zeebe.engine.state.instance.ElementInstance
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import java.nio.file.Path

class InstanceState(statePath: Path) {

    private var zeebeDbState: ZeebeDbState

    init {
        val readonlyDb = ReadonlyTransactionDb.openReadonlyDb(statePath)
        zeebeDbState = ZeebeDbState(readonlyDb, readonlyDb.createContext())
    }

    fun instanceDetails(elementInstanceKey : Long): InstanceDetails? {
        val instance : ElementInstance? = zeebeDbState.elementInstanceState.getInstance(elementInstanceKey)

        if (instance != null) {
            val children = zeebeDbState.elementInstanceState.getChildren(elementInstanceKey)
            return InstanceDetails(instance, children)
        }
        return null
    }
}
