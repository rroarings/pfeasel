package app;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import paintcomponents.BezierElement;
import paintcomponents.CircleElement;
import paintcomponents.FreehandElement;
import paintcomponents.LineElement;
import paintcomponents.PaintElement;
import paintcomponents.PolygonElement;
import paintcomponents.RectangleElement;
import paintcomponents.RoundRectangleElement;
import paintcomponents.TextElement;

public class ShapeCreationService {

    public Rectangle calculateBounds(Point start, Point end) {
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(start.x - end.x);
        int height = Math.abs(start.y - end.y);
        return new Rectangle(x, y, width, height);
    }

    public PaintElement createDragElement(
            ToolboxFrame.ToolType selectedTool,
            Rectangle drawingBounds,
            Point startPoint,
            Point endPoint,
            ToolboxFrame toolboxFrame) {

        if (toolboxFrame == null || selectedTool == null) {
            return null;
        }

        switch (selectedTool) {
            case RECTANGLE:
                if (drawingBounds == null || drawingBounds.width == 0 || drawingBounds.height == 0) {
                    return null;
                }
                return new RectangleElement(
                        drawingBounds.x,
                        drawingBounds.y,
                        drawingBounds.width,
                        drawingBounds.height,
                        toolboxFrame.isFillEnabled() ? toolboxFrame.getFillColor() : null,
                        toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : null,
                        (float) toolboxFrame.getCurrentStrokeWidth(),
                        toolboxFrame.isFillEnabled(),
                        toolboxFrame.isStrokeEnabled());
            case ROUND_RECTANGLE:
                if (drawingBounds == null || drawingBounds.width == 0 || drawingBounds.height == 0) {
                    return null;
                }
                return new RoundRectangleElement(
                        drawingBounds.x,
                        drawingBounds.y,
                        drawingBounds.width,
                        drawingBounds.height,
                        toolboxFrame.getArcWidth(),
                        toolboxFrame.getArcHeight(),
                        toolboxFrame.isFillEnabled() ? toolboxFrame.getFillColor() : null,
                        toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : null,
                        (float) toolboxFrame.getCurrentStrokeWidth(),
                        toolboxFrame.isFillEnabled(),
                        toolboxFrame.isStrokeEnabled());
            case CIRCLE:
                if (drawingBounds == null || drawingBounds.width == 0 || drawingBounds.height == 0) {
                    return null;
                }
                return new CircleElement(
                        drawingBounds.x,
                        drawingBounds.y,
                        drawingBounds.width,
                        drawingBounds.height,
                        toolboxFrame.isFillEnabled() ? toolboxFrame.getFillColor() : null,
                        toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : null,
                        (float) toolboxFrame.getCurrentStrokeWidth(),
                        toolboxFrame.isFillEnabled(),
                        toolboxFrame.isStrokeEnabled());
            case LINE:
                Color strokeColor = toolboxFrame.getStrokeColor();
                float strokeWidth = (float) toolboxFrame.getCurrentStrokeWidth();
                if (strokeColor == null || strokeWidth <= 0 || startPoint == null || endPoint == null) {
                    return null;
                }
                return new LineElement(startPoint.x, startPoint.y, endPoint.x, endPoint.y, strokeColor, strokeWidth);
            default:
                return null;
        }
    }

    public TextElement createTextElement(String text, Point point, Font font, Color color) {
        return new TextElement(text, point.x, point.y, font, color);
    }

    public PolygonElement createPolygonElement(List<Point> points, ToolboxFrame toolboxFrame) {
        Color fillColor = toolboxFrame.isFillEnabled() ? toolboxFrame.getFillColor() : null;
        Color strokeColor = toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : null;
        float strokeWidth = (float) toolboxFrame.getCurrentStrokeWidth();

        return new PolygonElement(
                points,
                fillColor,
                strokeColor,
                strokeWidth,
                toolboxFrame.isFillEnabled(),
                toolboxFrame.isStrokeEnabled());
    }

    public FreehandElement createFreehandElement(List<Point> points, ToolboxFrame toolboxFrame) {
        Color strokeColor = toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : Color.BLACK;
        float strokeWidth = (float) toolboxFrame.getCurrentStrokeWidth();
        List<Point> simplified = douglasPeucker(points, 2.0);
        return new FreehandElement(simplified, strokeColor, strokeWidth);
    }

    public BezierElement createBezierElement(List<Point> points, ToolboxFrame toolboxFrame) {
        Color strokeColor = toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : Color.BLACK;
        float strokeWidth = (float) toolboxFrame.getCurrentStrokeWidth();
        return new BezierElement(points, strokeColor, strokeWidth);
    }

    /**
     * Iterative Douglas-Peucker polyline simplification.
     * Removes points that deviate less than {@code epsilon} pixels from the
     * straight line between their neighbours.
     */
    private static List<Point> douglasPeucker(List<Point> points, double epsilon) {
        if (points.size() < 3) return new ArrayList<>(points);
        boolean[] keep = new boolean[points.size()];
        keep[0] = true;
        keep[points.size() - 1] = true;
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, points.size() - 1});
        while (!stack.isEmpty()) {
            int[] range = stack.pop();
            int lo = range[0], hi = range[1];
            double maxDist = 0;
            int maxIdx = lo;
            Point a = points.get(lo);
            Point b = points.get(hi);
            for (int i = lo + 1; i < hi; i++) {
                double d = pointToLineDistance(points.get(i), a, b);
                if (d > maxDist) { maxDist = d; maxIdx = i; }
            }
            if (maxDist > epsilon) {
                keep[maxIdx] = true;
                if (maxIdx - lo > 1) stack.push(new int[]{lo, maxIdx});
                if (hi - maxIdx > 1) stack.push(new int[]{maxIdx, hi});
            }
        }
        List<Point> result = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            if (keep[i]) result.add(points.get(i));
        }
        return result;
    }

    private static double pointToLineDistance(Point p, Point a, Point b) {
        double dx = b.x - a.x, dy = b.y - a.y;
        if (dx == 0 && dy == 0) return p.distance(a);
        double t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / (dx * dx + dy * dy);
        return Math.hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy));
    }
}
