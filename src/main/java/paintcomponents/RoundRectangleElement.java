package paintcomponents;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Point;
import java.awt.Rectangle;

public class RoundRectangleElement implements PaintElement {
    private int x, y, width, height, arcWidth, arcHeight;
    private Color fillColor;
    private Color strokeColor;
    private float strokeWidth;
    private boolean fillEnabled;
    private boolean strokeEnabled;
    private boolean hasShadow; // Added for individual shadow control
    private String displayName;

    // Constructor updated to accept arcWidth and arcHeight
    public RoundRectangleElement(int x, int y, int width, int height, 
                                 int arcWidth, int arcHeight, // Added arcWidth, arcHeight parameters
                                 Color fillColor, Color strokeColor, float strokeWidth,
                                 boolean fillEnabled, boolean strokeEnabled) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.arcWidth = arcWidth; // Use passed arcWidth
        this.arcHeight = arcHeight; // Use passed arcHeight
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
            g2d.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
        if (strokeEnabled && strokeColor != null && strokeWidth > 0) {
            g2d.setColor(strokeColor);
            g2d.setStroke(new BasicStroke(strokeWidth));
            g2d.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
    }

    @Override
    public void drawShadow(Graphics2D g2d, Color shadowColor, int shadowXOffset, int shadowYOffset) {
        if (shadowColor != null) {
            g2d.setColor(shadowColor);
            // Draw the shadow slightly offset, only if fill is enabled or stroke is enabled (to have a shape)
            if (fillEnabled || strokeEnabled) { 
                g2d.fillRoundRect(x + shadowXOffset, y + shadowYOffset, width, height, arcWidth, arcHeight);
            }
        }
    }

    @Override
    public boolean contains(Point p) {
        // For simplicity, using rectangular bounds for contains check. 
        // More precise check would involve RoundRectangle2D.contains()
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
        return "RoundRectangle";
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
        RoundRectangleElement newRect = new RoundRectangleElement(x, y, width, height, arcWidth, arcHeight, fillColor, strokeColor, strokeWidth, fillEnabled, strokeEnabled);
        newRect.setShadow(this.hasShadow);
        newRect.setDisplayName(this.displayName); // Will be updated by caller
        return newRect;
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}
