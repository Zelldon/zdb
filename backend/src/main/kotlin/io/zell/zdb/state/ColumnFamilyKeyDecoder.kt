package io.zell.zdb.state

import io.camunda.zeebe.db.impl.DbLong
import io.camunda.zeebe.db.impl.DbString
import io.camunda.zeebe.db.impl.ZeebeDbConstants
import io.camunda.zeebe.protocol.ZbColumnFamilies
import org.agrona.concurrent.UnsafeBuffer
import java.util.*

typealias Decoder = (ByteArray) -> String
class ColumnFamilyKeyDecoder {


    var cfDecoders: EnumMap<ZbColumnFamilies, Decoder> = EnumMap(ZbColumnFamilies::class.java)

        init {
            cfDecoders[ZbColumnFamilies.DEFAULT] = ::dbStringToStringDecoder
            cfDecoders[ZbColumnFamilies.KEY] = ::dbStringToStringDecoder
        }

    fun decodeColumnFamilyKey(cf: ZbColumnFamilies, key: ByteArray) : String {
        val function = cfDecoders[cf]
        return function?.let {
            function(key)
        } ?: HexFormat.ofDelimiter(" ").formatHex(key)
    }

    private fun dbLongToStringDecoder(bytes : ByteArray) : String {
        val dbLong = DbLong()
        dbLong.wrap(UnsafeBuffer(bytes), Long.SIZE_BYTES, bytes.size)
        return dbLong.value.toString()
    }

    private fun dbStringToStringDecoder(bytes : ByteArray) : String {
        val dbString = DbString()
        dbString.wrap(UnsafeBuffer(bytes), Long.SIZE_BYTES, bytes.size)
        return dbString.toString()
    }
}
