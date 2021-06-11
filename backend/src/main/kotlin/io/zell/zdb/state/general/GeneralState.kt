package io.zell.zdb.state.general

import io.camunda.zeebe.engine.state.ZeebeDbState
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import java.nio.file.Path

class GeneralState(statePath: Path) {

    private var zeebeDbState: ZeebeDbState

    init {
        val readonlyDb = ReadonlyTransactionDb.openReadonlyDb(statePath)
        zeebeDbState = ZeebeDbState(readonlyDb, readonlyDb.createContext())
    }

    fun generalState(): GeneralState {
        throw UnsupportedOperationException("not yet implemented")
    }
}
