package io.zell.zdb.log

import io.zell.zdb.journal.ReadOnlyJournalMetaStore


object NoopMetaStoreReadOnly : ReadOnlyJournalMetaStore {
    override fun loadLastFlushedIndex(): Long {
        return Long.MAX_VALUE;
    }

    override fun hasLastFlushedIndex(): Boolean {
        return true;
    }
}
