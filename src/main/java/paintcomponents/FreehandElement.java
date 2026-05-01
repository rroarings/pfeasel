package paintcomponents;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

public class FreehandElement implements PaintElement {
    private List<Point> points;
    private Color strokeColor;
    private float strokeWidth;
    private boolean hasShadow;
    private String displayName;

    public FreehandElement(List<Point> points, Color strokeColor, float strokeWidth) {
        this.points = new ArrayList<>(points);
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.hasShadow = false;
    }

    private GeneralPath buildPath(int dx, int dy) {
        GeneralPath path = new GeneralPath();
        if (points.size() < 2) return path;
        Point first = points.get(0);
        path.moveTo(first.x + dx, first.y + dy);
        for (int i = 1; i < points.size(); i++) {
            Point p = points.get(i);
            path.lineTo(p.x + dx, p.y + dy);
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
        for (int i = 1; i < points.size(); i++) {
            Point a = points.get(i - 1);
            Point b = points.get(i);
            Line2D seg = new Line2D.Double(a.x, a.y, b.x, b.y);
            if (seg.ptSegDist(p) <= strokeWidth / 2.0 + 3) return true;
        }
        return false;
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
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Point p : points) {
            if (p.x < minX) minX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.x > maxX) maxX = p.x;
            if (p.y > maxY) maxY = p.y;
        }
        int buf = (int) Math.ceil(strokeWidth / 2.0) + 1;
        return new Rectangle(minX - buf, minY - buf, maxX - minX + 2 * buf, maxY - minY + 2 * buf);
    }

    @Override
    public boolean hasShadow() { return hasShadow; }

    @Override
    public void setShadow(boolean hasShadow) { this.hasShadow = hasShadow; }

    @Override
    public String getName() { return "Freehand"; }

    @Override
    public String getDisplayName() { return displayName; }

    @Override
    public void setDisplayName(String name) { this.displayName = name; }

    @Override
    public PaintElement duplicate() {
        FreehandElement copy = new FreehandElement(points, strokeColor, strokeWidth);
        copy.setShadow(hasShadow);
        copy.setDisplayName(displayName);
        return copy;
    }
}
