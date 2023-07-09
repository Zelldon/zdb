/*
 * Copyright © 2021 Christopher Kujawa (zelldon91@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zell.zdb.db.readonly.transaction

import io.camunda.zeebe.protocol.ZbColumnFamilies
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.OptimisticTransactionDB
import org.rocksdb.RocksDB
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
