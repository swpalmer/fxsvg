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

import javafx.scene.Group;

/**
 *
 * @author scott
 */
class SvgGroup implements SvgContainer, SvgObjWithId {
    
    Group group = new Group();

    public Group group() {
        return group;
    }

    @Override
    public void add(SvgData svgObj) {
        if (svgObj instanceof SvgNode n) {
            group().getChildren().add(n.node());
        } else if (svgObj instanceof SvgGroup g) {
            group().getChildren().add(g.group());
        } else {
            throw new RuntimeException("Can't add a " + svgObj.getClass().getName() + " to a Group.");
        }
    }

    @Override
    public void id(String id) {
        group().setId(id);
    }

    @Override
    public String id() {
        return group().getId();
    }

    @Override
    public Object obj() {
        return group();
    }
    
}
