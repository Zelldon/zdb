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
