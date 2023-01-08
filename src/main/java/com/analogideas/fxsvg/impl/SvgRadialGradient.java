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
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.transform.Transform;

/**
 *
 * @author scott
 */
public class SvgRadialGradient implements SvgContainer, SvgObjWithId {
    Map<String, Object> defs;
    List<Stop> stops = new ArrayList<>();
    String id;
    double fx;
    double fy;
    double cx;
    double cy;
    double r;
    boolean proportional = true;
    CycleMethod cycle = CycleMethod.NO_CYCLE;
    String transform;
    String href;

    public SvgRadialGradient(Map<String, Object> defs) {
        this.defs = defs;
    }
    

    @Override
    public void id(String id) {
        this.id = id;
    }

    @Override
    public void add(SvgData svgObj) {
        if (svgObj instanceof SvgStop svgStop) {
            stops.add((Stop) svgStop.obj());
        }
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Object obj() {
        if (stops.isEmpty() && href != null) {
            Object other = defs.get(href);
            if (other instanceof RadialGradient otherGrad) {
                stops.addAll(otherGrad.getStops());
            }
        }

        if (transform != null) {
            // deal with gradient transform
            Transform t = SVGReader.transformsFromString(transform);
            var cp = t.transform(cx, cy);
            cx = cp.getX(); 
            cy = cp.getY();
            var fp = t.transform(fx, fy);
            fx = fp.getX();
            fy = fp.getY();
            var po = t.transform(0, 0); // transform origin so we can calc new 'r'
            var pr = t.transform(r, 0); // since 'r' should only be scaled
            var drx = pr.getX()-po.getX();
            var dry = pr.getY()-po.getY();
            r = Math.sqrt(drx*drx+dry*dry);
            //System.out.println("After transform: cx="+cx+", cy="+cy+", fx="+fx+", fy="+fy+", r="+r);
        }
        // convert fx,fy to polar coords.
        double dx = fx-cx;
        double dy = fy-cy;
        double fDist = Math.sqrt(dx * dx + dy * dy);
        double theta = Math.atan2(dy, dx);
        double fAngle = Math.toDegrees(theta);
        //fDist is relative to radius for JavaFX RadialGradient
        fDist = fDist/r;
        RadialGradient gradient = new RadialGradient(fAngle, fDist, cx, cy, r, proportional, cycle, stops);
        return gradient;
    }

    public void setFx(double fx) {
        this.fx = fx;
    }

    public void setFy(double fy) {
        this.fy = fy;
    }

    public void setCx(double cx) {
        this.cx = cx;
    }

    public void setCy(double cy) {
        this.cy = cy;
    }

    public void setR(double r) {
        this.r = r;
    }
    
    public void setCycleMethod(CycleMethod cycle) {
        this.cycle = cycle;
    }
    
    public void setProportional(boolean prop) {
        this.proportional = prop;
    }
    
    public void setTransform(String transform) {
        this.transform = transform;
    }

    // currently only used to inherit stops
    public void setHref(String href) {
        this.href = href;
    }

}
