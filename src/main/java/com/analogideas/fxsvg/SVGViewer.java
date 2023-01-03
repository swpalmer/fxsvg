/*
 * Copyright (C) 2023 Scott W. Palmer
 * All rights reserved.
 */

package com.analogideas.fxsvg;

import java.io.File;
import java.util.List;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 *
 * @author scott
 */
public class SVGViewer extends Application {
    
    String [] TEST_PATHS = {
        "/Users/scott/dev/Personal/Grade/src/main/resources/com/analogideas/grade/ui/1F4E6.svg",
        "/Users/scott/dev/Personal/Grade/src/main/resources/com/analogideas/grade/ui/1F3D7.svg",
        "/Users/scott/dev/Personal/Grade/src/main/resources/com/analogideas/grade/ui/1F511.svg",
        "/Users/scott/dev/Personal/Grade/src/main/resources/com/analogideas/grade/ui/1F512.svg",
        "/Users/scott/dev/Personal/Grade/src/main/resources/com/analogideas/grade/ui/1F527.svg",
        "/Users/scott/dev/Personal/Grade/src/main/resources/com/analogideas/grade/ui/1F529.svg",
        "/Users/scott/dev/Personal/Grade/src/main/resources/com/analogideas/grade/ui/1F6D1.svg",
        "/Users/scott/dev/Personal/Grade/src/main/resources/com/analogideas/grade/ui/1F9F0.svg",
        "/Users/scott/dev/Personal/Grade/src/main/resources/com/analogideas/grade/ui/1FAB2.svg",
        "/Users/scott/dev/Personal/Grade/src/main/resources/com/analogideas/grade/ui/23F1.svg",
        "/Users/scott/dev/Personal/Grade/src/main/resources/com/analogideas/grade/ui/2699.svg",
        "/Users/scott/dev/Personal/Grade/src/main/resources/com/analogideas/grade/ui/26A1.svg"
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
//            filePaths = new ArrayList<>(TEST_PATHS.length);
//            Collections.addAll(filePaths, TEST_PATHS);
            FileChooser fc = new FileChooser();
            svgFiles = fc.showOpenMultipleDialog(primaryStage);
        } else {
            svgFiles = filePaths.stream().map(File::new).toList();
        }
        Slider slider = new Slider(0.01, 10, 1);
        slider.setMaxWidth(Double.MAX_VALUE);
        for (File svgFile : svgFiles) {
            SVGReader svgReader = new SVGReader(svgFile);
            Group svgImg = svgReader.buildNode();
            svgImg.scaleXProperty().bind(slider.valueProperty());
            svgImg.scaleYProperty().bind(slider.valueProperty());
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
