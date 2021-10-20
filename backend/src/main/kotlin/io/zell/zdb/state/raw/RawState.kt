package io.zell.zdb.state.raw

import io.zell.zdb.db.readonly.transaction.ReadonlyTransactionDb
import java.nio.file.Path

class RawState(private val readonlyTransactionDb: ReadonlyTransactionDb) {

    constructor(statePath: Path) : this(ReadonlyTransactionDb.openReadonlyDb(statePath))

    fun exportElementInstanceKeyColumnFamily() {
        val elementInstanceKeyColumnFamily = ElementInstanceKeyColumnFamily(
            readonlyTransactionDb,
            readonlyTransactionDb.createContext()
        )

        elementInstanceKeyColumnFamily.acceptWhileTrue(printIt())
    }

    fun checkConsistencyElementInstanceKeyColumnFamily() {
        println("-----")
        println("Checking consistency of ZbColumnFamilies.ELEMENT_INSTANCE_KEY")
        println("-----")
        val elementInstanceKeyColumnFamily = ElementInstanceKeyColumnFamily(
            readonlyTransactionDb,
            readonlyTransactionDb.createContext()
        )

        elementInstanceKeyColumnFamily.findOrphans().forEach(System.out::println)
    }

    fun exportElementInstanceParentChildColumnFamily() {
        val elementInstanceParentChildColumnFamily = ElementInstanceParentChildColumnFamily(
            readonlyTransactionDb,
            readonlyTransactionDb.createContext()
        )

        elementInstanceParentChildColumnFamily.acceptWhileTrue(printIt())
    }

    fun exportMessageKeyColumnFamily() {
        val messageKeyColumnFamily =
            MessageKeyColumnFamily(
                readonlyTransactionDb,
                readonlyTransactionDb.createContext()
            )

        messageKeyColumnFamily.acceptWhileTrue(printIt())
    }

    fun exportMessageDeadlineColumnFamily() {
        val messageDeadlinesColumnFamily =
            MessageDeadlinesColumnFamily(
                readonlyTransactionDb,
                readonlyTransactionDb.createContext()
            )

        messageDeadlinesColumnFamily.acceptWhileTrue(printIt())
    }

    private fun printIt(): (entry: Any) -> Boolean =
        {
            println(it)
            true
        }

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

}