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
package io.zell.zdb.log.records

class ApplicationRecord(val index: Long, val term: Long, val highestPosition: Long, val lowestPosition: Long) :
    PersistedRecord {
    val entries = mutableListOf<Record>()

    override fun index(): Long {
        return index;
    }

    override fun term(): Long {
        return term;
    }

    override fun toString(): String {
        val entriesJson = entriesAsJson()
        return """{"index":$index, "term":$term,"highestPosition":$highestPosition,"lowestPosition":$lowestPosition,"entries":[${entriesJson}]}"""
    }

    fun entriesAsJson() : String {
        return entries.map {
            it.toString()
        }.joinToString()
    }


    fun entryAsColumn(record: Record): String {
//        val record = Json{ignoreUnknownKeys = true}.decodeFromString<Record>(recordAsJson)
        val stringBuilder = StringBuilder()
        val separator = " "
        stringBuilder
            .append(record.position)
            .append(separator)
            .append(record.sourceRecordPosition)
            .append(separator)
            .append(record.timestamp)
            .append(separator)
            .append(record.key)
            .append(separator)
            .append(record.recordType)
            .append(separator)
            .append(record.valueType)
            .append(separator)
            .append(record.intent)

        val piRelatedValue = record.piRelatedValue
        piRelatedValue?.let {
            piRelatedValue.processInstanceKey?.let {
                stringBuilder
                    .append(separator)
                    .append(it)
                    .append(separator)
            }

            piRelatedValue.bpmnElementType?.let {
                stringBuilder
                    .append(it)
                    .append(separator)
            }
        }
        return stringBuilder.toString()
    }

    override fun asColumnString(): String {
        val prefix = """$index $term """
        val stringBuilder = StringBuilder()
        entries.forEach {
            stringBuilder.append(prefix).append(entryAsColumn(it)).appendLine()
        }
        return stringBuilder.toString()
    }
}
