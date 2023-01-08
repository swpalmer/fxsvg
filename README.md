# fxsvg
Create JavaFX Nodes to render SVG files.


This library implements a quick and dirty SVGReader that creates a hierarchy of standard JavaFX Shapes to render the SVG.
It's very rough, and cuts a lot of corners, but is able to accurately render many SVG files suitable for scalable icons in JavaFX programs.

If you like it and find it useful, [buy me a coffee or a beer](https://paypal.me/swpalmer?country.x=CA&locale.x=en_US). :wink: 

# Usage

The simplest form is:

```java
Node mySvgThing = new SVGReader(new File("/path/to/your/awesome-image.svg")).buildNode();
```

The above method will set the ID of the root Node based on the filename.  Any IDs specified in the SVG will be prefixed with the root Node's ID separated by an underscore.

Another alternative is: 

```java
Node mySvgThing = new SVGReader(getClass().getResourceAsStream("image-resource.svg", "id-for-root-node")).buildNode();
```

This uses the specified ID for the root node.

If the resulting Node doesn't render at the size you need, scale it and wrap it in a Group:

```java
        double desiredWidth = 32.0;
        n.applyCss();
        double w = n.getBoundsInLocal().getWidth();
        n.setScaleX(desiredWidth/w);
        n.setScaleY(desiredWidth/w);
        Group g = new Group(n); // This Node will have the desired width
```
