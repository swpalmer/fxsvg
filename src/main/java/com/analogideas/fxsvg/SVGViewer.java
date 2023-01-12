/*
 * Copyright 2023 Scott W. Palmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.analogideas.fxsvg;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 *
 * @author scott
 */
public class SVGViewer extends Application {
    
    String [] TEST_PATHS = {
        "./src/test/resources/1669708251wallet-and-credit-cards.svg",
        "./src/test/resources/Arms_of_New_Brunswick.svg",
        "./src/test/resources/broken-heart-svgrepo-com.svg",
        "./src/test/resources/carbon.svg",
        "./src/test/resources/check-mark-svgrepo-com.svg",
        "./src/test/resources/folded-hands-skin-2-svgrepo-com.svg",
        "./src/test/resources/glasses-svgrepo-com.svg",
        "./src/test/resources/mercurial-logo-icon.svg",
        "./src/test/resources/myAvatar.svg",
        "./src/test/resources/parcel.svg",
        "./src/test/resources/redhurricane-lamp.svg",
        "./src/test/resources/waving-hand-skin-4-svgrepo-com.svg",
        "./src/test/resources/woozy-face-svgrepo-com.svg",
        "./src/test/resources/writing-hand-skin-3-svgrepo-com.svg",
        "./src/test/resources/writing-hand-skin-4-svgrepo-com.svg",
        "./src/test/resources/zany-face-svgrepo-com.svg",
    };
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        var params = getParameters();
        List<File> svgFiles;
        List<String> filePaths = params.getUnnamed();
        if (filePaths.isEmpty()) {
            if (TEST_PATHS.length > 0) {
                // uncomment or add to the test paths to quickly see test cases
                svgFiles = Arrays.stream(TEST_PATHS).map(File::new).toList();
            } else {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Scalable Vector Graphics", "*.svg", "*.SVG"));
                svgFiles = fc.showOpenMultipleDialog(primaryStage);
            }
        } else {
            svgFiles = filePaths.stream().map(File::new).toList();
        }
        Slider slider = new Slider(-50.0, 150, 0.0);
        slider.setMaxWidth(Double.MAX_VALUE);
        
        NumberBinding scaleProp = Bindings.when(Bindings.lessThan(slider.valueProperty(), 0))
                .then(Bindings.divide(Bindings.add(50.0,slider.valueProperty()),50.0))
                .otherwise(
                    Bindings.when(Bindings.greaterThan(slider.valueProperty(), 0))
                        .then(Bindings.add(1.0, Bindings.multiply(slider.valueProperty(),0.1)))
                        .otherwise(1.0)
                );
        
        HBox.setHgrow(slider, Priority.ALWAYS);
        for (File svgFile : svgFiles) {
            System.out.println(svgFile);
            SVGReader svgReader = new SVGReader(svgFile);
            Group svgImg = svgReader.buildNode();
            svgImg.scaleXProperty().bind(scaleProp);
            svgImg.scaleYProperty().bind(scaleProp);
            Group wrap = new Group(svgImg);
            box.getChildren().add(wrap);
        }
        HBox ctrls = new HBox(new Label("Scale:"), slider);
        ScrollPane sp = new ScrollPane(box);
        var root = new VBox(ctrls, sp);
        var scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("SVG Viewer");
        primaryStage.show();
    }
    
}
