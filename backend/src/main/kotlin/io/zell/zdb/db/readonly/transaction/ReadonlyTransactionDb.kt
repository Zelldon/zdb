package io.zell.zdb.db.readonly.transaction

import io.camunda.zeebe.db.impl.rocksdb.RocksDbConfiguration
import io.camunda.zeebe.engine.state.ZbColumnFamilies
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.OptimisticTransactionDB
import org.rocksdb.RocksDB
import java.io.File
import java.nio.file.Path

public class ReadonlyTransactionDb : ZeebeTransactionDb<ZbColumnFamilies> {

    constructor(
        defaultHandle: ColumnFamilyHandle?,
        optimisticTransactionDB: RocksDB?,
        closables: MutableList<AutoCloseable>?
    ) : super(defaultHandle, optimisticTransactionDB, closables)
    companion object {
        fun openReadonlyDb(path : Path) : ReadonlyTransactionDb {
            var closables = mutableListOf<AutoCloseable>()
            val rocksDB = OptimisticTransactionDB.openReadOnly(path.toString())
            closables.add(rocksDB)
            return ReadonlyTransactionDb(rocksDB.defaultColumnFamily, rocksDB, closables)

        }
    }
}
