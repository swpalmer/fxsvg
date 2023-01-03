/*
 * Copyright (C) 2023 Scott W. Palmer
 * All rights reserved.
 */
package com.analogideas.fxsvg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
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
import javafx.scene.transform.Translate;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

/** A simple SVG reader for JavaFX.
 *
 * @author scott
 */
public class SVGReader {
    private static Node DUMMY = new Group();
    private static final Logger LOGGER = Logger.getLogger(SVGReader.class.getName());
    private static final Level logLevel = Level.INFO;
    private final File svgFile;
    private String rootId;
    private final ArrayDeque<Node> nodeStack = new ArrayDeque<>();

    /**
     * Constructs a SVGReader that will build a Node from the given SVG file.
     * @param svgFile the SVG file to read
     */
    public SVGReader(File svgFile) {
        this.svgFile = Objects.requireNonNull(svgFile);
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
     * @throws java.io.FileNotFoundException 
     * @throws javax.xml.stream.XMLStreamException 
     */
    public Group buildNode() throws FileNotFoundException, XMLStreamException  {
        XMLStreamReader svgStream = javax.xml.stream.XMLInputFactory.newDefaultFactory().createXMLStreamReader(new FileInputStream(svgFile), "utf-8");

        Group svgNode = new Group();
        svgNode.setId(rootId != null ? rootId : svgFile.getName().replace('.', '-').replace('#', '_'));
        nodeStack.clear();
        nodeStack.push(svgNode);

        while (svgStream.hasNext()) {
            int parseEvent = svgStream.next();
            switch (parseEvent) {
                case XMLEvent.START_ELEMENT:
                    String name = svgStream.getLocalName();
                    process(name, svgStream);
                    break;    
                case XMLEvent.END_ELEMENT:
                    Node n = nodeStack.pop();
                    if (n == DUMMY)
                        continue;
                    if (nodeStack.peek() instanceof Group g) {
                        LOGGER.log(logLevel, () -> "Adding "+n+" to "+g);
                        g.getChildren().add(n);
                    }
                    break;    
                case XMLEvent.ATTRIBUTE:
                    String attr = svgStream.getLocalName();
                    String text = svgStream.getText();
                    LOGGER.log(logLevel, () -> "Attribute Event for: "+attr+", value = "+text);
                    break;    
                default:
                    break;
            }
        }
        return svgNode;
    }
    
    // returns true if a node was created based on this element
    private boolean process( String name, XMLStreamReader svgStream) {
        switch (name) {
            case "svg": // beginning a SVG document
                // TODO: process width, height, viewBox
                break;
            case "g": // group
                processGroup(svgStream);
                break;
            case "path": // SVG Path
                processPath(svgStream);
                break;
            case "polygon": // SVG polygon
                processPolygon(svgStream);
                break;
            case "line": // SVG line
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
            case "style":
                // This will require CSS parsing, and a map of defaults to apply to various element types
                // e.g. <style> svg { fill: none; stroke-width: 30px; } </style>
                // mean s stroke width of 30 should be set on all shapes that don't explicitly set their own.
                LOGGER.log(Level.WARNING, "<style> element isn't supported yet");
                // fall-through
            default:
                LOGGER.log(logLevel, () -> "Name: "+name+", attribute count = "+svgStream.getAttributeCount());
                nodeStack.push(DUMMY);
                return false;
        }
        return true;
    }

    private Paint fromFillAttr(String fill) {
        if ("none".equals(fill))
            return null;
        return Color.valueOf(fill);
    }

    private Paint fromStrokeAttr(String stroke) {
        if ("none".equals(stroke))
            return null;
        return Color.valueOf(stroke);
    }
    
    private void processGroup(XMLStreamReader svgStream) {
        Group g = new Group();
        processShapeAttributes(g, svgStream);
        nodeStack.push(g);
    }

    private void processPath(XMLStreamReader svgStream) {
        SVGPath path = new SVGPath();
        processShapeAttributes(path, svgStream);
        nodeStack.push(path);
    }
    
    private void processPolygon(XMLStreamReader svgStream) {
        Polygon poly = new Polygon();
        processShapeAttributes(poly, svgStream);
        nodeStack.push(poly);
    }

    private void processLine(XMLStreamReader svgStream) {
        Line line = new Line();
        processShapeAttributes(line, svgStream);
        nodeStack.push(line);
    }
    
    private void processRect(XMLStreamReader svgStream) {
        Rectangle rect = new Rectangle();
        processShapeAttributes(rect, svgStream);
        nodeStack.push(rect);
    }

    private void processCircle(XMLStreamReader svgStream) {
        Circle circle = new Circle();
        processShapeAttributes(circle, svgStream);
        nodeStack.push(circle);
    }
    
    private void processEllipse(XMLStreamReader svgStream) {
        Ellipse ellipse = new Ellipse();
        processShapeAttributes(ellipse, svgStream);
        nodeStack.push(ellipse);
    }

    private void processPolyline(XMLStreamReader svgStream) {
        Polyline polyline = new Polyline();
        processShapeAttributes(polyline, svgStream);
        nodeStack.push(polyline);
    }
    
    private void processText(XMLStreamReader svgStream) {
        Text text = new Text();
        processShapeAttributes(text, svgStream);
        try {
            text.setText(svgStream.getElementText());
        } catch(XMLStreamException ex) {
            LOGGER.log(Level.SEVERE,"Failed reading text", ex);
        }
        nodeStack.push(text);
    }
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
                    if (node instanceof Shape shape)
                        shape.setFill(fromFillAttr(value));
                    break;
                case "stroke":
                    if (node instanceof Shape shape)
                        shape.setStroke(fromStrokeAttr(value));
                    break;
                case "stroke-linecap":
                    if (node instanceof Shape shape)
                        shape.setStrokeLineCap(lineCap(value));
                    break;
                case "stroke-linejoin":
                    if (node instanceof Shape shape)
                        shape.setStrokeLineJoin(lineJoin(value));
                    break;
                case "stroke-width":
                    if (node instanceof Shape shape)
                        shape.setStrokeWidth(Double.parseDouble(value));
                    break;
                case "stroke-dasharray":
                    if (node instanceof Shape shape)
                        shape.getStrokeDashArray().addAll(pointsList(value));// comma separated numbers
                    break;
                case "fill-opacity":
                case "stroke-opacity":
                    // TODO: tweak colour
                    LOGGER.log(logLevel, "fill-opacity and stroke-opacity are not supported yet.");
                    break;
                // Usually id is set for Group
                case "id":
                    String id = svgFile.getName() + '-' + value;
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
                    }
                    break;
                // Polygon or Polyline
                case "points":
                    if (node instanceof Polygon polygon) {
                        polygon.getPoints().addAll(points(value));
                    } else if (node instanceof Polyline polyline) {
                        polyline.getPoints().addAll(points(value));
                    }
                    break;
                // Rectangle
                case "x":
                    if (node instanceof Rectangle rect) {
                        rect.setX(Double.parseDouble(value));
                    } else if (node instanceof Text text) {
                        text.setX(Double.parseDouble(value));
                    }
                    break;
                case "y":
                    if (node instanceof Rectangle rect) {
                        rect.setY(Double.parseDouble(value));
                    } else if (node instanceof Text text) {
                        text.setY(Double.parseDouble(value));
                    }
                    break;
                case "rx": // rect or ellipse
                    double rx = Double.parseDouble(value);
                    if (node instanceof Rectangle rect) {
                        rect.setArcWidth(rx);
                    } else if (node instanceof Ellipse ellipse) {
                        ellipse.setRadiusX(rx);
                    }
                    break;
                case "ry":
                    double ry = Double.parseDouble(value);
                    if (node instanceof Rectangle rect) {
                        rect.setArcHeight(ry);
                    } else if (node instanceof Ellipse ellipse) {
                        ellipse.setRadiusY(ry);
                    }
                    break;
                case "x1":
                    if (node instanceof Line line) line.setStartX(Double.parseDouble(value));
                    break;
                case "width":
                    if (node instanceof Rectangle rect) rect.setWidth(Double.parseDouble(value));
                    break;
                case "height":
                    if (node instanceof Rectangle rect) rect.setHeight(Double.parseDouble(value));
                    break;
                // Circle or Ellipse
                case "cx":
                    double cx = Double.parseDouble(value);
                    if (node instanceof Circle circ) {
                        circ.setCenterX(cx);
                    } else if (node instanceof Ellipse ellipse) {
                        ellipse.setCenterX(cx);
                    }
                case "cy":
                    double cy = Double.parseDouble(value);
                    if (node instanceof Circle circ) {
                        circ.setCenterY(cy);
                    } else if (node instanceof Ellipse ellipse) {
                        ellipse.setCenterY(cy);
                    }
                    break;
                case "r": // circle
                    if (node instanceof Circle circ) {
                        circ.setRadius(Double.parseDouble(value));
                    }
                    break;
                // Line
                case "y1":
                    if (node instanceof Line line) line.setStartY(Double.parseDouble(value));
                    break;
                case "x2":
                    if (node instanceof Line line) line.setEndX(Double.parseDouble(value));
                    break;
                case "y2":
                    if (node instanceof Line line) line.setEndY(Double.parseDouble(value));
                    break;
                default:
                    System.out.println("Ignoring attribute: "+attrName);
            }
        }
    }
    
    private StrokeLineJoin lineJoin(String value) {
        switch(value) {
            case "bevel":
                return StrokeLineJoin.BEVEL;
            case "miter":
                return StrokeLineJoin.MITER;
            case "round": // fall through
            default:
            return StrokeLineJoin.ROUND;
        }
    }
    private StrokeLineCap lineCap(String value) {
        switch(value) {
            case "butt":
                return StrokeLineCap.BUTT;
            case "square":
                return StrokeLineCap.SQUARE;
            case "round":
            default:
                return StrokeLineCap.ROUND;
        }
    }
    
    private Double[] points(String points) {
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
        value = value.trim();
        while(!value.isEmpty()) {
            System.out.println(value);
            int paramStart = value.indexOf('(')+1;
            int paramEnd = value.indexOf(')', paramStart);
            if (paramStart < 0 || paramEnd < 0)
                break;
            Double [] p = points(value.substring(paramStart, paramEnd));
            if (value.startsWith("matrix")) {
                LOGGER.log(logLevel, () -> "matrix "+Arrays.toString(p));
                if (p.length >= 6) {
                    // SVG   a  c  e   NOTE order of parameters
                    //       b  d  f   are column-wise
                    //
                    //JavaFX a  b  c   NOTE order of parameters
                    //       d  e  f   are row-wise
                    //
                    node.getTransforms().add(new Affine(p[0], p[2], p[4], p[1], p[3], p[5]));
                }
            } else if (value.startsWith("translate")) {
                if (p.length >= 2) {
                    node.getTransforms().add(new Translate(p[0], p[1]));
                } else if (p.length == 1) {
                    node.getTransforms().add(new Translate(p[0], 0));
                }
            } else if (value.startsWith("rotate")) {
                if (p.length >= 3) {
                    node.getTransforms().add(new Rotate(p[0], p[1], p[2]));
                } else if (p.length == 1) {
                    node.getTransforms().add(new Rotate(p[0]));
                }
            } else if (value.startsWith("scale")) {
                if (p.length >= 2) {
                    node.getTransforms().add(new Scale(p[0], p[1]));
                } else if (p.length == 1) {
                    node.getTransforms().add(new Scale(p[0], p[0]));
                }
            } else if (value.startsWith("skewX")) {
                if (p.length >= 1) {
                    double angle = Math.toRadians(p[0]);
                    node.getTransforms().add(new Shear(Math.tan(angle), 0));
                }
            } else if (value.startsWith("skewY")) {
                if (p.length >= 1) {
                    double angle = Math.toRadians(p[0]);
                    node.getTransforms().add(new Shear(0, Math.tan(angle)));
                }
            }
            value = value.substring(paramEnd+1).trim();
        }
    }

    private void applyStyles(Node node, String style) {
        LOGGER.log(logLevel, "style attribute not supported yet");
        String[] parts = style.split(";\\s*");
        for (String part : parts) {
            String[] keyValue = part.split("\\s*:\\s*");
            if (keyValue.length != 2) {
                LOGGER.log(Level.WARNING, () -> "Odd style info: \""+part+"\" split into: "+Arrays.toString(keyValue));
                continue;
            }
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();
            LOGGER.log(logLevel, () -> "Key: "+key+", value: "+value);
            switch(key) {
                case "font-family":
                    if (node instanceof Text text) {
                        text.setFont(Font.font(value));
                    }
                    break;
            }
        }
        // for text font-family, font-size, ...
        // posisble transforms... etc
    }


}
