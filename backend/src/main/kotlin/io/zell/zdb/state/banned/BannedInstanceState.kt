package io.zell.zdb.state.banned

import io.camunda.zeebe.db.impl.DbLong
import io.camunda.zeebe.db.impl.DbNil
import io.camunda.zeebe.protocol.ZbColumnFamilies
import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import java.nio.file.Path

class BannedInstanceState(private var readonlyDb: ReadonlyTransactionDb) {

    constructor(statePath: Path) : this(ReadonlyTransactionDb.openReadonlyDb(statePath))

    fun listBannedInstances() : List<Long> {
        val bannedInstances = mutableListOf<Long>()

        val processInstanceKey = DbLong()
        val bannedInstanceColumnFamily =
            readonlyDb.createColumnFamily(
                ZbColumnFamilies.BANNED_INSTANCE,
                readonlyDb.createContext(),
                processInstanceKey,
                DbNil.INSTANCE)

        bannedInstanceColumnFamily.forEach { key, _ -> bannedInstances.add(key.value) }


        return bannedInstances
    }

}
