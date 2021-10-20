package io.zell.zdb.state.raw

import io.camunda.zeebe.db.ColumnFamily
import io.camunda.zeebe.db.TransactionContext
import io.camunda.zeebe.db.ZeebeDb
import io.camunda.zeebe.db.impl.DbLong
import io.camunda.zeebe.engine.state.ZbColumnFamilies
import io.camunda.zeebe.engine.state.message.StoredMessage

class MessageKeyColumnFamily(
    zeebeDb: ZeebeDb<ZbColumnFamilies>,
    transactionContext: TransactionContext
) {

    private val messageColumnFamily: ColumnFamily<DbLong, StoredMessage>

    private val messageKey: DbLong = DbLong()
    private val message: StoredMessage = StoredMessage()

    init {
        messageColumnFamily = zeebeDb.createColumnFamily<DbLong, StoredMessage>(
            ZbColumnFamilies.MESSAGE_KEY, transactionContext, messageKey, message
        )
    }

    fun acceptWhileTrue(visitor: Visitor) {
        messageColumnFamily.whileTrue { key, value ->
            visitor.visit(MessageKeyEntry(key.value, copyMessage(value)))
        }
    }

    private fun copyMessage(value: StoredMessage): StoredMessage {
        val storedMessageCopy = StoredMessage()
        storedMessageCopy.messageKey = value.messageKey
        storedMessageCopy.message = value.message
        return storedMessageCopy
    }

    fun get(key: Long): MessageKeyEntry? {
        messageKey.wrapLong(key)

        val message = messageColumnFamily.get(messageKey)

        return if (message == null) {
            null
        } else {
            MessageKeyEntry(key, copyMessage(message))
        }
    }

    data class MessageKeyEntry(val messageKey: Long, val message: StoredMessage)

    fun interface Visitor {
        fun visit(entry: MessageKeyEntry): Boolean
    }

}