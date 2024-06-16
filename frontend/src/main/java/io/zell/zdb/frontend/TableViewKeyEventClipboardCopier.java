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
package io.zell.zdb.frontend;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.*;

public class TableViewKeyEventClipboardCopier<T extends KeyEvent> implements EventHandler<T> {

  public static final KeyCodeCombination KEY_CODE_COMBINATION_COPY =
      new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);
  private final TableView tableView;

  public TableViewKeyEventClipboardCopier(final TableView tableView) {
    this.tableView = tableView;
  }

  @Override
  public void handle(final T keyEvent) {
    if (KEY_CODE_COMBINATION_COPY.match(keyEvent)) {
      if (keyEvent.getSource() instanceof TableView) {
        final var clipboard = Clipboard.getSystemClipboard();
        final var content = new ClipboardContent();

        final ObservableList<TablePosition> selectedCells =
            this.tableView.getSelectionModel().getSelectedCells();

        for (final var position : selectedCells) {
          final var value = position.getTableColumn().getCellObservableValue(position.getRow());
          final String valueStr = value.getValue().toString();
          System.out.println(valueStr);
          content.putString(valueStr);
        }
        clipboard.setContent(content);
        System.out.println("Selection copied to clipboard");

        // event is handled, consume it
        keyEvent.consume();
      }
    }
  }
}
