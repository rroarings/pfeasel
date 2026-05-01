package app;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import paintcomponents.PaintElement;
import ui.GridManager;

public class CanvasRenderer {
    private static final int RESIZE_HANDLE_SIZE = 8;

    public void render(
            Graphics2D g2d,
            ToolboxFrame toolboxFrame,
            GridManager gridManager,
            List<PaintElement> paintElements,
            int canvasWidth,
            int canvasHeight,
            boolean antiAliasingActive,
            boolean rsInterfaceVisible,
            BufferedImage rsInterfaceImage,
            PaintElement selectedElementForMove,
            Point startPoint,
            Point endPoint,
            Rectangle currentDrawingRectangle,
            boolean isDrawingPolygon,
            List<Point> currentPolygonPoints,
            List<Point> currentFreehandPoints,
            boolean isDrawingBezier,
            List<Point> currentBezierPoints) {

        if (antiAliasingActive) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }

        if (rsInterfaceVisible && rsInterfaceImage != null) {
            g2d.drawImage(rsInterfaceImage, 0, 0, null);
        }

        if (gridManager.isGridVisible()) {
            gridManager.drawGrid(g2d, canvasWidth, canvasHeight);
        }

        for (int i = paintElements.size() - 1; i >= 0; i--) {
            PaintElement element = paintElements.get(i);
            if (element != null) {
                if (element.hasShadow() && toolboxFrame != null) {
                    element.drawShadow(g2d, toolboxFrame.getShadowColor(), toolboxFrame.getShadowXOffset(), toolboxFrame.getShadowYOffset());
                }
                element.draw(g2d);
            }
        }

        if (toolboxFrame != null && toolboxFrame.getSelectedTool() == ToolboxFrame.ToolType.MOVE && selectedElementForMove != null) {
            Rectangle bounds = selectedElementForMove.getBounds();
            if (bounds != null) {
                g2d.setColor(Color.BLUE);
                float[] dash = {4f, 4f};
                Stroke oldStroke = g2d.getStroke();
                g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                g2d.drawRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4);
                g2d.setStroke(oldStroke);

                if (selectedElementForMove.isResizable() && bounds.width > 0 && bounds.height > 0) {
                    drawResizeHandles(g2d, bounds);
                }
            }
        }

        if (startPoint != null && endPoint != null && toolboxFrame != null) {
            ToolboxFrame.ToolType previewTool = toolboxFrame.getSelectedTool();

            if ((previewTool == ToolboxFrame.ToolType.RECTANGLE
                    || previewTool == ToolboxFrame.ToolType.ROUND_RECTANGLE
                    || previewTool == ToolboxFrame.ToolType.CIRCLE
                    || previewTool == ToolboxFrame.ToolType.LINE)
                    && currentDrawingRectangle != null) {
                g2d.setColor(new Color(0, 120, 255, 128));
                float[] dash = {4f, 4f};
                Stroke oldStroke = g2d.getStroke();
                g2d.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                g2d.drawRect(currentDrawingRectangle.x - 2, currentDrawingRectangle.y - 2,
                        currentDrawingRectangle.width + 4, currentDrawingRectangle.height + 4);
                g2d.setStroke(oldStroke);
            }

            Color previewFillColor = toolboxFrame.isFillEnabled() ? toolboxFrame.getFillColor() : null;
            Color previewStrokeColor = toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : null;
            float previewStrokeWidth = (float) toolboxFrame.getCurrentStrokeWidth();

            if (previewTool == ToolboxFrame.ToolType.LINE) {
                if (toolboxFrame.isStrokeEnabled() && previewStrokeColor != null && previewStrokeWidth > 0) {
                    g2d.setColor(previewStrokeColor);
                    g2d.setStroke(new BasicStroke(previewStrokeWidth));
                    g2d.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
                }
            } else if (currentDrawingRectangle != null) {
                if (toolboxFrame.isFillEnabled() && previewFillColor != null) {
                    g2d.setColor(previewFillColor);
                    switch (previewTool) {
                        case RECTANGLE:
                            g2d.fillRect(currentDrawingRectangle.x, currentDrawingRectangle.y,
                                    currentDrawingRectangle.width, currentDrawingRectangle.height);
                            break;
                        case ROUND_RECTANGLE:
                            g2d.fillRoundRect(currentDrawingRectangle.x, currentDrawingRectangle.y,
                                    currentDrawingRectangle.width, currentDrawingRectangle.height,
                                    toolboxFrame.getArcWidth(), toolboxFrame.getArcHeight());
                            break;
                        case CIRCLE:
                            g2d.fillOval(currentDrawingRectangle.x, currentDrawingRectangle.y,
                                    currentDrawingRectangle.width, currentDrawingRectangle.height);
                            break;
                        default:
                            break;
                    }
                }

                if (toolboxFrame.isStrokeEnabled() && previewStrokeColor != null && previewStrokeWidth > 0) {
                    g2d.setColor(previewStrokeColor);
                    g2d.setStroke(new BasicStroke(previewStrokeWidth));
                    switch (previewTool) {
                        case RECTANGLE:
                            g2d.drawRect(currentDrawingRectangle.x, currentDrawingRectangle.y,
                                    currentDrawingRectangle.width, currentDrawingRectangle.height);
                            break;
                        case ROUND_RECTANGLE:
                            g2d.drawRoundRect(currentDrawingRectangle.x, currentDrawingRectangle.y,
                                    currentDrawingRectangle.width, currentDrawingRectangle.height,
                                    toolboxFrame.getArcWidth(), toolboxFrame.getArcHeight());
                            break;
                        case CIRCLE:
                            g2d.drawOval(currentDrawingRectangle.x, currentDrawingRectangle.y,
                                    currentDrawingRectangle.width, currentDrawingRectangle.height);
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        if (isDrawingPolygon && currentPolygonPoints != null && !currentPolygonPoints.isEmpty()) {
            g2d.setColor(Color.GRAY);
            Point prevPoint = null;
            ToolboxFrame.ToolType currentTool = (toolboxFrame != null) ? toolboxFrame.getSelectedTool() : null;

            for (int i = 0; i < currentPolygonPoints.size(); i++) {
                Point p = currentPolygonPoints.get(i);
                if (i == 0) {
                    g2d.setColor(Color.GREEN);
                    g2d.fillOval(p.x - 4, p.y - 4, 8, 8);
                    g2d.setColor(Color.GRAY);
                } else {
                    g2d.fillOval(p.x - 3, p.y - 3, 6, 6);
                }

                if (prevPoint != null) {
                    g2d.drawLine(prevPoint.x, prevPoint.y, p.x, p.y);
                }
                prevPoint = p;
            }

            if (prevPoint != null && endPoint != null && currentTool == ToolboxFrame.ToolType.POLYGON) {
                g2d.drawLine(prevPoint.x, prevPoint.y, endPoint.x, endPoint.y);
            }
        }

        // Freehand in-progress preview
        if (currentFreehandPoints != null && currentFreehandPoints.size() >= 2 && toolboxFrame != null
                && toolboxFrame.getSelectedTool() == ToolboxFrame.ToolType.FREEHAND) {
            Color strokeColor = toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : Color.BLACK;
            float strokeWidth = (float) toolboxFrame.getCurrentStrokeWidth();
            g2d.setColor(strokeColor);
            g2d.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            GeneralPath previewPath = new GeneralPath();
            Point fp = currentFreehandPoints.get(0);
            previewPath.moveTo(fp.x, fp.y);
            for (int i = 1; i < currentFreehandPoints.size(); i++) {
                Point fp2 = currentFreehandPoints.get(i);
                previewPath.lineTo(fp2.x, fp2.y);
            }
            g2d.draw(previewPath);
        }

        // Bezier in-progress preview
        if (isDrawingBezier && currentBezierPoints != null && !currentBezierPoints.isEmpty() && toolboxFrame != null) {
            Color strokeColor = toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : Color.GRAY;
            float strokeWidth = (float) toolboxFrame.getCurrentStrokeWidth();

            // Build preview list: placed anchors + live mouse position as tentative next point
            List<Point> previewPoints = new ArrayList<>(currentBezierPoints);
            if (endPoint != null) {
                previewPoints.add(endPoint);
            }

            if (previewPoints.size() >= 2) {
                g2d.setColor(strokeColor);
                g2d.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.draw(buildCatmullRomPath(previewPoints, 0, 0));
            } else if (endPoint != null) {
                // Only one placed point — dashed line to mouse
                Stroke oldStroke = g2d.getStroke();
                float[] dash = {4f, 4f};
                g2d.setColor(Color.GRAY);
                g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                Point sole = currentBezierPoints.get(0);
                g2d.drawLine(sole.x, sole.y, endPoint.x, endPoint.y);
                g2d.setStroke(oldStroke);
            }

            // Draw anchor dots for placed points
            for (int i = 0; i < currentBezierPoints.size(); i++) {
                Point bp = currentBezierPoints.get(i);
                if (i == 0) {
                    g2d.setColor(Color.GREEN);
                    g2d.fillOval(bp.x - 4, bp.y - 4, 8, 8);
                } else {
                    g2d.setColor(Color.BLUE);
                    g2d.fillOval(bp.x - 3, bp.y - 3, 6, 6);
                }
            }
        }
    }

    private static GeneralPath buildCatmullRomPath(List<Point> pts, int dx, int dy) {
        GeneralPath path = new GeneralPath();
        if (pts.size() < 2) return path;
        int n = pts.size();
        Point first = pts.get(0);
        path.moveTo(first.x + dx, first.y + dy);
        for (int i = 0; i < n - 1; i++) {
            Point p0 = pts.get(Math.max(0, i - 1));
            Point p1 = pts.get(i);
            Point p2 = pts.get(i + 1);
            Point p3 = pts.get(Math.min(n - 1, i + 2));
            double cp1x = p1.x + (p2.x - p0.x) / 6.0;
            double cp1y = p1.y + (p2.y - p0.y) / 6.0;
            double cp2x = p2.x - (p3.x - p1.x) / 6.0;
            double cp2y = p2.y - (p3.y - p1.y) / 6.0;
            path.curveTo(cp1x + dx, cp1y + dy, cp2x + dx, cp2y + dy, p2.x + dx, p2.y + dy);
        }
        return path;
    }

    private void drawResizeHandles(Graphics2D g2d, Rectangle bounds) {
        int half = RESIZE_HANDLE_SIZE / 2;
        int left = bounds.x;
        int centerX = bounds.x + bounds.width / 2;
        int right = bounds.x + bounds.width;
        int top = bounds.y;
        int centerY = bounds.y + bounds.height / 2;
        int bottom = bounds.y + bounds.height;

        Rectangle[] handles = new Rectangle[] {
                new Rectangle(left - half, top - half, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE),
                new Rectangle(centerX - half, top - half, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE),
                new Rectangle(right - half, top - half, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE),
                new Rectangle(right - half, centerY - half, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE),
                new Rectangle(right - half, bottom - half, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE),
                new Rectangle(centerX - half, bottom - half, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE),
                new Rectangle(left - half, bottom - half, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE),
                new Rectangle(left - half, centerY - half, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE)
        };

        g2d.setColor(Color.WHITE);
        for (Rectangle handle : handles) {
            g2d.fillRect(handle.x, handle.y, handle.width, handle.height);
        }

        g2d.setColor(new Color(0, 70, 190));
        for (Rectangle handle : handles) {
            g2d.drawRect(handle.x, handle.y, handle.width, handle.height);
        }
    }
}
