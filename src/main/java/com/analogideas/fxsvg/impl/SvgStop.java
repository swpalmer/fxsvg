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

import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;

/**
 *
 * @author scott
 */
public class SvgStop implements SvgObjWithId {
    
    String id;
    double offset;
    double opacity = 1.0;
    String color= "#000";

    public void setOffset(double offset) {
        this.offset = offset;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setOpacity(double opacity) {
        this.opacity = opacity;
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
        if (opacity != 1.0) {
            javafx.scene.paint.Color c = Color.valueOf(color);
            c = c.deriveColor(0, 1.0, 1.0, opacity);
            return new Stop(offset, c);
        }
        return new Stop(offset, Color.valueOf(color));
    }

    @Override
    public String toString() {
        return "<stop offset=\""+offset+"\" stop-opacity=\""+opacity+"\" stop-color=\""+color+"\" />";
    }
}
