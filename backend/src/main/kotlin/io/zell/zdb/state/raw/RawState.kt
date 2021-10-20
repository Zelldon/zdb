package io.zell.zdb.state.raw

import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import java.nio.file.Path

class RawState(private val readonlyTransactionDb: ReadonlyTransactionDb) {

    constructor(statePath: Path) : this(ReadonlyTransactionDb.openReadonlyDb(statePath))

    fun checkConsistencyMessageDeadlineColumnFamily() {
        println("-----")
        println("Checking consistency of ZbColumnFamilies.MESSAGE_DEADLINES")
        println("-----")
        val messageDeadlinesColumnFamily =
            MessageDeadlinesColumnFamily(
                readonlyTransactionDb,
                readonlyTransactionDb.createContext()
            )

        val messageKeyColumnFamily =
            MessageKeyColumnFamily(
                readonlyTransactionDb,
                readonlyTransactionDb.createContext()
            )

        messageDeadlinesColumnFamily.acceptWhileTrue {
            val messageKeyEntry = messageKeyColumnFamily.get(it.messageKey)

            if (messageKeyEntry == null) {
                println("$it references messageKey: ${it.messageKey} which no longer exists")
            }
            true
        }
    }

    fun exportMessageKeyColumnFamily() {
        val messageKeyColumnFamily =
            MessageKeyColumnFamily(
                readonlyTransactionDb,
                readonlyTransactionDb.createContext()
            )

        messageKeyColumnFamily.acceptWhileTrue {
            println(it)
            true
        }
    }

    fun exportMessageDeadlineColumnFamily() {
        val messageDeadlinesColumnFamily =
            MessageDeadlinesColumnFamily(
                readonlyTransactionDb,
                readonlyTransactionDb.createContext()
            )

        messageDeadlinesColumnFamily.acceptWhileTrue {
            println(it)
            true
        }
    }

}