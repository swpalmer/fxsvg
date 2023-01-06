/*
 * Copyright 2023 scott.
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

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/**
 *
 * @author scott
 */
class SvgLinearGradient implements SvgContainer, SvgObjWithId {
    
    List<Stop> stops = new ArrayList<>();
    String id;
    double x1;
    double y1;
    double x2;
    double y2;
    CycleMethod cycle = CycleMethod.NO_CYCLE;
    String transform;
    
    @Override
    public void add(SvgData svgObj) {
        //System.out.println("Adding to LinearGradient: " + svgObj);
        if (svgObj instanceof SvgStop svgStop) {
            stops.add((Stop) svgStop.obj());
        }
    }

    @Override
    public void id(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Object obj() {
        if (transform != null) {
            // TODO transform x1,y1, x2,y2
        }
        LinearGradient gradient = new LinearGradient(x1, y1, x2, y2, true, cycle, stops);
        return gradient;
    }

    void setX1(double x1) {
        this.x1 = x1;
    }

    void setY1(double y1) {
        this.y1 = y1;
    }

    void setX2(double x2) {
        this.x2 = x2;
    }

    void setY2(double y2) {
        this.y2 = y2;
    }
    
    void setCycleMethod(CycleMethod cycle) {
        this.cycle = cycle;
    }
    
    void setTransform(String transform) {
        this.transform = transform;
    }
}
