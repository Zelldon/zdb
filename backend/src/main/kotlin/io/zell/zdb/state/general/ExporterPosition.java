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
package io.zell.zdb.state.general;

import io.camunda.zeebe.db.DbValue;
import io.camunda.zeebe.msgpack.UnpackedObject;
import io.camunda.zeebe.msgpack.property.LongProperty;

/**
 * *NOTE: COPIED FROM ZEEBE BROKER*
 */
public class ExporterPosition extends UnpackedObject implements DbValue {
  private final LongProperty positionProp = new LongProperty("exporterPosition");

  public ExporterPosition() {
    declareProperty(positionProp);
  }

  public void set(final long position) {
    positionProp.setValue(position);
  }

  public long get() {
    return positionProp.getValue();
  }
}
