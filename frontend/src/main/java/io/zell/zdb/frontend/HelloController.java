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

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

public class HelloController implements Initializable {

  @FXML private TableView<ZeebeRecord> zeebeData;

  @FXML private Button findDataPath;

  @FXML private TextField dataPath;
  private final DirectoryChooser directoryChooser = new DirectoryChooser();
  private ObservableList<ZeebeRecord> dataObservableList;

  @Override
  public void initialize(final URL url, final ResourceBundle resourceBundle) {
    //        // time to initialize fields
    //        final var userHome = System.getProperty("user.home");
    //        dataPath.setText(userHome);
    //
    //        dataObservableList = FXCollections.observableList(new ArrayList<>());
    //        // put into the data table view
    //        zeebeData.setItems(dataObservableList);
    //
    //        // separate via column
    //        TableColumn<ZeebeRecord,Long> positionCol = new TableColumn<>("Position");
    //        positionCol.setCellValueFactory(new PropertyValueFactory<>("position"));
    //
    //        TableColumn<ZeebeRecord,Long> sourceRecordPostionCol = new TableColumn<>("Source
    // Record Position");
    //        sourceRecordPostionCol.setCellValueFactory(new
    // PropertyValueFactory<>("sourceRecordPostion"));
    //
    //        TableColumn<ZeebeRecord,Long> timestampCol = new TableColumn<>("Timestamp");
    //        timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
    //
    //        TableColumn<ZeebeRecord,Long> keyCol = new TableColumn<>("Key");
    //        keyCol.setCellValueFactory(new PropertyValueFactory<>("key"));
    //
    //
    //        TableColumn<ZeebeRecord,String> recordTypeCol = new TableColumn<>("Record Type");
    //        recordTypeCol.setCellValueFactory(new PropertyValueFactory<>("recordType"));
    //
    //        TableColumn<ZeebeRecord,String> valueTypeCol = new TableColumn<>("Value Type");
    //        valueTypeCol.setCellValueFactory(new PropertyValueFactory<>("valueType"));
    //
    //        TableColumn<ZeebeRecord,String> intentCol = new TableColumn<>("Intent");
    //        intentCol.setCellValueFactory(new PropertyValueFactory<>("intent"));
    //
    //        // define columns
    //        zeebeData.getColumns().setAll(positionCol, sourceRecordPostionCol, timestampCol,
    // keyCol, recordTypeCol, valueTypeCol, intentCol);
  }

  @FXML
  protected void onFindFile() {}
}
