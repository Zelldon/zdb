package io.zell.zdb.state.instance

import io.camunda.zeebe.db.ColumnFamily
import io.camunda.zeebe.db.impl.DbLong
import io.camunda.zeebe.engine.state.ProcessingDbState
import io.camunda.zeebe.engine.state.immutable.ProcessingState
import io.camunda.zeebe.engine.state.instance.ElementInstance
import io.camunda.zeebe.protocol.ZbColumnFamilies
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent
import io.camunda.zeebe.protocol.record.value.BpmnElementType
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import java.nio.file.Path
import java.util.function.Predicate

class InstanceState(readonlyTransactionDb: ReadonlyTransactionDb) {

    private var zeebeDbState: ProcessingState
    private var readonlyDb : ReadonlyTransactionDb

    init {
        readonlyDb = readonlyTransactionDb
        zeebeDbState = ProcessingDbState(1, readonlyDb, readonlyDb.createContext(), { 1 })
    }

    constructor(statePath: Path) : this(ReadonlyTransactionDb.openReadonlyDb(statePath))

    fun instanceDetails(elementInstanceKey : Long): InstanceDetails? {
        val instance : ElementInstance? = zeebeDbState.elementInstanceState.getInstance(elementInstanceKey)

        if (instance != null) {
            val children = zeebeDbState.elementInstanceState.getChildren(elementInstanceKey)
            return InstanceDetails(instance, children)
        }
        return null
    }

    fun listInstances(): List<InstanceDetails> {
        val elementInstanceColumnFamily = createElementInstanceCF()

        val instances = mutableListOf<InstanceDetails>()
        elementInstanceColumnFamily
            .forEach { key, element  -> instances.add(instanceDetails(key.value)!!) }

        return instances
    }

    fun listProcessInstances(predicate: Predicate<InstanceDetails>): List<InstanceDetails> {
        val elementInstanceColumnFamily = createElementInstanceCF()

        val instances = mutableListOf<InstanceDetails>()
        elementInstanceColumnFamily
            .forEach { key, _  ->
                    val instanceDetails = instanceDetails(key.value)!!
                if (instanceDetails.elementType == BpmnElementType.PROCESS && predicate.test(instanceDetails)) {
                    instances.add(instanceDetails)
                }
            }

        return instances
    }

    private fun createElementInstanceCF(): ColumnFamily<DbLong, ElementInstance> {
        val elementInstanceKey = DbLong()
        val elementInstance = ElementInstance(-1, ProcessInstanceIntent.ACTIVATE_ELEMENT, ProcessInstanceRecord())

        val elementInstanceColumnFamily = readonlyDb.createColumnFamily(
            ZbColumnFamilies.ELEMENT_INSTANCE_KEY,
            readonlyDb.createContext(),
            elementInstanceKey,
            elementInstance
        )
        return elementInstanceColumnFamily
    }
}
