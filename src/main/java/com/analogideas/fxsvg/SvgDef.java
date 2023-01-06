package com.analogideas.fxsvg;

import java.util.Map;


/**
 *
 * @author scott
 */
record SvgDef(Map<String,Object> map) implements SvgContainer {

    @Override
    public void add(SvgData svgObj) {
        if (svgObj instanceof SvgObjWithId oid) {
            map().put(oid.id(), oid.obj());
        } else {
            throw new RuntimeException("Can't add a " + svgObj.getClass().getName() + " it has no id.");
        }
    }
}
