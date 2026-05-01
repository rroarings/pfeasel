package paintcomponents;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

public class BezierElement implements PaintElement {
    private List<Point> points;
    private Color strokeColor;
    private float strokeWidth;
    private boolean hasShadow;
    private String displayName;

    public BezierElement(List<Point> points, Color strokeColor, float strokeWidth) {
        this.points = new ArrayList<>(points);
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.hasShadow = false;
    }

    /**
     * Builds a smooth Catmull-Rom spline path through all stored anchor points,
     * offset by (dx, dy).
     */
    private GeneralPath buildPath(int dx, int dy) {
        GeneralPath path = new GeneralPath();
        if (points.size() < 2) return path;
        int n = points.size();
        Point first = points.get(0);
        path.moveTo(first.x + dx, first.y + dy);
        for (int i = 0; i < n - 1; i++) {
            Point p0 = points.get(Math.max(0, i - 1));
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            Point p3 = points.get(Math.min(n - 1, i + 2));
            // Catmull-Rom to cubic Bezier control point conversion
            double cp1x = p1.x + (p2.x - p0.x) / 6.0;
            double cp1y = p1.y + (p2.y - p0.y) / 6.0;
            double cp2x = p2.x - (p3.x - p1.x) / 6.0;
            double cp2y = p2.y - (p3.y - p1.y) / 6.0;
            path.curveTo(cp1x + dx, cp1y + dy, cp2x + dx, cp2y + dy, p2.x + dx, p2.y + dy);
        }
        return path;
    }

    @Override
    public void draw(Graphics2D g2d) {
        if (strokeColor == null || strokeWidth <= 0 || points.size() < 2) return;
        g2d.setColor(strokeColor);
        g2d.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(buildPath(0, 0));
    }

    @Override
    public void drawShadow(Graphics2D g2d, Color shadowColor, int shadowXOffset, int shadowYOffset) {
        if (!hasShadow || shadowColor == null || strokeWidth <= 0 || points.size() < 2) return;
        g2d.setColor(shadowColor);
        g2d.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(buildPath(shadowXOffset, shadowYOffset));
    }

    @Override
    public boolean contains(Point p) {
        if (points.size() < 2) return false;
        GeneralPath path = buildPath(0, 0);
        float hitWidth = Math.max(strokeWidth + 6, 8);
        return new BasicStroke(hitWidth).createStrokedShape(path).contains(p);
    }

    @Override
    public void setPosition(int x, int y) {
        if (points.isEmpty()) return;
        Rectangle b = getBounds();
        int dx = x - b.x;
        int dy = y - b.y;
        List<Point> translated = new ArrayList<>();
        for (Point pt : points) translated.add(new Point(pt.x + dx, pt.y + dy));
        points = translated;
    }

    @Override
    public Point getPosition() {
        return new Point(getBounds().x, getBounds().y);
    }

    @Override
    public Rectangle getBounds() {
        if (points.isEmpty()) return new Rectangle(0, 0, 0, 0);
        if (points.size() == 1) return new Rectangle(points.get(0).x, points.get(0).y, 0, 0);
        return buildPath(0, 0).getBounds();
    }

    @Override
    public boolean hasShadow() { return hasShadow; }

    @Override
    public void setShadow(boolean hasShadow) { this.hasShadow = hasShadow; }

    @Override
    public String getName() { return "Bezier"; }

    @Override
    public String getDisplayName() { return displayName; }

    @Override
    public void setDisplayName(String name) { this.displayName = name; }

    @Override
    public PaintElement duplicate() {
        BezierElement copy = new BezierElement(points, strokeColor, strokeWidth);
        copy.setShadow(hasShadow);
        copy.setDisplayName(displayName);
        return copy;
    }
}
