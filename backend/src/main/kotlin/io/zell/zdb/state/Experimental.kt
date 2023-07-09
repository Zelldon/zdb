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
package io.zell.zdb.state

import io.camunda.zeebe.db.impl.ZeebeDbConstants
import io.camunda.zeebe.protocol.ZbColumnFamilies
import io.camunda.zeebe.protocol.impl.encoding.MsgPackConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.agrona.concurrent.UnsafeBuffer
import org.rocksdb.OptimisticTransactionDB
import org.rocksdb.ReadOptions
import org.rocksdb.RocksDB
import java.nio.file.Path
import java.util.*

class Experimental(private var rocksDb: RocksDB) {
        constructor(statePath: Path) : this(OptimisticTransactionDB.openReadOnly(statePath.toString()))


    fun interface Visitor {
        fun visit(cf: ZbColumnFamilies, key: ByteArray, value: ByteArray)
    }

    fun interface JsonVisitor {
        fun visit(cf: ZbColumnFamilies, key: ByteArray, valueJson: String)
    }

    fun visitDB(visitor: Visitor) {
       rocksDb.newIterator(rocksDb.defaultColumnFamily, ReadOptions()).use {
           it.seekToFirst()
           while (it.isValid) {
               val key: ByteArray = it.key()
               val value: ByteArray = it.value()
               val unsafeBuffer = UnsafeBuffer(key)
               val enumValue = unsafeBuffer.getLong(0, ZeebeDbConstants.ZB_DB_BYTE_ORDER)
               val cf = ZbColumnFamilies.values()[enumValue.toInt()]
               visitor.visit(cf, key, value)
               it.next()
           }
       }
    }

    fun visitDBWithJsonValues(visitor: JsonVisitor) {
        visitDB { cf, key, value ->
            val jsonValue = MsgPackConverter.convertToJson(value)
            visitor.visit(cf, key, jsonValue)
        }
    }

    fun stateStatistics() : Map<ZbColumnFamilies, Int> {
        val countMap = EnumMap<ZbColumnFamilies, Int>(ZbColumnFamilies::class.java)

        visitDB { cf, _, _ ->
            val count: Int = countMap.computeIfAbsent(cf) { 0 }
            countMap[cf] = count + 1
        }
        return countMap
    }

    fun stateStatisticsAsJsonString() : String {
        val stateStatistics = stateStatistics()
        return Json.encodeToString(stateStatistics)
    }
}