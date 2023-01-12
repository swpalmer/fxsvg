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

import com.analogideas.fxsvg.impl.SvgClip;
import com.analogideas.fxsvg.impl.SvgContainer;
import com.analogideas.fxsvg.impl.SvgData;
import com.analogideas.fxsvg.impl.SvgDef;
import com.analogideas.fxsvg.impl.SvgGroup;
import com.analogideas.fxsvg.impl.SvgLinearGradient;
import com.analogideas.fxsvg.impl.SvgNode;
import com.analogideas.fxsvg.impl.SvgRadialGradient;
import com.analogideas.fxsvg.impl.SvgStop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.stage.Screen;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;


/** A simple SVG reader for JavaFX.
 *
 * @author scott
 */
public class SVGReader {
    private static final Logger LOGGER = Logger.getLogger(SVGReader.class.getName());
    private static final Level logLevel = Level.INFO;
    private final InputStream svgData;
    private String rootId;
    private final ArrayDeque<SvgData> svgObjStack = new ArrayDeque<>();

    private final Map<String, Object> defs = new HashMap<>();
    
    static interface DeferredValue {
        void apply();
    }

    record DeferredFill(Shape shape, String fill, Map<String, Object> defs) implements DeferredValue {
        @Override
        public void apply() {
            shape.setFill(paintFromAttr(defs,fill));
        }
    }

    record DeferredStroke(Shape shape, String stroke, Map<String, Object> defs) implements DeferredValue {
        @Override
        public void apply() {
            shape.setStroke(paintFromAttr(defs, stroke));
        }
    }

    private final List<DeferredValue> deferredValues = new ArrayList<>();
    
    /**
     * Constructs a SVGReader that will build a Node from the given SVG file.
     * @param svgFile the SVG file to read
     * @throws java.io.FileNotFoundException
     */
    public SVGReader(File svgFile) throws FileNotFoundException {
        this.svgData = new FileInputStream(Objects.requireNonNull(svgFile));
        this.rootId = svgFile.getName().replace('.', '-').replace('#', '_');
    }
    
    /**
     * Constructs a SVGReader that will build a Node from the given SVG file.
     * @param svgStream the SVG data to read
     * @param rootId ID to use for the root Node. 
     * (ids within the SVG data will be prefixed with this String)
     */
    public SVGReader(InputStream svgStream, String rootId) {
        this.rootId = rootId;
        this.svgData = Objects.requireNonNull(svgStream);
    }
    
    /**
     * Sets the id for the root Node of the image.  Any id attributes specified
     * in the SVG will be prefixed with this id+'-' to ensure they are unique
     * in the scene graph.
     * The default value for the root node id is derived from the filename.
     * @param id the id for the root node of the SVG image.
     */
    public void setId(String id) {
        rootId = id;
    }
    
    /**
     * Creates a Node that will render as the SVG image using a hierarchy of
     * JavaFX Shapes.
     * @return a Group Node representing the SVG image 
     * @throws javax.xml.stream.XMLStreamException 
     */
    public Group buildNode() throws XMLStreamException  {
        XMLStreamReader svgStream = javax.xml.stream.XMLInputFactory.newDefaultFactory().createXMLStreamReader(svgData, "utf-8");
        svgObjStack.clear();
        defs.clear();
        deferredValues.clear();
        
        SvgGroup svgNode = new SvgGroup();
        svgNode.id(rootId != null ? rootId : "");
        svgObjStack.push(svgNode);

        while (svgStream.hasNext()) {
            int parseEvent = svgStream.next();
            switch (parseEvent) {
                case XMLEvent.START_ELEMENT: {
                    String name = svgStream.getLocalName();
                    process(name, svgStream);
                    break;
                }
                case XMLEvent.END_ELEMENT: {
                    String name = svgStream.getLocalName();
                    if (producesObject(name)) {
                        SvgData obj = svgObjStack.pop();
                        if (obj instanceof SvgDef svgdef) {
                            // drop it, the defs are already added to the map
                            LOGGER.log(logLevel, String.valueOf(svgdef));
                        } else if (svgObjStack.peek() instanceof SvgContainer c) {
                            //LOGGER.log(logLevel, () -> "Adding "+obj+" to "+c);
                            c.add(obj);
                        }
                    }
                    break;
                }
                case XMLEvent.ATTRIBUTE:
                    String attr = svgStream.getLocalName();
                    String text = svgStream.getText();
                    LOGGER.log(logLevel, () -> "Attribute Event for: "+attr+", value = "+text);
                    break;    
                default:
                    break;
            }
        }
        deferredValues.forEach(DeferredValue::apply);
        return svgNode.group();
    }
    
    private boolean producesObject(String elementName) {
        return switch (elementName) {
            case "g", "path", "polygon", "line", "circle", "ellipse", "rect",
                "polyline", "text", "defs", "clipPath", "linearGradient",
                "radialGradient", "stop" -> true;
            default -> false;
        };
    }
    
    // process an element
    private void process( String name, XMLStreamReader svgStream) {
        switch (name) {
            case "svg": // beginning a SVG document
                // TODO: process width, height, viewBox
                break;
            case "g":
                processGroup(svgStream);
                break;
            case "path":
                processPath(svgStream);
                break;
            case "polygon":
                processPolygon(svgStream);
                break;
            case "line":
                processLine(svgStream);
                break;
            case "circle":
                processCircle(svgStream);
                break;
            case "ellipse":
                processEllipse(svgStream);
                break;
            case "rect":
                processRect(svgStream);
                break;
            case "polyline":
                processPolyline(svgStream);
                break;
            case "text":
                processText(svgStream);
                break;
            case "defs":
                svgObjStack.push(new SvgDef(defs));
                break;
            case "clipPath":
                processClipPath(svgStream);
                break;
            case "linearGradient":
                processLinearGradient(svgStream);
                break;
            case "radialGradient":
                processRadialGradient(svgStream);
                break;
            case "stop":
                processGradientStop(svgStream);
                break;
            case "style":
                // This will require more advanced CSS parsing,
                // and a map of defaults to apply to various element types
                // e.g. <style> svg { fill: none; stroke-width: 30px; } </style>
                // mean s stroke width of 30 should be set on all shapes that
                // don't explicitly set their own.
                LOGGER.log(Level.WARNING, "<style> element isn't supported yet");
                // fall-through
            default:
                LOGGER.log(logLevel, () -> "Unhandled element: \""+name+"\", attribute count = "+svgStream.getAttributeCount());
        }
    }

    private static RandomGenerator rng = RandomGenerator.getDefault();
    
    private Paint paintFromAttr(String paint) {
        return paintFromAttr(defs, paint);
    }
    
    private static Paint paintFromAttr(Map<String,Object> defs, String paint) {
        if ("none".equals(paint))
            return null;
        if (paint.startsWith("url(#")) {
            String ref = paint.substring(5,paint.indexOf(')'));
            Object obj = defs.get(ref);
            if (obj instanceof Paint p) {
                //System.out.println("Found paint: "+paint);
                return p;
            } else {
                LOGGER.log(Level.WARNING, () -> "No Paint looking up \""+ref+'"');
                return Color.rgb(rng.nextInt(256),rng.nextInt(256),rng.nextInt(256));
            }
        }
        return Color.valueOf(paint);
    }
    
    private Node clipFromAttr(String clip) {
        if (clip.startsWith("url(#")) {
            var ref = clip.substring(5,clip.indexOf(')'));
            Object obj = defs.get(rootId + '-' +ref);
            if (obj instanceof Node n) {
                return n;
            } else {
                LOGGER.log(Level.WARNING, () -> "Can't find clip for id=\""+ref+'"');
            }
        } else {
            LOGGER.log(Level.WARNING, () -> "clip-path isn't referencing a url(): "+clip);
        }
        return null;
    }

    private void processGroup(XMLStreamReader svgStream) {
        SvgGroup g = new SvgGroup();
        processShapeAttributes(g.group(), svgStream);
        svgObjStack.push(g);
    }
    
    private void processClipPath(XMLStreamReader svgStream) {
        var g = new Group();
        processShapeAttributes(g, svgStream); // will assign ID
        svgObjStack.push(new SvgClip(g));
    }

    private void processPath(XMLStreamReader svgStream) {
        SVGPath path = new SVGPath();
        processShapeAttributes(path, svgStream);
        push(path);
    }
    
    private void processPolygon(XMLStreamReader svgStream) {
        Polygon poly = new Polygon();
        processShapeAttributes(poly, svgStream);
        push(poly);
    }

    private void processLine(XMLStreamReader svgStream) {
        Line line = new Line();
        processShapeAttributes(line, svgStream);
        push(line);
    }
    
    private void processRect(XMLStreamReader svgStream) {
        Rectangle rect = new Rectangle();
        processShapeAttributes(rect, svgStream);
        push(rect);
    }

    private void processCircle(XMLStreamReader svgStream) {
        Circle circle = new Circle();
        processShapeAttributes(circle, svgStream);
        push(circle);
    }
    
    private void processEllipse(XMLStreamReader svgStream) {
        Ellipse ellipse = new Ellipse();
        processShapeAttributes(ellipse, svgStream);
        push(ellipse);
    }

    private void processPolyline(XMLStreamReader svgStream) {
        Polyline polyline = new Polyline();
        processShapeAttributes(polyline, svgStream);
        push(polyline);
    }
    
    private void processText(XMLStreamReader svgStream) {
        Text text = new Text();
        processShapeAttributes(text, svgStream);
        try {
            text.setText(svgStream.getElementText());
        } catch(XMLStreamException ex) {
            LOGGER.log(Level.SEVERE,"Failed reading text", ex);
        }
        push(text);
    }
    
    private void processLinearGradient(XMLStreamReader svgStream) {
        SvgLinearGradient svgGradient = new SvgLinearGradient(defs);
        processLinearGradientAttributes(svgGradient, svgStream);
        svgObjStack.push(svgGradient);
    }
    
    private void processRadialGradient(XMLStreamReader svgStream) {
        SvgRadialGradient svgGradient = new SvgRadialGradient(defs);
        processRadialGradientAttributes(svgGradient, svgStream);
        svgObjStack.push(svgGradient);
    }
    
    private void processGradientStop(XMLStreamReader svgStream) {
        SvgStop svgStop = new SvgStop();
        processGradientStopAttributes(svgStop, svgStream);
        svgObjStack.push(svgStop);
    }
    
    private void push(Node n) {
        svgObjStack.push(new SvgNode(n));
    }
    
    private void processLinearGradientAttributes(SvgLinearGradient grad, XMLStreamReader svgStream) {
        boolean usedPercent = false;
        final int attrCount = svgStream.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            String attrName = svgStream.getAttributeLocalName(i);
            String value = svgStream.getAttributeValue(i);
            if (value.endsWith("%")) {
                usedPercent = true;
            }
            switch (attrName) {
                case "x1":
                    grad.setX1(parseValue(value));
                    break;
                case "y1":
                grad.setY1(parseValue(value));
                break;
                case "x2":
                grad.setX2(parseValue(value));
                break;
                case "y2":
                    grad.setY2(parseValue(value));
                    break;
                    case "gradientUnits":
                    break;
                    case "gradientTransform":
                    grad.setTransform(value);
                    break;
                    case "spreadMethod":
                    CycleMethod cycle = switch(value) {
                        default -> CycleMethod.NO_CYCLE;
                        case "pad" -> CycleMethod.NO_CYCLE;
                        case "reflect" -> CycleMethod.REFLECT;
                        case "repeat" -> CycleMethod.REPEAT;
                    };
                    grad.setCycleMethod(cycle);
                    break;
                case "href":
                if (value.startsWith("#")) {
                    grad.setHref(value.substring(1));
                }
                break;
                case "id":
                String id = value;
                grad.id(id);
                break;
            }
        }
        grad.setProportional(usedPercent); // dumb heuristic
    }

    private void processRadialGradientAttributes(SvgRadialGradient grad, XMLStreamReader svgStream) {
        final int attrCount = svgStream.getAttributeCount();
        boolean usedPercent = false;
        for (int i = 0; i < attrCount; i++) {
            String attrName = svgStream.getAttributeLocalName(i);
            String value = svgStream.getAttributeValue(i);
            if (value.endsWith("%")) {
                usedPercent = true;
            }
            switch (attrName) {
                case "fx":
                    grad.setFx(parseValue(value));
                    break;
                case "fy":
                    grad.setFy(parseValue(value));
                    break;
                case "cx":
                    grad.setCx(parseValue(value));
                    break;
                case "cy":
                    grad.setCy(parseValue(value));
                    break;
                case "r":
                    grad.setR(parseValue(value));
                    break;
                case "gradientUnits":
                    break;
                case "gradientTransform":
                    grad.setTransform(value);
                    break;
                case "spreadMethod":
                    grad.setCycleMethod(cycleMethod(value));
                    break;
                case "href":
                    if (value.startsWith("#")) {
                        grad.setHref(value.substring(1));
                    } else {
                        LOGGER.log(Level.WARNING, () -> "Ignoring external href, "+attrName+':'+value);
                    }
                    break;
                case "id":
                    grad.id(value);
                    break;
                case "fr":
                    // TODO: new in SVG 2.0
                    // To map to JavaFX we will need to use this to adjust the
                    // stop positions.
                    // fall-through
                default:
                    LOGGER.log(logLevel, () -> "Ignoring style: "+attrName+':'+value);
            }
        }
        grad.setProportional(usedPercent); // dumb heuristic
    }
    
    private void processGradientStopAttributes(SvgStop svgStop, XMLStreamReader svgStream) {
        final int attrCount = svgStream.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            String attrName = svgStream.getAttributeLocalName(i);
            String value = svgStream.getAttributeValue(i);
            //System.out.println("attr: " + attrName + ", value: " + value);
            switch (attrName) {
                case "offset":
                    svgStop.setOffset(parseValue(value)); // can be a percentage
                    break;
                case "style": // e.g. stop-opacity:1;stop-color:#27aae1
                {
                    double opacity = 1.0;
                    String color = "#000";
                    String[] properties = value.split("\\s*;\\s*");
                    for (String prop : properties) {
                        String[] keyvalue = prop.split("\\s*:\\s*");
                        if (keyvalue.length >= 2) {
                            switch(keyvalue[0].trim()) {
                                case "stop-opacity":
                                    opacity = Double.parseDouble(keyvalue[1]);
                                    break;
                                case "stop-color":
                                    color = keyvalue[1];
                                    break;
                            }
                        }
                    }
                    svgStop.setOpacity(opacity);
                    svgStop.setColor(color);
                    break;
                }
                case "stop-color":
                    svgStop.setColor(value);
                    break;
                case "stop-opacity":
                    svgStop.setOpacity(Double.parseDouble(value));
                    break;
                case "id":
                    svgStop.id(value);
                    break;
            }
        }
    }
    
    // Also handles Groups
    private void processShapeAttributes(Node node, XMLStreamReader svgStream) {
        if (node instanceof Shape shape) {
            shape.setStroke(null); // SVG default for stroke is invisible/absent
        }
        final int attrCount = svgStream.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            String attrName = svgStream.getAttributeLocalName(i);
            String value = svgStream.getAttributeValue(i);
            //System.out.println("attr: " + attrName + ", value: " + value);
            switch (attrName) {
                case "fill":
                    if (node instanceof Shape shape) {
                        // Deferred because it may use forward references
                        deferredValues.add(new DeferredFill(shape, value, defs));
                    } else {
                        // TODO: support inheriting values from Group
                        LOGGER.log(logLevel, () -> attrName+" ignored, Node is not a Shape: "+node.getClass().getName());
                    }
                    break;
                case "stroke":
                    if (node instanceof Shape shape) {
                        deferredValues.add(new DeferredStroke(shape, value, defs));
                    } else {
                        LOGGER.log(logLevel, () -> attrName+" ignored, Node is not a Shape: "+node.getClass().getName());
                    }
                    break;
                case "stroke-linecap":
                    if (node instanceof Shape shape) {
                        shape.setStrokeLineCap(lineCap(value));
                    } else {
                        LOGGER.log(logLevel, () -> attrName+" ignored, Node is not a Shape: "+node.getClass().getName());
                    }
                    break;
                case "stroke-linejoin":
                    if (node instanceof Shape shape) {
                        shape.setStrokeLineJoin(lineJoin(value));
                    } else {
                        LOGGER.log(logLevel, () -> attrName+" ignored, Node is not a Shape: "+node.getClass().getName());
                    }
                    break;
                case "stroke-miterlimit":
                    if (node instanceof Shape shape) {
                        shape.setStrokeMiterLimit(sizeFromAttr(value));
                    } else {
                        LOGGER.log(logLevel, () -> attrName+" ignored, Node is not a Shape: "+node.getClass().getName());
                    }
                    break;
                case "stroke-width":
                    if (node instanceof Shape shape) {
                        shape.setStrokeWidth(sizeFromAttr(value));
                    } else {
                        LOGGER.log(logLevel, () -> attrName+" ignored, Node is not a Shape: "+node.getClass().getName());
                    }
                    break;
                case "stroke-dasharray":
                    if (node instanceof Shape shape) {
                        shape.getStrokeDashArray().addAll(pointsList(value));// comma separated numbers
                    } else {
                        LOGGER.log(logLevel, () -> attrName+" ignored, Node is not a Shape: "+node.getClass().getName());
                    }
                    break;
                case "fill-opacity":
                case "stroke-opacity":
                    // TODO: tweak colour
                    LOGGER.log(logLevel, "fill-opacity and stroke-opacity are not supported yet.");
                    break;
                case "opacity":
                    node.setOpacity(Double.parseDouble(value));
                    break;
                case "clip-path":
                    node.setClip(clipFromAttr(value));
                    break;
                // Usually id is set for Group
                case "id":
                    String id = rootId + '-' + value;
                    //System.out.println("id=\"" + id + "\"");
                    node.setId(id);
                    break;
                case "transform":
                    applyTransform(node, value);
                    break;
                case "style":
                    applyStyles(node, value);
                    break;
                // Path
                case "d":
                    if(node instanceof SVGPath path) {
                        path.setContent(value);
                    } else {
                        LOGGER.log(logLevel, "d ignored, Node is not a SVGPath,");
                    }
                    break;
                // Polygon or Polyline
                case "points":
                {
                    Double[] p = points(value);
                    if (node instanceof Polygon polygon) {
                        polygon.getPoints().addAll(p);
                    } else if (node instanceof Polyline polyline) {
                        polyline.getPoints().addAll(p);
                    } else {
                        LOGGER.log(logLevel, "points ignored, Node is not a Polygon or Polyline.");
                    }
                    break;
                }
                // Rectangle
                case "x":
                {
                    double x = sizeFromAttr(value);
                    if (node instanceof Rectangle rect) {
                        rect.setX(x);
                    } else if (node instanceof Text text) {
                        text.setX(x);
                    } else {
                        LOGGER.log(logLevel, "x ignored, Node is not a Rectangle or Text.");
                    }
                    break;
                }
                case "y":
                {
                    double y = sizeFromAttr(value);
                    if (node instanceof Rectangle rect) {
                        rect.setY(y);
                    } else if (node instanceof Text text) {
                        text.setY(y);
                    } else {
                        LOGGER.log(logLevel, "y ignored, Node is not a Rectangle or Text.");
                    }
                    break;
                }
                case "rx": // rect or ellipse
                {
                    double rx = sizeFromAttr(value);
                    if (node instanceof Rectangle rect) {
                        rect.setArcWidth(rx);
                    } else if (node instanceof Ellipse ellipse) {
                        ellipse.setRadiusX(rx);
                    } else {
                        LOGGER.log(logLevel, "rx ignored, Node is not a Rectangle or Ellipse.");
                    }
                    break;
                }
                case "ry":
                    double ry = sizeFromAttr(value);
                    if (node instanceof Rectangle rect) {
                        rect.setArcHeight(ry);
                    } else if (node instanceof Ellipse ellipse) {
                        ellipse.setRadiusY(ry);
                    } else {
                        LOGGER.log(logLevel, "ry ignored, Node is not a Rectangle or Ellipse.");
                    }
                    break;
                case "width":
                    if (node instanceof Rectangle rect) {
                        rect.setWidth(sizeFromAttr(value));
                    } else {
                        LOGGER.log(logLevel, "width ignored, Node is not a Rectangle.");
                    }
                    break;
                case "height":
                    if (node instanceof Rectangle rect) {
                        rect.setHeight(sizeFromAttr(value));
                    } else {
                        LOGGER.log(logLevel, "height ignored, Node is not a Rectangle.");
                    }
                    break;
                // Circle or Ellipse
                case "cx":
                    double cx = sizeFromAttr(value);
                    if (node instanceof Circle circ) {
                        circ.setCenterX(cx);
                    } else if (node instanceof Ellipse ellipse) {
                        ellipse.setCenterX(cx);
                    }
                case "cy":
                    double cy = sizeFromAttr(value);
                    if (node instanceof Circle circ) {
                        circ.setCenterY(cy);
                    } else if (node instanceof Ellipse ellipse) {
                        ellipse.setCenterY(cy);
                    }
                    break;
                case "r": // circle
                    if (node instanceof Circle circ) {
                        circ.setRadius(sizeFromAttr(value));
                    } else {
                        LOGGER.log(logLevel, "r ignored, Node is not a Circle.");
                    }
                    break;
                // Line
                case "x1":
                    if (node instanceof Line line) {
                        line.setStartX(sizeFromAttr(value));
                    } else {
                        LOGGER.log(logLevel, "x1 ignored, Node is not a Line.");
                    }
                    break;
                case "y1":
                    if (node instanceof Line line) {
                        line.setStartY(sizeFromAttr(value));
                    } else {
                        LOGGER.log(logLevel, "y1 ignored, Node is not a Line.");
                    }
                    break;
                case "x2":
                    if (node instanceof Line line) {
                        line.setEndX(sizeFromAttr(value));
                    } else {
                        LOGGER.log(logLevel, "x2 ignored, Node is not a Line.");
                    }
                    break;
                case "y2":
                    if (node instanceof Line line) {
                        line.setEndY(sizeFromAttr(value));
                    } else {
                        LOGGER.log(logLevel, "y2 ignored, Node is not a Line.");
                    }
                    break;
                default:
                    System.out.println("Ignoring attribute: "+attrName+'='+value);
            }
        }
    }
    
    private CycleMethod cycleMethod(String value) {
        return switch (value) {
            default -> CycleMethod.NO_CYCLE;
            case "pad" -> CycleMethod.NO_CYCLE;
            case "reflect" -> CycleMethod.REFLECT;
            case "repeat" -> CycleMethod.REPEAT;
        };
    }
            
    private StrokeLineJoin lineJoin(String value) {
        return switch(value) {
            case "bevel" -> StrokeLineJoin.BEVEL;
            case "miter" -> StrokeLineJoin.MITER;
            case "round" -> StrokeLineJoin.ROUND;
            default -> StrokeLineJoin.ROUND;
        };
    }
    private StrokeLineCap lineCap(String value) {
        return switch(value) {
            case "butt" -> StrokeLineCap.BUTT;
            case "square" -> StrokeLineCap.SQUARE;
            case "round" -> StrokeLineCap.ROUND;
            default -> StrokeLineCap.ROUND;
        };
    }
    
    private static Double[] points(String points) {
        ArrayList<Double> pointList = new ArrayList<>();
        String[] values = points.split("\s+,?\s*|,\s*");
        for (String v : values) {
            try {
                pointList.add(Double.valueOf(v));
            } catch(NumberFormatException ex) {}
        }
        return pointList.toArray(Double[]::new);
    }

    private Double[] pointsList(String points) {
        ArrayList<Double> pointList = new ArrayList<>();
        String[] values = points.split(",");
        for (String v : values) {
            try {
                pointList.add(Double.valueOf(v.trim()));
            } catch(NumberFormatException ex) {}
        }
        return pointList.toArray(Double[]::new);
    }
    
    private void applyTransform(Node node, String value) {
        node.getTransforms().addAll(transformsFromString(value));
    }
    
    public static Transform transformsFromString(String value) {
        Transform t = new Affine();
        value = value.trim();
        //LOGGER.log(logLevel, value);
        while(!value.isEmpty()) {
            int paramEnd = value.indexOf(')')+1;
            if (paramEnd <= 0)
                break;
            final String transform = value.substring(0, paramEnd);
            t = t.createConcatenation(transformFromString(transform));
            value = value.substring(paramEnd).trim();
        }
        return t;
    }
    
    public static Transform transformFromString(String transform) {
        int paramStart = transform.indexOf('(') + 1;
        int paramEnd = transform.indexOf(')', paramStart);
        if (paramStart < 0 || paramEnd < 0) {
            throw new RuntimeException("No parameters for transform: "+transform);
        }
        Double[] p = points(transform.substring(paramStart, paramEnd));
        if (transform.startsWith("matrix")) {
            if (p.length == 6) {
                //System.out.println("matrix " + Arrays.toString(p));
                //  SVG  0  2  4   Order of parameters
                //       1  3  5   are column-wise
                //
                //JavaFX 0  1  2   Order of parameters
                //       3  4  5   are row-wise
                //
                return new Affine(p[0], p[2], p[4], p[1], p[3], p[5]);
            } else {
                LOGGER.log(Level.WARNING, () -> "Bad Matrix " + Arrays.toString(p));
            }
        } else if (transform.startsWith("translate")) {
            //System.out.println("translate " + Arrays.toString(p));
            if (p.length >= 2) {
                return new Translate(p[0], p[1]);
            } else if (p.length == 1) {
                return new Translate(p[0], 0);
            }
        } else if (transform.startsWith("rotate")) {
            //System.out.println("rotate " + Arrays.toString(p));
            if (p.length >= 3) {
                return new Rotate(p[0], p[1], p[2]);
            } else if (p.length == 1) {
                return new Rotate(p[0]);
            }
        } else if (transform.startsWith("scale")) {
            //System.out.println("scale " + Arrays.toString(p));
            if (p.length >= 2) {
                return new Scale(p[0], p[1]);
            } else if (p.length == 1) {
                return new Scale(p[0], p[0]);
            }
        } else if (transform.startsWith("skewX")) {
            if (p.length >= 1) {
                double angle = Math.toRadians(p[0]);
                return new Shear(Math.tan(angle), 0);
            }
        } else if (transform.startsWith("skewY")) {
            if (p.length >= 1) {
                double angle = Math.toRadians(p[0]);
                return new Shear(0, Math.tan(angle));
            }
        }

        throw new RuntimeException("Unhandled transform: "+transform);
    }

    // TODO: may need to change this to use SvgNode wrapper to support deferred
    // application of paint and construction of paint from separate color and
    // opacity values
    private void applyStyles(Node node, String style) {
        String[] parts = style.split(";\\s*");
        for (String part : parts) {
            String[] keyValue = part.split("\\s*:\\s*");
            if (keyValue.length != 2) {
                LOGGER.log(Level.WARNING, () -> "Odd style info: \""+part+"\" split into: "+Arrays.toString(keyValue));
                continue;
            }
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();
            switch(key) {
                case "stroke":
                    if (node instanceof Shape s) {
                        s.setStroke(paintFromAttr(value));
                    }
                    break;
                case "stroke-linecap":
                    if (node instanceof Shape s) {
                        s.setStrokeLineCap(lineCap(value));
                    }
                    break;
                case "stroke-linejoin":
                    if (node instanceof Shape s) {
                        s.setStrokeLineJoin(lineJoin(value));
                    }
                    break;
                case "stroke-miterlimit":
                    if (node instanceof Shape s) {
                        s.setStrokeMiterLimit(sizeFromAttr(value));
                    }
                    break;
                case "stroke-opacity":
                    // TODO: support stroke-opacity != 1
                    if (node instanceof Shape s) {
                        double op = Double.parseDouble(value);
                        if (op != 1.0) {
                            LOGGER.log(logLevel, () -> "Style not supported yet, Key: "+key+", value: "+value);
                        }
                    }
                    break;
                case "fill":
                    if (node instanceof Shape s) {
                        s.setFill(paintFromAttr(value));
                    }
                    break;
                case "fill-opacity":
                    // TODO: support fill-opacity != 1
                    if (node instanceof Shape s) {
                        double op = Double.parseDouble(value);
                        if (op != 1.0) {
                            LOGGER.log(logLevel, () -> "Style not supported yet, Key: "+key+", value: "+value);
                        }
                    }
                    break;
                case "font-family":
                    if (node instanceof Text text) {
                        text.setFont(Font.font(value));
                    }
                    break;
                case "stroke-width":
                    if (node instanceof Shape s) {
                        s.setStrokeWidth(sizeFromAttr(value));
                    }
                    break;
                case "stroke-dasharray":
                    if (!"none".equals(value)) {
                        LOGGER.log(logLevel, () -> "Style not supported yet, Key: "+key+", value: "+value);
                    }
                    break;
                case "fill-rule": {
                    var rule = switch(value) {
                            case "evenodd" -> FillRule.EVEN_ODD;
                            default -> FillRule.NON_ZERO;
                        };
                    if (node instanceof SVGPath s) {
                        s.setFillRule(rule);
                    }
                    break;
                }
                case "transform":
                default:
                    LOGGER.log(logLevel, () -> "Style not supported yet, Key: "+key+", value: "+value);
                    break;
            }
        }
        // for text font-family, font-size, ...
        // posisble transforms... etc
    }

    double parseValue(String value) {
        if (value.endsWith("%")) {
            return Double.parseDouble(value.substring(0, value.length()-1)) / 100.0;
        }
        return Double.parseDouble(value);
    }

    double sizeFromAttr(String value) {
        double w = 1.0;
        if ("none".equals(value)) {
            w = 0.0;
        } else if (value.length() > 2 && !Character.isDigit(value.charAt(value.length()-1))) {
            // has units
            //  px | mm | cm | in | pt | pc | em | ex
            // Would be nice to be able to use the JavaFX CSS Parser here.
            // E.g.
            //var font = Font.font(null);
            //ParsedValue<ParsedValue<String, Size>, Number> parsedValue = ???
            //w = javafx.css.converter.SizeConverter.getInstance().convert(parsedValue, font).doubleValue();

            w = Double.parseDouble(value.substring(0, value.length()-2));

            if (value.endsWith("pc")) {
                w *= 12.0; // 12 points in a pica
            } else if (value.endsWith("em")) {
                // wrong because we aren't using the right font
                Text t = new Text("m");
                t.applyCss();
                w *= t.getBoundsInLocal().getWidth();
            } else if (value.endsWith("ex")) {
                // wrong because we aren't using the right font
                Text t = new Text("x");
                t.applyCss();
                w *= t.getBoundsInLocal().getWidth();
            } else if (value.endsWith("mm")) {
                w *= Screen.getPrimary().getDpi() * 0.03937;
            } else if (value.endsWith("cm")) {
                w *= Screen.getPrimary().getDpi() * 0.3937;
            } else if (value.endsWith("in")) {
                w *= Screen.getPrimary().getDpi(); // could be wrong screen
            }
        } else {
            w = Double.parseDouble(value);
        }
        return w;
    }

}
