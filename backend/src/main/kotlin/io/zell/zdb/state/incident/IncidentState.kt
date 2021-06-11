package io.zell.zdb.state.incident

import io.camunda.zeebe.db.impl.DbLong
import io.camunda.zeebe.engine.state.ZbColumnFamilies
import io.camunda.zeebe.engine.state.ZeebeDbState
import io.camunda.zeebe.engine.state.instance.Incident
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import java.nio.file.Path

class IncidentState(readonlyDb: ReadonlyTransactionDb) {

    private var zeebeDbState: ZeebeDbState
    private var readonlyDb: ReadonlyTransactionDb

    init {
        this.readonlyDb = readonlyDb
        zeebeDbState = ZeebeDbState(readonlyDb, readonlyDb.createContext())
    }

    constructor(statePath: Path) : this(ReadonlyTransactionDb.openReadonlyDb(statePath))

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

    fun listIncidents(): List<IncidentDetails> {
        val incidentKey = DbLong()
        val incident = Incident()
        val incidentColumnFamily = readonlyDb.createColumnFamily(ZbColumnFamilies.INCIDENTS, readonlyDb.createContext(), incidentKey, incident)

        val incidents = mutableListOf<IncidentDetails>()
        incidentColumnFamily.forEach { key, _ -> incidents.add(incidentDetails(key.value))}

        return incidents
    }

}
