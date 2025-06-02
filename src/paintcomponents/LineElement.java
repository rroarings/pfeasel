package paintcomponents;

import java.awt.*;

public class LineElement implements PaintElement {
    private int x1, y1, x2, y2;
    private Color strokeColor;
    private float strokeWidth;
    private boolean hasShadow; // Added for individual shadow control
    private String displayName;

    public LineElement(int x1, int y1, int x2, int y2, Color strokeColor, float strokeWidth) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.hasShadow = false; // Default to no shadow
    }

    @Override
    public void draw(Graphics2D g2d) {
        if (strokeColor != null && strokeWidth > 0) {
            g2d.setColor(strokeColor);
            g2d.setStroke(new BasicStroke(strokeWidth));
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    @Override
    public boolean contains(Point p) {
        java.awt.geom.Line2D line = new java.awt.geom.Line2D.Double(x1, y1, x2, y2);
        return line.ptSegDist(p) <= strokeWidth / 2 + 2; // Add a small tolerance
    }

    @Override
    public void setPosition(int x, int y) {
        int dx = x - this.x1;
        int dy = y - this.y1;
        this.x1 = x;
        this.y1 = y;
        this.x2 += dx;
        this.y2 += dy;
    }

    @Override
    public Point getPosition() {
        return new Point(this.x1, this.y1);
    }

    @Override
    public void drawShadow(Graphics2D g2d, Color shadowColor, int shadowXOffset, int shadowYOffset) {
        if (strokeColor != null && strokeWidth > 0) { // Only draw shadow if the line itself is visible
            g2d.setColor(shadowColor);
            g2d.setStroke(new BasicStroke(strokeWidth));
            g2d.drawLine(x1 + shadowXOffset, y1 + shadowYOffset, x2 + shadowXOffset, y2 + shadowYOffset);
        }
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
        return "Line " + getX1() + "," + getY1();
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
        LineElement newLine = new LineElement(getX1(), getY1(), getX2(), getY2(), getStrokeColor(), getStrokeWidth());
        newLine.setShadow(this.hasShadow);
        newLine.setDisplayName(this.displayName); // Will be updated by caller
        return newLine;
    }

    // Getters and setters can be added if needed for modification later
    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    @Override
    public java.awt.Rectangle getBounds() {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        // For a line, the width and height of the bounding box are the differences
        // between the max and min coordinates.
        // Add a small buffer to account for stroke width, especially for horizontal/vertical lines.
        int buffer = (int) Math.ceil(strokeWidth / 2.0);
        return new java.awt.Rectangle(minX - buffer, minY - buffer, maxX - minX + 2 * buffer, maxY - minY + 2 * buffer);
    }
}
