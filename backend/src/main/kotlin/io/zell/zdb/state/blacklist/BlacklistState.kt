package io.zell.zdb.state.blacklist

import io.camunda.zeebe.db.impl.DbLong
import io.camunda.zeebe.db.impl.DbNil
import io.camunda.zeebe.engine.state.ZbColumnFamilies
import io.camunda.zeebe.engine.state.ZeebeDbState
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import java.nio.file.Path

class BlacklistState(private var readonlyDb: ReadonlyTransactionDb) {

    constructor(statePath: Path) : this(ReadonlyTransactionDb.openReadonlyDb(statePath))

    fun listBlacklistedInstances() : List<Long> {
        val blacklistedInstances = mutableListOf<Long>()

        val processInstanceKey = DbLong()
        val blackListColumnFamily =
            readonlyDb.createColumnFamily(ZbColumnFamilies.BLACKLIST,
                readonlyDb.createContext(),
                processInstanceKey,
                DbNil.INSTANCE)

        blackListColumnFamily.forEach { key, _ -> blacklistedInstances.add(key.value) }


        return blacklistedInstances
    }

}
