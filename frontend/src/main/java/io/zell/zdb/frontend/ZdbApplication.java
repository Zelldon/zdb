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

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ZdbApplication extends Application {
  @Override
  public void start(final Stage stage) throws IOException {
    final FXMLLoader fxmlLoader = new FXMLLoader(ZdbApplication.class.getResource("zdb-view.fxml"));
    final Scene scene = new Scene(fxmlLoader.load(), 320, 240);
    stage.setTitle("Zeebe debug and inspection tool");
    stage.setScene(scene);
    stage.show();
  }

  public static void main(final String[] args) {
    launch();
  }
}
