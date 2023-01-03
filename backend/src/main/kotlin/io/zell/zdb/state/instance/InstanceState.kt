package io.zell.zdb.state.instance

import io.camunda.zeebe.db.impl.DbLong
import io.camunda.zeebe.engine.state.ZbColumnFamilies
import io.camunda.zeebe.engine.state.ZeebeDbState
import io.camunda.zeebe.engine.state.instance.ElementInstance
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import java.nio.file.Path

class InstanceState(readonlyTransactionDb: ReadonlyTransactionDb) {

    private var zeebeDbState: ZeebeDbState
    private var readonlyDb : ReadonlyTransactionDb

    init {
        readonlyDb = readonlyTransactionDb
        zeebeDbState = ZeebeDbState(1, readonlyDb, readonlyDb.createContext(), { 1 })
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
