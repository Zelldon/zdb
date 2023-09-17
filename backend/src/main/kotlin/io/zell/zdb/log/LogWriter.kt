package io.zell.zdb.log

import java.io.OutputStream
import java.io.PrintWriter

class LogWriter(val out: OutputStream, val reader: LogContentReader) {

    fun writeAsTable() {
        val printWriter = PrintWriter(out, true)
        val columnTitle = "Index Term Position SourceRecordPosition Timestamp Key RecordType ValueType Intent ProcessInstanceKey BPMNElementType "
        printWriter.println(columnTitle)
        var separator = ""
        while (reader.hasNext()) {
            val record: PersistedRecord = reader.next()
            printWriter.print(separator + record.asColumnString())
            separator = ""
        }
        printWriter.flush();
    }

}