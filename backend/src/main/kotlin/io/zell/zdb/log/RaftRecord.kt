package io.zell.zdb.log

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class RaftRecord(val index: Long, val term : Long) : PersistedRecord {
    override fun index(): Long {
        return index;
    }

    override fun term(): Long {
        return term;
    }

    override fun toString(): String {
        return Json.encodeToString(this);
    }
}
