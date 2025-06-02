package paintcomponents;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.BasicStroke;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.ArrayList;

public class PolygonElement implements PaintElement {
    private Polygon polygon;
    private Color fillColor;
    private Color strokeColor;
    private float strokeWidth;
    private boolean fillEnabled;
    private boolean strokeEnabled;
    private boolean hasShadow;
    private String displayName;

    public PolygonElement(List<Point> points, Color fillColor, Color strokeColor, float strokeWidth,
                          boolean fillEnabled, boolean strokeEnabled) {
        this.polygon = new Polygon();
        for (Point p : points) {
            this.polygon.addPoint(p.x, p.y);
        }
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.fillEnabled = fillEnabled;
        this.strokeEnabled = strokeEnabled;
        this.hasShadow = false; // Default shadow state
    }

    @Override
    public void draw(Graphics2D g2d) {
        if (fillEnabled && fillColor != null) {
            g2d.setColor(fillColor);
            g2d.fillPolygon(polygon);
        }
        if (strokeEnabled && strokeColor != null && strokeWidth > 0) {
            g2d.setColor(strokeColor);
            g2d.setStroke(new BasicStroke(strokeWidth));
            g2d.drawPolygon(polygon);
        }
    }

    @Override
    public void drawShadow(Graphics2D g2d, Color shadowColor, int shadowXOffset, int shadowYOffset) {
        if (hasShadow && shadowColor != null) {
            Polygon shadowPolygon = new Polygon();
            for (int i = 0; i < polygon.npoints; i++) {
                shadowPolygon.addPoint(polygon.xpoints[i] + shadowXOffset, polygon.ypoints[i] + shadowYOffset);
            }
            g2d.setColor(shadowColor);
            // Draw shadow if the main shape is either filled or stroked
            if (fillEnabled || strokeEnabled) {
                g2d.fillPolygon(shadowPolygon);
            }
        }
    }

    @Override
    public boolean contains(Point p) {
        return polygon.contains(p);
    }

    @Override
    public void setPosition(int x, int y) {
        Rectangle bounds = polygon.getBounds();
        int dx = x - bounds.x;
        int dy = y - bounds.y;
        polygon.translate(dx, dy);
    }

    @Override
    public Point getPosition() {
        Rectangle bounds = polygon.getBounds();
        return new Point(bounds.x, bounds.y);
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
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String name) {
        this.displayName = name;
    }

    @Override
    public String getName() {
        return "Polygon";
    }

    @Override
    public PaintElement duplicate() {
        List<Point> newPoints = new ArrayList<>();
        for (int i = 0; i < polygon.npoints; i++) {
            newPoints.add(new Point(polygon.xpoints[i], polygon.ypoints[i]));
        }
        PolygonElement newPolygon = new PolygonElement(newPoints, fillColor, strokeColor, strokeWidth, fillEnabled, strokeEnabled);
        newPolygon.setShadow(this.hasShadow);
        newPolygon.setDisplayName(this.displayName); // Will be updated by caller
        return newPolygon;
    }

    @Override
    public Rectangle getBounds() {
        return polygon.getBounds();
    }
}
