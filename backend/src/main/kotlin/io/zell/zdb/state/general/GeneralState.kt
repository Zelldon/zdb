package io.zell.zdb.state.general

import io.camunda.zeebe.db.impl.DbCompositeKey
import io.camunda.zeebe.db.impl.DbLong
import io.camunda.zeebe.db.impl.DbNil
import io.camunda.zeebe.db.impl.DbString
import io.camunda.zeebe.engine.state.ProcessingDbState
import io.camunda.zeebe.engine.state.immutable.ProcessingState
import io.camunda.zeebe.engine.state.variable.VariableInstance
import io.camunda.zeebe.protocol.ZbColumnFamilies
import io.camunda.zeebe.stream.impl.state.DbLastProcessedPositionState
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import io.zell.zdb.state.incident.IncidentState
import io.zell.zdb.state.instance.InstanceState
import java.nio.file.Path

class GeneralState(statePath: Path) {

    private var zeebeDbState: ProcessingState
    private var readonlyDb: ReadonlyTransactionDb

    init {
        readonlyDb = ReadonlyTransactionDb.openReadonlyDb(statePath)
        zeebeDbState = ProcessingDbState(1, readonlyDb, readonlyDb.createContext(), { 1 })
    }

    fun generalDetails(): GeneralDetails {
        return GeneralDetails(
                processingDetails(),
                exporterDetails(),
                incidentDetails(),
                messagesDetails(),
                instanceDetails(),
                variablesDetails())
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
            if (lowestPosition > exporterPosition) {
                lowestPosition = exporterPosition
            }
        }

        if (exporters.isEmpty())
        {
            lowestPosition = 0
        }

        return ExportingDetails(exporters, lowestPosition)
    }

    private fun processingDetails(): ProcessingDetails {
        return ProcessingDetails(DbLastProcessedPositionState(readonlyDb, readonlyDb.createContext()).lastSuccessfulProcessedRecordPosition)
    }

    private fun incidentDetails(): IncidentDetails {

        val processInstanceKey = DbLong()
        val bannedInstanceCF = readonlyDb.createColumnFamily(
            ZbColumnFamilies.BANNED_INSTANCE,
            readonlyDb.createContext(),
            processInstanceKey,
            DbNil.INSTANCE
        )

        var bannedInstances = 0L
        bannedInstanceCF.forEach { key -> bannedInstances++ }

        return IncidentDetails(
            bannedInstances,
            IncidentState(readonlyDb).listIncidents().count().toLong()
        )
    }

    private fun instanceDetails(): ProcessInstancesDetails {
        var elementInstanceCount = 0L
        var processInstanceCount = 0L

        InstanceState(readonlyDb).listInstances().forEach { instance ->

            if (instance.processInstanceKey == instance.key) {
                processInstanceCount++
            } else {
                elementInstanceCount++
            }
        }

        return ProcessInstancesDetails(processInstanceCount, elementInstanceCount)
    }

    private fun variablesDetails(): VariablesDetails {

        val scopeKey = DbLong()
        val variableName = DbString()
        val scopeKeyVariableNameKey = DbCompositeKey<DbLong, DbString>(scopeKey, variableName)
        val variablesColumnFamily =
            readonlyDb.createColumnFamily(
                ZbColumnFamilies.VARIABLES,
                readonlyDb.createContext(),
                scopeKeyVariableNameKey,
                VariableInstance()
            )

        var maxSize = Long.MIN_VALUE
        var minSize = Long.MAX_VALUE
        var avgSize = 0.0
        var variablesCount = 0L
        variablesColumnFamily.forEach { key, variable ->
            val variableSize = variable.value.capacity().toLong()

            if (variableSize < minSize) {
                minSize = variableSize
            }

            if (variableSize > maxSize) {
                maxSize = variableSize
            }

            avgSize += variableSize
            variablesCount++

        }

        if (variablesCount > 0) {
            avgSize /= variablesCount
        }

        return VariablesDetails(variablesCount, maxSize, minSize, avgSize)
    }

    private fun messagesDetails(): MessagesDetails {
        val messageKey = DbLong()

        val deadlineKey = DbLong()
        val deadlineMessageKey = DbCompositeKey(deadlineKey, messageKey)
        val deadlineColumnFamily = readonlyDb
            .createColumnFamily(
                ZbColumnFamilies.MESSAGE_DEADLINES,
                readonlyDb.createContext(),
                deadlineMessageKey,
                DbNil.INSTANCE
            )

        var messageCount = 0L
        var firstDeadline = Long.MAX_VALUE
        var lastDeadline = 0L
        deadlineColumnFamily.forEach { key, msg ->
            messageCount++
            val deadline = key.first().value

            if (firstDeadline > deadline)
            {
                firstDeadline = deadline
            }
            lastDeadline = deadline
        }

        return MessagesDetails(messageCount, firstDeadline, lastDeadline)
    }


}
