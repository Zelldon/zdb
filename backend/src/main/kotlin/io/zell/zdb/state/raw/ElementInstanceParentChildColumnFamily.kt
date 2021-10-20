package io.zell.zdb.state.raw

import io.camunda.zeebe.db.ColumnFamily
import io.camunda.zeebe.db.TransactionContext
import io.camunda.zeebe.db.ZeebeDb
import io.camunda.zeebe.db.impl.DbCompositeKey
import io.camunda.zeebe.db.impl.DbLong
import io.camunda.zeebe.db.impl.DbNil
import io.camunda.zeebe.engine.state.ZbColumnFamilies

class ElementInstanceParentChildColumnFamily(
    zeebeDb: ZeebeDb<ZbColumnFamilies>,
    transactionContext: TransactionContext
) {


    private val parentKey: DbLong = DbLong()
    private val elementInstanceKey: DbLong = DbLong()
    private val parentChildKey: DbCompositeKey<DbLong, DbLong> =
        DbCompositeKey(parentKey, elementInstanceKey)
    private var parentChildColumnFamily: ColumnFamily<DbCompositeKey<DbLong, DbLong>, DbNil>


    init {
        parentChildColumnFamily = zeebeDb.createColumnFamily(
            ZbColumnFamilies.ELEMENT_INSTANCE_PARENT_CHILD,
            transactionContext,
            parentChildKey,
            DbNil.INSTANCE
        )
    }

    fun acceptWhileTrue(visitor: Visitor) {
        parentChildColumnFamily.whileTrue { key, _ ->
            visitor.visit(
                ElementInstanceParentChildEntry(
                    key.first.value,
                    key.second.value,
                )
            )
        }
    }

    data class ElementInstanceParentChildEntry(
        val parentKey: Long,
        val elementInstanceKey: Long,
    )

    fun interface Visitor {
        fun visit(entry: ElementInstanceParentChildEntry): Boolean
    }

}