package paintcomponents;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Point;
import java.awt.Rectangle;

public class CircleElement implements PaintElement {
    private int x, y, width, height; // x, y are top-left of bounding box
    private Color fillColor;
    private Color strokeColor;
    private float strokeWidth;
    private boolean fillEnabled;
    private boolean strokeEnabled;
    private boolean hasShadow; // Added for individual shadow control
    private String displayName;

    public CircleElement(int x, int y, int width, int height, 
                         Color fillColor, Color strokeColor, float strokeWidth,
                         boolean fillEnabled, boolean strokeEnabled) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.fillEnabled = fillEnabled;
        this.strokeEnabled = strokeEnabled;
        this.hasShadow = false; // Default to no shadow
    }

    @Override
    public void draw(Graphics2D g2d) {
        if (fillEnabled && fillColor != null) {
            g2d.setColor(fillColor);
            g2d.fillOval(x, y, width, height);
        }
        if (strokeEnabled && strokeColor != null && strokeWidth > 0) {
            g2d.setColor(strokeColor);
            g2d.setStroke(new BasicStroke(strokeWidth));
            g2d.drawOval(x, y, width, height);
        }
    }

    @Override
    public void drawShadow(Graphics2D g2d, Color shadowColor, int shadowXOffset, int shadowYOffset) {
        if (shadowColor != null) {
            g2d.setColor(shadowColor);
            if (fillEnabled || strokeEnabled) {
                g2d.fillOval(x + shadowXOffset, y + shadowYOffset, width, height);
            }
        }
    }

    @Override
    public boolean contains(Point p) {
        return new Rectangle(x, y, width, height).contains(p);
    }

    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public Point getPosition() {
        return new Point(this.x, this.y);
    }

    @Override
    public boolean hasShadow() {
        return hasShadow;
    }

    @Override
    public void setShadow(boolean hasShadow) {
        this.hasShadow = hasShadow;
    }

    @Override
    public String getName() {
        return "Circle";
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String name) {
        this.displayName = name;
    }

    @Override
    public PaintElement duplicate() {
        CircleElement newCircle = new CircleElement(x, y, width, height, fillColor, strokeColor, strokeWidth, fillEnabled, strokeEnabled);
        newCircle.setShadow(this.hasShadow);
        newCircle.setDisplayName(this.displayName); // Will be updated by caller
        return newCircle;
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}
