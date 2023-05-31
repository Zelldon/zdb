package io.zell.zdb.log

import io.camunda.zeebe.stream.impl.records.TypedRecordImpl

class ApplicationRecord(val index: Long, val term : Long) : PersistedRecord {
    val entries = mutableListOf<TypedRecordImpl>()

    override fun index(): Long {
        return index;
    }

    override fun term(): Long {
        return term;
    }

    override fun toString(): String {

        val entriesJson = entries.map { it.toJson() }.joinToString()
        return "{\"index\":$index, \"term\":$term, \"entries\":[${entriesJson}]}"
    }
}
