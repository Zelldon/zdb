package io.zell.zdb.log

import io.camunda.zeebe.journal.JournalMetaStore

object NoopMetaStore : JournalMetaStore {
    override fun storeLastFlushedIndex(index: Long) {}
    override fun loadLastFlushedIndex(): Long {
        return 0
    }
}
