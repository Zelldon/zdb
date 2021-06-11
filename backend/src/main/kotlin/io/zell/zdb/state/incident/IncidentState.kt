package io.zell.zdb.state.incident

import io.camunda.zeebe.engine.state.ZeebeDbState
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import java.nio.file.Path

class IncidentState(statePath: Path) {

    private var zeebeDbState: ZeebeDbState

    init {
        val readonlyDb = ReadonlyTransactionDb.openReadonlyDb(statePath)
        zeebeDbState = ZeebeDbState(readonlyDb, readonlyDb.createContext())
    }

    fun jobIncidentKey(jobKey : Long): Long {
        return zeebeDbState.incidentState.getJobIncidentKey(jobKey)
    }

    fun processInstanceIncidentKey(elementInstanceKey: Long): Long {
        return zeebeDbState.incidentState.getProcessInstanceIncidentKey(elementInstanceKey)
    }

    fun incidentDetails(incidentKey : Long): IncidentDetails {
        val incidentState = zeebeDbState.incidentState
        val incidentRecord = incidentState.getIncidentRecord(incidentKey)

        return IncidentDetails(incidentRecord)
    }

}
