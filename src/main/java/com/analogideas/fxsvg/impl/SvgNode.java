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

import javafx.scene.Node;

/**
 *
 * @author scott
 */
public class SvgNode implements SvgObjWithId {
    
    Node node;

    public SvgNode(Node node) {
        this.node = node;
    }

    public Node node() {
        return node;
    }

    @Override
    public void id(String id) {
        node.setId(id);
    }

    @Override
    public String id() {
        return node().getId();
    }

    @Override
    public Object obj() {
        return node();
    }
    
}
