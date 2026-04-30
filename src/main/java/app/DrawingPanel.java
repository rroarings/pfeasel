package app;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import paintcomponents.PaintElement;
import paintcomponents.PolygonElement;
import paintcomponents.TextElement;

public class DrawingPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DrawingPanel.class);

    private final Main host;
    private final DrawingController drawingController;
    private final CanvasRenderer canvasRenderer;
    private final ShapeCreationService shapeCreationService;

    private Point dragStartPoint;
    private Point startPoint;
    private Point endPoint;
    private Rectangle currentDrawingRectangle;
    private final List<Point> currentPolygonPoints = new ArrayList<>();
    private boolean isDrawingPolygon = false;

    public DrawingPanel(Main host, DrawingController drawingController, CanvasRenderer canvasRenderer, ShapeCreationService shapeCreationService) {
        this.host = host;
        this.drawingController = drawingController;
        this.canvasRenderer = canvasRenderer;
        this.shapeCreationService = shapeCreationService;

        setPreferredSize(new Dimension(765, 503));
        setBackground(Color.LIGHT_GRAY);
        setFocusable(true);

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                host.updateMouseCoordinates(e.getX(), e.getY());
                ToolboxFrame toolboxFrame = host.getToolboxFrame();
                ToolboxFrame.ToolType selectedTool = (toolboxFrame != null) ? toolboxFrame.getSelectedTool() : null;
                Point currentMousePoint = e.getPoint();

                if (selectedTool == ToolboxFrame.ToolType.MOVE) {
                    drawingController.handleMouseMoved(currentMousePoint, DrawingPanel.this);
                } else {
                    setCursor(java.awt.Cursor.getDefaultCursor());
                }

                if (selectedTool == ToolboxFrame.ToolType.POLYGON) {
                    endPoint = host.isSnapToGridActive() && host.getGridManager().isGridVisible()
                            ? snapPointToGrid(currentMousePoint)
                            : currentMousePoint;
                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                host.updateMouseCoordinates(e.getX(), e.getY());
                ToolboxFrame toolboxFrame = host.getToolboxFrame();
                ToolboxFrame.ToolType selectedTool = (toolboxFrame != null) ? toolboxFrame.getSelectedTool() : null;
                Point currentMousePoint = e.getPoint();

                endPoint = host.isSnapToGridActive() && host.getGridManager().isGridVisible()
                        ? snapPointToGrid(currentMousePoint)
                        : currentMousePoint;

                if (selectedTool == ToolboxFrame.ToolType.MOVE) {
                    drawingController.handleMouseDragged(currentMousePoint, DrawingPanel.this);
                } else if (selectedTool == ToolboxFrame.ToolType.LINE) {
                    repaint();
                } else if ((selectedTool == ToolboxFrame.ToolType.RECTANGLE
                        || selectedTool == ToolboxFrame.ToolType.ROUND_RECTANGLE
                        || selectedTool == ToolboxFrame.ToolType.CIRCLE)
                        && dragStartPoint != null) {
                    currentDrawingRectangle = shapeCreationService.calculateBounds(dragStartPoint, endPoint);
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(java.awt.Cursor.getDefaultCursor());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                ToolboxFrame toolboxFrame = host.getToolboxFrame();
                ToolboxFrame.ToolType selectedTool = (toolboxFrame != null) ? toolboxFrame.getSelectedTool() : null;
                Point currentPoint = e.getPoint();

                if (selectedTool == ToolboxFrame.ToolType.MOVE) {
                    drawingController.handleMousePressed(currentPoint, DrawingPanel.this);
                    return;
                }

                drawingController.clearSelection(DrawingPanel.this);
                setCursor(java.awt.Cursor.getDefaultCursor());

                startPoint = host.isSnapToGridActive() && host.getGridManager().isGridVisible()
                        ? snapPointToGrid(currentPoint)
                        : currentPoint;

                currentDrawingRectangle = null;

                if (selectedTool == ToolboxFrame.ToolType.LINE) {
                    endPoint = startPoint;
                } else if (selectedTool == ToolboxFrame.ToolType.RECTANGLE
                        || selectedTool == ToolboxFrame.ToolType.ROUND_RECTANGLE
                        || selectedTool == ToolboxFrame.ToolType.CIRCLE) {
                    dragStartPoint = startPoint;
                    endPoint = startPoint;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                ToolboxFrame toolboxFrame = host.getToolboxFrame();
                ToolboxFrame.ToolType selectedTool = (toolboxFrame != null) ? toolboxFrame.getSelectedTool() : null;

                if (selectedTool == ToolboxFrame.ToolType.MOVE) {
                    drawingController.handleMouseReleased(e.getPoint(), DrawingPanel.this);
                    return;
                }

                endPoint = host.isSnapToGridActive() && host.getGridManager().isGridVisible()
                        ? snapPointToGrid(e.getPoint())
                        : e.getPoint();

                if (startPoint == null) {
                    return;
                }

                if ((selectedTool == ToolboxFrame.ToolType.RECTANGLE
                        || selectedTool == ToolboxFrame.ToolType.ROUND_RECTANGLE
                        || selectedTool == ToolboxFrame.ToolType.CIRCLE)
                        && dragStartPoint != null
                        && toolboxFrame != null) {
                    currentDrawingRectangle = shapeCreationService.calculateBounds(dragStartPoint, endPoint);
                }

                PaintElement newElement = shapeCreationService.createDragElement(
                        selectedTool,
                        currentDrawingRectangle,
                        startPoint,
                        endPoint,
                        toolboxFrame);

                if (newElement != null && toolboxFrame != null) {
                    newElement.setShadow(toolboxFrame.isShadowEnabled());
                    String uniqueDisplayName = host.generateUniqueDisplayName(newElement.getName());
                    newElement.setDisplayName(uniqueDisplayName);
                    host.addPaintElement(newElement);

                    if (currentDrawingRectangle != null
                            && (selectedTool == ToolboxFrame.ToolType.RECTANGLE
                                    || selectedTool == ToolboxFrame.ToolType.ROUND_RECTANGLE
                                    || selectedTool == ToolboxFrame.ToolType.CIRCLE)) {
                        host.setLastActionStatus("Drew " + uniqueDisplayName + " at "
                                + currentDrawingRectangle.x + ", " + currentDrawingRectangle.y);
                    } else if (selectedTool == ToolboxFrame.ToolType.LINE && startPoint != null && endPoint != null) {
                        host.setLastActionStatus("Drew line from " + startPoint.x + ", " + startPoint.y
                                + " to " + endPoint.x + ", " + endPoint.y);
                    } else {
                        host.setLastActionStatus("Drew " + uniqueDisplayName);
                    }
                }

                startPoint = null;
                dragStartPoint = null;
                currentDrawingRectangle = null;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                ToolboxFrame toolboxFrame = host.getToolboxFrame();
                ToolboxFrame.ToolType selectedTool = (toolboxFrame != null) ? toolboxFrame.getSelectedTool() : null;
                Point clickedPoint = e.getPoint();

                logger.info("[DEBUG] mouseClicked entered. Tool: {} , Click at: {}", selectedTool, clickedPoint);

                if (host.isSnapToGridActive() && host.getGridManager().isGridVisible()) {
                    clickedPoint = snapPointToGrid(clickedPoint);
                }

                if (isDrawingPolygon && selectedTool != ToolboxFrame.ToolType.POLYGON) {
                    if (currentPolygonPoints.size() >= 3) {
                        finalizePolygon();
                    } else {
                        isDrawingPolygon = false;
                        currentPolygonPoints.clear();
                        repaint();
                    }
                }

                if (selectedTool == ToolboxFrame.ToolType.TEXT) {
                    if (toolboxFrame == null) {
                        return;
                    }
                    String text = toolboxFrame.getTextInput();
                    if (text.isEmpty()) {
                        JOptionPane.showMessageDialog(host, "Please enter text in the toolbox first.", "Text Input Empty", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    Font font = toolboxFrame.getSelectedFont();
                    Color color = toolboxFrame.getFillColor();
                    if (!toolboxFrame.isFillEnabled()) {
                        color = toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : Color.BLACK;
                    }

                    TextElement textElement = shapeCreationService.createTextElement(text, clickedPoint, font, color);
                    textElement.setShadow(toolboxFrame.isShadowEnabled());
                    String uniqueDisplayName = host.generateUniqueDisplayName(textElement.getName());
                    textElement.setDisplayName(uniqueDisplayName);
                    host.addPaintElement(textElement);
                    repaint();
                    return;
                }

                if (selectedTool == ToolboxFrame.ToolType.POLYGON) {
                    isDrawingPolygon = true;

                    if (!currentPolygonPoints.isEmpty() && currentPolygonPoints.size() >= 2) {
                        Point firstPoint = currentPolygonPoints.get(0);
                        final int closingTolerance = 8;
                        if (clickedPoint.distance(firstPoint) <= closingTolerance) {
                            finalizePolygon();
                            return;
                        }
                    }

                    currentPolygonPoints.add(clickedPoint);

                    if (e.getClickCount() == 2 && currentPolygonPoints.size() >= 3) {
                        finalizePolygon();
                    } else {
                        repaint();
                    }
                }
            }
        });
    }

    private void finalizePolygon() {
        if (currentPolygonPoints.size() >= 3) {
            ToolboxFrame toolboxFrame = host.getToolboxFrame();
            if (toolboxFrame == null) {
                return;
            }

            PolygonElement polygon = shapeCreationService.createPolygonElement(currentPolygonPoints, toolboxFrame);
            polygon.setShadow(toolboxFrame.isShadowEnabled());
            String uniqueDisplayName = host.generateUniqueDisplayName(polygon.getName());
            polygon.setDisplayName(uniqueDisplayName);
            host.addPaintElement(polygon);
            host.setLastActionStatus("Drew polygon '" + uniqueDisplayName + "' with " + currentPolygonPoints.size() + " points");
        }

        currentPolygonPoints.clear();
        isDrawingPolygon = false;
        startPoint = null;
        endPoint = null;
        repaint();
    }

    private Point snapPointToGrid(Point p) {
        int snappedX = (int) (Math.round((double) p.x / host.getGridManager().getGridWidth()) * host.getGridManager().getGridWidth());
        int snappedY = (int) (Math.round((double) p.y / host.getGridManager().getGridHeight()) * host.getGridManager().getGridHeight());
        return new Point(snappedX, snappedY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        canvasRenderer.render(
                g2d,
                host.getToolboxFrame(),
                host.getGridManager(),
                host.getPaintElements(),
                getWidth(),
                getHeight(),
                host.isAntiAliasingActive(),
                host.isDrawRSInterfaceVisible(),
                host.getRsInterfaceImage(),
                drawingController.getSelectedElementForMove(),
                startPoint,
                endPoint,
                currentDrawingRectangle,
                isDrawingPolygon,
                currentPolygonPoints);
    }
}
