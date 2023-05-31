package io.zell.zdb.state.instance

import io.camunda.zeebe.db.impl.DbLong
import io.camunda.zeebe.engine.state.ProcessingDbState
import io.camunda.zeebe.engine.state.immutable.ProcessingState
import io.camunda.zeebe.engine.state.instance.ElementInstance
import io.camunda.zeebe.protocol.ZbColumnFamilies
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import java.nio.file.Path

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
        val elementInstanceKey = DbLong()
        val elementInstance = ElementInstance(-1, ProcessInstanceIntent.ACTIVATE_ELEMENT, ProcessInstanceRecord())

        val elementInstanceColumnFamily = readonlyDb.createColumnFamily(
            ZbColumnFamilies.ELEMENT_INSTANCE_KEY,
            readonlyDb.createContext(),
            elementInstanceKey,
            elementInstance
        )

        val instances = mutableListOf<InstanceDetails>()
        elementInstanceColumnFamily
            .forEach { key, element  -> instances.add(instanceDetails(key.value)!!) }

        return instances
    }
}
