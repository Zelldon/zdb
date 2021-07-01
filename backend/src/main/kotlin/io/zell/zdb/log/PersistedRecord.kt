package io.zell.zdb.log

interface PersistedRecord {
    fun index() : Long
    fun term() : Long
    override fun toString() : String
}
