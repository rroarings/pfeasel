package app;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPanel;

import paintcomponents.PaintElement;

public class DrawingController {
    private static final int RESIZE_HANDLE_HIT_SIZE = 12;
    private static final int MIN_RESIZE_DIMENSION = 6;

    private final Main host;

    private Point dragOffset;
    private PaintElement selectedElementForMove;
    private ResizeHandle activeResizeHandle = ResizeHandle.NONE;
    private Rectangle resizeStartBounds;
    private boolean minSizeReachedDuringResize;

    private enum ResizeHandle {
        NONE,
        NORTH_WEST,
        NORTH,
        NORTH_EAST,
        EAST,
        SOUTH_EAST,
        SOUTH,
        SOUTH_WEST,
        WEST
    }

    public DrawingController(Main host) {
        this.host = host;
    }

    public PaintElement getSelectedElementForMove() {
        return selectedElementForMove;
    }

    public void clearSelection(JPanel panel) {
        selectedElementForMove = null;
        activeResizeHandle = ResizeHandle.NONE;
        resizeStartBounds = null;
        dragOffset = null;
        minSizeReachedDuringResize = false;
        panel.setCursor(Cursor.getDefaultCursor());
    }

    public void handleMouseMoved(Point currentPoint, JPanel panel) {
        updateMoveCursor(currentPoint, panel);
    }

    public void handleMouseDragged(Point currentPoint, JPanel panel) {
        if (selectedElementForMove == null) {
            return;
        }

        if (activeResizeHandle != ResizeHandle.NONE && resizeStartBounds != null && selectedElementForMove.isResizable()) {
            Point resizePoint = currentPoint;
            if (host.isSnapToGridActive() && host.getGridManager().isGridVisible()) {
                resizePoint = snapPointToGrid(resizePoint);
            }

            Rectangle resizedBounds = calculateResizedBounds(resizeStartBounds, activeResizeHandle, resizePoint);
            minSizeReachedDuringResize = (resizedBounds.width <= MIN_RESIZE_DIMENSION || resizedBounds.height <= MIN_RESIZE_DIMENSION);
            selectedElementForMove.resizeToBounds(resizedBounds);
            updateMoveCursor(currentPoint, panel);
            host.repaintDrawingPanel();
            return;
        }

        if (dragOffset != null) {
            int newX = currentPoint.x - dragOffset.x;
            int newY = currentPoint.y - dragOffset.y;

            if (host.isSnapToGridActive() && host.getGridManager().isGridVisible()) {
                Point snappedPosition = snapPointToGrid(new Point(newX, newY));
                selectedElementForMove.setPosition(snappedPosition.x, snappedPosition.y);
            } else {
                selectedElementForMove.setPosition(newX, newY);
            }
            host.repaintDrawingPanel();
        }
    }

    public void handleMousePressed(Point currentPoint, JPanel panel) {
        activeResizeHandle = ResizeHandle.NONE;
        resizeStartBounds = null;
        dragOffset = null;
        minSizeReachedDuringResize = false;

        if (selectedElementForMove != null && selectedElementForMove.isResizable()) {
            Rectangle selectedBounds = selectedElementForMove.getBounds();
            ResizeHandle clickedHandle = getResizeHandleAtPoint(currentPoint, selectedBounds);
            if (clickedHandle != ResizeHandle.NONE) {
                activeResizeHandle = clickedHandle;
                resizeStartBounds = new Rectangle(selectedBounds);
                updateMoveCursor(currentPoint, panel);
                host.repaintDrawingPanel();
                return;
            }
        }

        List<PaintElement> paintElements = host.getPaintElements();
        PaintElement hitElement = findTopmostElementAt(currentPoint, paintElements);
        if (hitElement != null) {
            selectedElementForMove = hitElement;
            Point elementPos = selectedElementForMove.getPosition();
            dragOffset = new Point(currentPoint.x - elementPos.x, currentPoint.y - elementPos.y);

            ToolboxFrame toolbox = host.getToolboxFrame();
            if (toolbox != null) {
                int hitIndex = paintElements.indexOf(hitElement);
                if (hitIndex >= 0) {
                    int layerIndex = paintElements.size() - 1 - hitIndex;
                    toolbox.selectLayerInList(layerIndex);
                }
            }
        } else {
            selectedElementForMove = null;
        }
        host.repaintDrawingPanel();
    }

    public void handleMouseReleased(Point currentPoint, JPanel panel) {
        if (selectedElementForMove != null && activeResizeHandle != ResizeHandle.NONE) {
            activeResizeHandle = ResizeHandle.NONE;
            resizeStartBounds = null;
            if (minSizeReachedDuringResize) {
                host.setLastActionStatus("Resized " + host.getElementDisplayName(selectedElementForMove) + " (minimum size reached)");
            } else {
                host.setLastActionStatus("Resized " + host.getElementDisplayName(selectedElementForMove));
            }
            minSizeReachedDuringResize = false;
            updateMoveCursor(currentPoint, panel);
            host.repaintDrawingPanel();
            return;
        }

        if (selectedElementForMove != null && dragOffset != null) {
            int newX = currentPoint.x - dragOffset.x;
            int newY = currentPoint.y - dragOffset.y;
            Point finalElementPos = new Point(newX, newY);

            if (host.isSnapToGridActive() && host.getGridManager().isGridVisible()) {
                finalElementPos = snapPointToGrid(finalElementPos);
            }
            selectedElementForMove.setPosition(finalElementPos.x, finalElementPos.y);
        }

        dragOffset = null;
        updateMoveCursor(currentPoint, panel);
        host.repaintDrawingPanel();
    }

    private PaintElement findTopmostElementAt(Point point, List<PaintElement> paintElements) {
        for (int i = 0; i < paintElements.size(); i++) {
            PaintElement element = paintElements.get(i);
            if (element != null && element.contains(point)) {
                return element;
            }
        }
        return null;
    }

    private Point snapPointToGrid(Point p) {
        int snappedX = (int) (Math.round((double) p.x / host.getGridManager().getGridWidth()) * host.getGridManager().getGridWidth());
        int snappedY = (int) (Math.round((double) p.y / host.getGridManager().getGridHeight()) * host.getGridManager().getGridHeight());
        return new Point(snappedX, snappedY);
    }

    private ResizeHandle getResizeHandleAtPoint(Point point, Rectangle bounds) {
        if (point == null || bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            return ResizeHandle.NONE;
        }

        ResizeHandle[] handles = {
                ResizeHandle.NORTH_WEST,
                ResizeHandle.NORTH,
                ResizeHandle.NORTH_EAST,
                ResizeHandle.EAST,
                ResizeHandle.SOUTH_EAST,
                ResizeHandle.SOUTH,
                ResizeHandle.SOUTH_WEST,
                ResizeHandle.WEST
        };

        for (ResizeHandle handle : handles) {
            Rectangle hitRect = getHandleRect(handle, bounds, RESIZE_HANDLE_HIT_SIZE);
            if (hitRect != null && hitRect.contains(point)) {
                return handle;
            }
        }
        return ResizeHandle.NONE;
    }

    private Rectangle getHandleRect(ResizeHandle handle, Rectangle bounds, int size) {
        int half = size / 2;
        int left = bounds.x;
        int centerX = bounds.x + bounds.width / 2;
        int right = bounds.x + bounds.width;
        int top = bounds.y;
        int centerY = bounds.y + bounds.height / 2;
        int bottom = bounds.y + bounds.height;

        switch (handle) {
            case NORTH_WEST:
                return new Rectangle(left - half, top - half, size, size);
            case NORTH:
                return new Rectangle(centerX - half, top - half, size, size);
            case NORTH_EAST:
                return new Rectangle(right - half, top - half, size, size);
            case EAST:
                return new Rectangle(right - half, centerY - half, size, size);
            case SOUTH_EAST:
                return new Rectangle(right - half, bottom - half, size, size);
            case SOUTH:
                return new Rectangle(centerX - half, bottom - half, size, size);
            case SOUTH_WEST:
                return new Rectangle(left - half, bottom - half, size, size);
            case WEST:
                return new Rectangle(left - half, centerY - half, size, size);
            default:
                return null;
        }
    }

    private Rectangle calculateResizedBounds(Rectangle originalBounds, ResizeHandle handle, Point currentPoint) {
        int left = originalBounds.x;
        int top = originalBounds.y;
        int right = originalBounds.x + originalBounds.width;
        int bottom = originalBounds.y + originalBounds.height;

        switch (handle) {
            case NORTH_WEST:
                left = Math.min(currentPoint.x, right - MIN_RESIZE_DIMENSION);
                top = Math.min(currentPoint.y, bottom - MIN_RESIZE_DIMENSION);
                break;
            case NORTH:
                top = Math.min(currentPoint.y, bottom - MIN_RESIZE_DIMENSION);
                break;
            case NORTH_EAST:
                right = Math.max(currentPoint.x, left + MIN_RESIZE_DIMENSION);
                top = Math.min(currentPoint.y, bottom - MIN_RESIZE_DIMENSION);
                break;
            case EAST:
                right = Math.max(currentPoint.x, left + MIN_RESIZE_DIMENSION);
                break;
            case SOUTH_EAST:
                right = Math.max(currentPoint.x, left + MIN_RESIZE_DIMENSION);
                bottom = Math.max(currentPoint.y, top + MIN_RESIZE_DIMENSION);
                break;
            case SOUTH:
                bottom = Math.max(currentPoint.y, top + MIN_RESIZE_DIMENSION);
                break;
            case SOUTH_WEST:
                left = Math.min(currentPoint.x, right - MIN_RESIZE_DIMENSION);
                bottom = Math.max(currentPoint.y, top + MIN_RESIZE_DIMENSION);
                break;
            case WEST:
                left = Math.min(currentPoint.x, right - MIN_RESIZE_DIMENSION);
                break;
            default:
                break;
        }

        return new Rectangle(left, top, right - left, bottom - top);
    }

    private Cursor getCursorForHandle(ResizeHandle handle) {
        switch (handle) {
            case NORTH_WEST:
            case SOUTH_EAST:
                return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
            case NORTH_EAST:
            case SOUTH_WEST:
                return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
            case NORTH:
            case SOUTH:
                return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
            case EAST:
            case WEST:
                return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            default:
                return Cursor.getDefaultCursor();
        }
    }

    private void updateMoveCursor(Point point, JPanel panel) {
        if (point == null) {
            panel.setCursor(Cursor.getDefaultCursor());
            return;
        }

        if (activeResizeHandle != ResizeHandle.NONE) {
            panel.setCursor(getCursorForHandle(activeResizeHandle));
            return;
        }

        if (selectedElementForMove != null && selectedElementForMove.isResizable()) {
            Rectangle bounds = selectedElementForMove.getBounds();
            ResizeHandle hoverHandle = getResizeHandleAtPoint(point, bounds);
            if (hoverHandle != ResizeHandle.NONE) {
                panel.setCursor(getCursorForHandle(hoverHandle));
                return;
            }
        }

        PaintElement hoveredElement = findTopmostElementAt(point, host.getPaintElements());
        if (hoveredElement != null) {
            panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        } else {
            panel.setCursor(Cursor.getDefaultCursor());
        }
    }
}
