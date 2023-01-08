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

import javafx.scene.Group;

/**
 *
 * @author scott
 */
public class SvgClip implements SvgContainer, SvgObjWithId {
    
    Group clip;

    public SvgClip(Group clip) {
        this.clip = clip;
    }

    public Group clip() {
        return clip;
    }

    @Override
    public void id(String id) {
        clip().setId(id);
    }

    @Override
    public String id() {
        return clip.getId();
    }

    @Override
    public Object obj() {
        return clip();
    }

    @Override
    public void add(SvgData svgObj) {
        if (svgObj instanceof SvgNode n) {
            clip().getChildren().add(n.node());
        } else if (svgObj instanceof SvgGroup g) {
            clip().getChildren().add(g.group());
        } else {
            throw new RuntimeException("Can't add a " + svgObj.getClass().getName() + " to a clipPath.");
        }
    }
    
}
