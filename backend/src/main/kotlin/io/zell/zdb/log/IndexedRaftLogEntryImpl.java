/*
 * Copyright 2017-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zell.zdb.log;

import io.atomix.raft.protocol.PersistedRaftRecord;
import io.atomix.raft.storage.log.IndexedRaftLogEntry;
import io.atomix.raft.storage.log.entry.ApplicationEntry;
import io.atomix.raft.storage.log.entry.RaftEntry;
import io.zell.zdb.journal.ReadOnlyJournalRecord;

/** Indexed journal entry. */
record IndexedRaftLogEntryImpl(long index, long term, RaftEntry entry, ReadOnlyJournalRecord record)
        implements IndexedRaftLogEntry {

    IndexedRaftLogEntryImpl(final long term, final RaftEntry entry, final ReadOnlyJournalRecord record) {
        this(record.index(), term, entry, record);
    }

    @Override
    public boolean isApplicationEntry() {
        return entry instanceof ApplicationEntry;
    }

    @Override
    public ApplicationEntry getApplicationEntry() {
        return (ApplicationEntry) entry;
    }

    @Override
    public PersistedRaftRecord getPersistedRaftRecord() {
        final byte[] serializedRaftLogEntry = new byte[record.data().capacity()];
        record.data().getBytes(0, serializedRaftLogEntry);
        return new PersistedRaftRecord(
                term, index, record.asqn(), record.checksum(), serializedRaftLogEntry);
    }
}