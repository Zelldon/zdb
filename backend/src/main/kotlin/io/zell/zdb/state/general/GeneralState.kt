package io.zell.zdb.state.general

import io.camunda.zeebe.db.impl.DbLong
import io.camunda.zeebe.db.impl.DbNil
import io.camunda.zeebe.db.impl.DbString
import io.camunda.zeebe.engine.state.ZbColumnFamilies
import io.camunda.zeebe.engine.state.ZeebeDbState
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import io.zell.zdb.state.incident.IncidentState
import io.zell.zdb.state.instance.InstanceState
import java.nio.file.Path

class GeneralState(statePath: Path) {

    private var zeebeDbState: ZeebeDbState
    private var readonlyDb: ReadonlyTransactionDb

    init {
        readonlyDb = ReadonlyTransactionDb.openReadonlyDb(statePath)
        zeebeDbState = ZeebeDbState(readonlyDb, readonlyDb.createContext())
    }

    fun generalState(): GeneralState {
        throw UnsupportedOperationException("not yet implemented")
    }

    private fun exporterDetails(): ExportingDetails {
        val exporterId = DbString()
        val position = ExporterPosition()

        val exporterPositionColumnFamily =
            readonlyDb.createColumnFamily(
                ZbColumnFamilies.EXPORTER,
                readonlyDb.createContext(),
                exporterId,
                position
            )

        val exporters = mutableMapOf<String, Long>()
        var lowestPosition = Long.MAX_VALUE
        exporterPositionColumnFamily.forEach { id, pos ->
            val exporterPosition = pos.get()
            exporters[id.toString()] = exporterPosition
            if (lowestPosition > exporterPosition)
            {
                lowestPosition = exporterPosition
            }
        }

        return ExportingDetails(exporters, lowestPosition)
    }

    private fun processingDetails(): ProcessingDetails {
        return ProcessingDetails(zeebeDbState.lastProcessedPositionState.lastSuccessfulProcessedRecordPosition)
    }

    private fun incidentDetails(): IncidentDetails {

        val processInstanceKey = DbLong()
        val blackListColumnFamily = readonlyDb.createColumnFamily(
            ZbColumnFamilies.BLACKLIST, readonlyDb.createContext(), processInstanceKey, DbNil.INSTANCE
        )

        var blacklistedInstances = 0L
        blackListColumnFamily.forEach { key -> blacklistedInstances++}

        return IncidentDetails(blacklistedInstances,
            IncidentState(readonlyDb).listIncidents().count().toLong()
        )
    }

    private fun instanceDetails(): ProcessInstancesDetails {
        var elementInstanceCount = 0L
        var processInstanceCount = 0L

        InstanceState(readonlyDb).listInstances().forEach { instance ->

            if (instance.processInstanceKey == instance.key)
            {
                processInstanceCount++
            }
            else
            {
                elementInstanceCount++
            }
        }

        return ProcessInstancesDetails(processInstanceCount, elementInstanceCount)
    }


}
