package io.zell.zdb.state.incident

import io.camunda.zeebe.db.impl.DbLong
import io.camunda.zeebe.engine.state.ProcessingDbState
import io.camunda.zeebe.engine.state.immutable.ProcessingState
import io.camunda.zeebe.engine.state.instance.Incident
import io.camunda.zeebe.protocol.ZbColumnFamilies
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import java.nio.file.Path

class IncidentState(readonlyDb: ReadonlyTransactionDb) {

    private var zeebeDbState: ProcessingState
    private var readonlyDb: ReadonlyTransactionDb

    init {
        this.readonlyDb = readonlyDb
        zeebeDbState = ProcessingDbState(1, readonlyDb, readonlyDb.createContext(), { 1 })
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

        return IncidentDetails(incidentKey, incidentRecord)
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
