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
package com.analogideas.fxsvg.impl;

import com.analogideas.fxsvg.SVGReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.transform.Transform;

/**
 *
 * @author scott
 */
public class SvgLinearGradient implements SvgContainer, SvgObjWithId {
    private Map<String, Object> defs;
    List<Stop> stops = new ArrayList<>();
    String id;
    double x1;
    double y1;
    double x2;
    double y2;
    CycleMethod cycle = CycleMethod.NO_CYCLE;
    String transform;
    String href;
    boolean proportional = true;

    public SvgLinearGradient(Map<String, Object> defs) {
        this.defs = defs; // to resolve hrefs
    }
    
    
    @Override
    public void add(SvgData svgObj) {
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
            // deal with gradient transform
            Transform t = SVGReader.transformsFromString(transform);
            var p1 = t.transform(x1, y1);
            x1 = p1.getX();
            y1 = p1.getY();
            var p2 = t.transform(x2, y2);
            x2 = p2.getX();
            y2 = p2.getY();
        }
        if (stops.isEmpty() && href != null) {
            Object other = defs.get(href);
            if (other instanceof LinearGradient otherGrad) {
                stops.addAll(otherGrad.getStops());
            }
        }
        LinearGradient gradient = new LinearGradient(x1, y1, x2, y2, proportional, cycle, stops);
        return gradient;
    }

    public void setX1(double x1) {
        this.x1 = x1;
    }

    public void setY1(double y1) {
        this.y1 = y1;
    }

    public void setX2(double x2) {
        this.x2 = x2;
    }

    public void setY2(double y2) {
        this.y2 = y2;
    }
    
    public void setCycleMethod(CycleMethod cycle) {
        this.cycle = cycle;
    }
    
    public void setTransform(String transform) {
        this.transform = transform;
    }
    
    public void setProportional(boolean prop) {
        this.proportional = prop;
    }
    
    // currently only used to inherit stops
    public void setHref(String href) {
        this.href = href;
    }
}
