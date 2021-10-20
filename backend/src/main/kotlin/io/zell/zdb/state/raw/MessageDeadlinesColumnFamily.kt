package io.zell.zdb.state.raw

import io.camunda.zeebe.db.ColumnFamily
import io.camunda.zeebe.db.TransactionContext
import io.camunda.zeebe.db.ZeebeDb
import io.camunda.zeebe.db.impl.DbCompositeKey
import io.camunda.zeebe.db.impl.DbLong
import io.camunda.zeebe.db.impl.DbNil
import io.camunda.zeebe.engine.state.ZbColumnFamilies

class MessageDeadlinesColumnFamily(
    zeebeDb: ZeebeDb<ZbColumnFamilies>,
    transactionContext: TransactionContext
) {

    private var deadline: DbLong = DbLong()
    private val messageKey: DbLong = DbLong()
    private var deadlineMessageKey: DbCompositeKey<DbLong, DbLong> =
        DbCompositeKey(deadline, messageKey)
    private var deadlineColumnFamily: ColumnFamily<DbCompositeKey<DbLong, DbLong>, DbNil>

    init {
        deadlineColumnFamily = zeebeDb.createColumnFamily<DbCompositeKey<DbLong, DbLong>, DbNil>(
            ZbColumnFamilies.MESSAGE_DEADLINES,
            transactionContext,
            deadlineMessageKey,
            DbNil.INSTANCE
        )
    }

    fun acceptWhileTrue(visitor: Visitor) {
        deadlineColumnFamily.whileTrue { key, _ ->
            visitor.visit(MessageDeadlinesEntry(key.first.value, key.second.value))
        }
    }

    data class MessageDeadlinesEntry(val deadline: Long, val messageKey: Long)

    fun interface Visitor {
        fun visit(entry: MessageDeadlinesEntry): Boolean
    }

}