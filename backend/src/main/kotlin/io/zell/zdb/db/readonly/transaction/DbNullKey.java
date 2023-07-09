/*
 * Copyright © 2021 Christopher Kujawa (zelldon91@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zell.zdb.db.readonly.transaction;

import io.camunda.zeebe.db.DbKey;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/** This class is used only internally by #isEmpty to search for same column family prefix. */
final class DbNullKey implements DbKey {

  public static final DbNullKey INSTANCE = new DbNullKey();

  DbNullKey() {}

  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    // do nothing
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    // do nothing
  }

  @Override
  public int getLength() {
    return 0;
  }
}
