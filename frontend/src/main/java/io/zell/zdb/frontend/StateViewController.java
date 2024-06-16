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

import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.zell.zdb.state.KeyFormatters;
import io.zell.zdb.state.ZeebeDbReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.stage.DirectoryChooser;

public class StateViewController implements Initializable {

  @FXML private TableView<StateDataKV> zeebeData;

  @FXML private Button findDataPath;

  @FXML private TextField dataPath;

  @FXML private ChoiceBox<String> columnFamily;

  private final DirectoryChooser directoryChooser = new DirectoryChooser();
  private ObservableList<StateDataKV> dataObservableList;

  @Override
  public void initialize(final URL url, final ResourceBundle resourceBundle) {
    // time to initialize fields
    final var userHome = System.getProperty("user.home");
    this.dataPath.setText(userHome);
    this.dataPath.setEditable(false);

    this.columnFamily.setItems(
        FXCollections.observableList(
            Arrays.stream(ZbColumnFamilies.values()).map(Enum::name).toList()));
    this.columnFamily.setValue(ZbColumnFamilies.DEFAULT.name());

    // put into the data table view
    this.dataObservableList = FXCollections.observableList(new ArrayList<>());
    this.zeebeData.setItems(this.dataObservableList);
    // define columns
    this.zeebeData.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    this.zeebeData
        .getColumns()
        .setAll(
            StateViewController.<String>createTableColumn("Key", "key"),
            StateViewController.<String>createTableColumn("Value", "value"));

    // enable multi-selection
    this.zeebeData.getSelectionModel().setCellSelectionEnabled(true);
    this.zeebeData.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    this.zeebeData.setOnKeyPressed(new TableViewKeyEventClipboardCopier<>(this.zeebeData));
  }

  private static <T> TableColumn<StateDataKV, T> createTableColumn(
      final String displayName, final String propertyName) {
    final TableColumn<StateDataKV, T> column = new TableColumn<StateDataKV, T>(displayName);
    column.setCellValueFactory(new PropertyValueFactory<StateDataKV, T>(propertyName));
    return column;
  }

  public class StateDataKV {
    private final StringProperty key;
    private final StringProperty value;

    public StateDataKV(final String key, final String value) {
      this.key = new SimpleStringProperty(key);
      this.value = new SimpleStringProperty(value);
    }

    public String getKey() {
      return this.key.get();
    }

    public StringProperty keyProperty() {
      return this.key;
    }

    public void setKey(final String key) {
      this.key.set(key);
    }

    public String getValue() {
      return this.value.get();
    }

    public StringProperty valueProperty() {
      return this.value;
    }

    public void setValue(final String value) {
      this.value.set(value);
    }
  }

  @FXML
  protected void onFindFile() {
    this.directoryChooser.setTitle("Zeebe data path");
    this.directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

    final File file = this.directoryChooser.showDialog(this.findDataPath.getScene().getWindow());

    if (file != null) {
      this.dataPath.setText(file.getAbsolutePath());
      fillTableViewWithStateData(this.dataPath.getText(), this.columnFamily.getValue());
    }
  }

  private void fillTableViewWithStateData(final String path, final String columnFamilyName) {
    // read data based on column family
    final var zeebeDbReader = new ZeebeDbReader(new File(path).toPath());
    final var columnFamilyValue = ZbColumnFamilies.valueOf(columnFamilyName);

    final var keyFormatter = KeyFormatters.ofDefault().forColumnFamily(columnFamilyValue);

    // remove old content
    this.dataObservableList.clear();
    this.zeebeData.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    // update table view
    zeebeDbReader.visitDBWithPrefix(
        columnFamilyValue,
        (key, value) ->
            this.dataObservableList.add(new StateDataKV(keyFormatter.formatKey(key), value)));
  }

  public void selectColumnFamily() {
    // do something on selecting CF
    try {
      final var expectedPath = this.dataPath.getText();

      fillTableViewWithStateData(expectedPath, this.columnFamily.getValue());
    } catch (final Exception exception) {
      if (exception instanceof FileNotFoundException) {
        // no state path
        System.out.println(
            String.format(
                "Current path %s doesn't point to a state path.", this.dataPath.getText()));
        return;
      }
      exception.printStackTrace();
    }
  }
}
