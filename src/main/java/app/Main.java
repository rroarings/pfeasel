package app;

import javax.imageio.ImageIO;
import javax.swing.*;

import actions.AddElementAction;
import actions.ChangeDisplayNameAction;
import actions.ClearAllAction;
import actions.DeleteElementAction;
import actions.ReorderLayerAction;
import actions.UndoableAction;
import paintcomponents.RectangleElement;
import paintcomponents.RoundRectangleElement;
import paintcomponents.CircleElement;
import paintcomponents.LineElement;
import paintcomponents.PolygonElement;
import paintcomponents.TextElement;
import paintcomponents.ImageElement;
import ui.GridManager;

import paintcomponents.PaintElement;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private JLabel statusLabel;
    private JLabel lastActionLabel; // New label for last action status
    private JLabel debugLogLabel;
    private boolean debugLogVisible = false;
    private DrawingPanel drawingPanel;
    private BufferedImage rsInterfaceImage;

    private ToolboxFrame toolboxFrame;

    private List<PaintElement> paintElements = new ArrayList<>();
    private Point dragStartPoint;
    private Point dragOffset; // For moving elements
    private PaintElement selectedElementForMove = null; // Element being moved
    private Point startPoint; // Added for line drawing
    private Point endPoint; // Added for line drawing
    private Rectangle currentDrawingRectangle;
    private List<Point> currentPolygonPoints; // Added for polygon drawing
    private boolean isDrawingPolygon = false; // Added for polygon drawing state

    // New state fields
    private boolean rsInterfaceVisible = true;
    private boolean snapToGridActive = false;
    private boolean antiAliasingActive = true; // Default to on

    // Undo/Redo stacks
    private Stack<UndoableAction> undoStack = new Stack<>();
    private Stack<UndoableAction> redoStack = new Stack<>();

    private GridManager gridManager;

    public Main() {
        setTitle("PFeasel Paint Creator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        gridManager = new GridManager(this::repaintDrawingPanel);

        loadRSInterfaceImage();
        drawingPanel = new DrawingPanel();
        add(drawingPanel, BorderLayout.CENTER);

        // Add left padding to statusLabel
        statusLabel = new JLabel("X: 0, Y: 0");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        // Add last action label
        lastActionLabel = new JLabel("Last action: ");
        lastActionLabel.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));

        // Add debug log label (restore to original far right, but keep invisible by default)
        debugLogLabel = new JLabel("");
        debugLogLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        debugLogLabel.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 12));
        debugLogLabel.setVisible(false);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(lastActionLabel, BorderLayout.CENTER);
        statusPanel.add(debugLogLabel, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);

        // Set custom app icon
        try {
            java.net.URL iconUrl = getClass().getResource("/img/ui/easel-logo.jpeg");
            if (iconUrl != null) {
                setIconImage(new ImageIcon(iconUrl).getImage());
            }
        } catch (Exception e) {
            logger.error("Failed to set app icon: " + e.getMessage());
        }

        pack();
        setLocationRelativeTo(null);
    }

    // Helper for status updates (if setLastActionStatus is not present)
    private void setStatus(String status) {
        if (statusLabel != null) statusLabel.setText(status);
    }

    public void setLastActionStatus(String status) {
        if (lastActionLabel != null) lastActionLabel.setText("Last action: " + status);
    }

    public void setDebugLogVisible(boolean visible) {
        this.debugLogVisible = visible;
        debugLogLabel.setVisible(visible);
    }
    public void setDebugLogText(String text) {
        if (this.debugLogVisible) debugLogLabel.setText(text);
    }

    public boolean isDebugLogVisible() {
        return debugLogVisible;
    }

    // --- BEGIN: File and Image Handling Implementation ---
    private File currentSaveFile = null;

    public void setCurrentSaveFile(File file) {
        this.currentSaveFile = file;
    }

    public void handleSave() {
        if (currentSaveFile == null) {
            handleSaveAs();
            return;
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new java.io.FileOutputStream(currentSaveFile))) {
            oos.writeObject(paintElements);
            setStatus("Saved to " + currentSaveFile.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            setStatus("Save failed");
        }
    }

    public void handleSaveAs() {
        JFileChooser chooser = new JFileChooser();
        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter("Paint Files (*.pfd)", "pfd");
        chooser.setFileFilter(filter);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pfd")) {
                file = new File(file.getParentFile(), file.getName() + ".pfd");
            }
            setCurrentSaveFile(file);
            handleSave();
        } else {
            setStatus("Save As cancelled");
        }
    }

    public void handleOpen() {
        JFileChooser chooser = new JFileChooser();
        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter("Paint Files (*.pfd)", "pfd");
        chooser.setFileFilter(filter);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (ObjectInputStream ois = new ObjectInputStream(new java.io.FileInputStream(file))) {
                Object obj = ois.readObject();
                if (obj instanceof List) {
                    List<?> loaded = (List<?>) obj;
                    List<PaintElement> loadedElements = new ArrayList<>();
                    for (Object o : loaded) {
                        if (o instanceof PaintElement) loadedElements.add((PaintElement) o);
                    }
                    paintElements.clear();
                    paintElements.addAll(loadedElements);
                    setCurrentSaveFile(file);
                    updateToolboxLayerList();
                    drawingPanel.repaint();
                    setStatus("Opened " + file.getName());
                } else {
                    throw new Exception("File does not contain a valid drawing.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to open: " + ex.getMessage(), "Open Error", JOptionPane.ERROR_MESSAGE);
                setStatus("Open failed");
            }
        } else {
            setStatus("Open cancelled");
        }
    }

    public void handleImageUrlInput() {
        String url = JOptionPane.showInputDialog(this, "Enter image URL:", "Add Image from URL", JOptionPane.PLAIN_MESSAGE);
        if (url == null || url.trim().isEmpty()) {
            setStatus("Image URL input cancelled");
            return;
        }
        try {
            URL imageUrl = new java.io.File(new java.net.URI(url)).toURI().toURL();
            BufferedImage img = javax.imageio.ImageIO.read(imageUrl);
            if (img == null) throw new Exception("Could not load image from URL.");
            ImageElement element = new ImageElement(img, new Point(50, 50), url, "Image");
            paintElements.add(0, element);
            updateToolboxLayerList();
            drawingPanel.repaint();
            setStatus("Image added from URL");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load image: " + ex.getMessage(), "Image Error", JOptionPane.ERROR_MESSAGE);
            setStatus("Image URL failed");
        }
    }

    public void handleLocalImageInput() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                BufferedImage img = javax.imageio.ImageIO.read(file);
                if (img == null) throw new Exception("Could not load image file.");
                ImageElement element = new ImageElement(img, new Point(50, 50), file.getAbsolutePath(), "Image");
                paintElements.add(0, element);
                updateToolboxLayerList();
                drawingPanel.repaint();
                setStatus("Image added from file");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to load image: " + ex.getMessage(), "Image Error", JOptionPane.ERROR_MESSAGE);
                setStatus("Image file failed");
            }
        } else {
            setStatus("Image file input cancelled");
        }
    }
    // --- END: File and Image Handling Implementation ---

    public void setToolboxFrame(ToolboxFrame toolboxFrame) {
        this.toolboxFrame = toolboxFrame;
        if (this.toolboxFrame != null) {
            updateCursorForTool(this.toolboxFrame.getSelectedTool());
        }
    }

    public void repaintDrawingPanel() {
        if (drawingPanel != null) {
            drawingPanel.repaint();
        }
    }

    private void loadRSInterfaceImage() {
        try {
            // Load from classpath (works in JAR and IDE)
            URL imgUrl = getClass().getResource("/img/ui/rs-interface.png");
            if (imgUrl != null) {
                rsInterfaceImage = ImageIO.read(imgUrl);
            } else {
                logger.error("Could not find RS interface image at /img/ui/rs-interface.png");
                rsInterfaceImage = null;
            }
        } catch (IOException e) {
            logger.error("Failed to load RS interface image: " + e.getMessage());
            rsInterfaceImage = null;
        }
    }

    public void updateCursorForTool(ToolboxFrame.ToolType toolType) {
        if (drawingPanel == null) return;
        switch (toolType) {
            case RECTANGLE:
            case ROUND_RECTANGLE:
            case CIRCLE:
            case LINE:
            case POLYGON:
            case TEXT:
            case IMAGE_URL:
            case IMAGE_LOCAL:
                drawingPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                break;
            case MOVE:
                drawingPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                break;
            default:
                drawingPanel.setCursor(Cursor.getDefaultCursor());
                break;
        }
    }

    public void addUndoableAction(UndoableAction action) {
        undoStack.push(action);
        redoStack.clear(); // Clear redo stack whenever a new action is performed
        updateUndoRedoMenuItems();
        setLastActionStatus(action.getActionName());
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            UndoableAction action = undoStack.pop();
            action.undo();
            redoStack.push(action);
            updateUndoRedoMenuItems();
            updateToolboxLayerList(); // Refresh layer list as undo might change it
            drawingPanel.repaint();
            setLastActionStatus("Undo: " + action.getActionName());
            logger.info("Performed UNDO");
        } else {
            setLastActionStatus("Undo: nothing to undo");
            logger.info("Undo stack is empty.");
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            UndoableAction action = redoStack.pop();
            action.redo();
            undoStack.push(action);
            updateUndoRedoMenuItems();
            updateToolboxLayerList(); // Refresh layer list as redo might change it
            drawingPanel.repaint();
            setLastActionStatus("Redo: " + action.getActionName());
            logger.info("Performed REDO");
        } else {
            setLastActionStatus("Redo: nothing to redo");
            logger.info("Redo stack is empty.");
        }
    }

    private void updateUndoRedoMenuItems() {
        if (toolboxFrame != null) {
            toolboxFrame.setUndoEnabled(!undoStack.isEmpty());
            toolboxFrame.setRedoEnabled(!redoStack.isEmpty());
        }
    }

    public void updateToolboxLayerList() {
        if (toolboxFrame != null) {
            List<String> layerNames = paintElements.stream()
                                                 .map(PaintElement::getDisplayName)
                                                 .collect(Collectors.toList());
            Collections.reverse(layerNames); // To display top layer at the top of the list
            toolboxFrame.updateLayersList(layerNames);
        }
    }

    public void internalAddElementToList(PaintElement element, int index) {
        logger.debug("internalAddElementToList called with element={}, index={}", element, index);
        if (index < 0 || index > paintElements.size()) {
            paintElements.add(0, element);
            logger.error("internalAddElementToList: Invalid index {}, adding to top (0).", index);
        } else {
            paintElements.add(index, element);
        }
        updateToolboxLayerList();
        if (index == 0) {
            if (toolboxFrame != null) toolboxFrame.selectLayerInList(0);
        }
        drawingPanel.repaint();
    }

    public void internalRemoveElementFromList(PaintElement element) {
        logger.debug("internalRemoveElementFromList called with element={}", element);
        paintElements.remove(element);
        updateToolboxLayerList();
        drawingPanel.repaint();
    }

    public void internalRemoveElementFromList(int index) {
        logger.debug("internalRemoveElementFromList called with index={}", index);
        if (index >= 0 && index < paintElements.size()) {
            paintElements.remove(index);
            updateToolboxLayerList();
            drawingPanel.repaint();
        } else {
            logger.error("internalRemoveElementFromList: Invalid index {}", index);
        }
    }

    public void internalRestoreElementsList(List<PaintElement> elementsToRestore) {
        logger.debug("internalRestoreElementsList called with {} elements", elementsToRestore != null ? elementsToRestore.size() : 0);
        paintElements.clear();
        paintElements.addAll(elementsToRestore);
        updateToolboxLayerList();
        drawingPanel.repaint();
        if (toolboxFrame != null && !paintElements.isEmpty()) {
            toolboxFrame.selectLayerInList(0);
        }
    }

    public void addPaintElement(PaintElement element, boolean addToUndoStack) {
        logger.debug("addPaintElement called with element={}, addToUndoStack={}", element, addToUndoStack);
        if (addToUndoStack) {
            UndoableAction action = new AddElementAction(this, element, 0); // Pass index 0 as required by constructor
            internalAddElementToList(element, 0);
            addUndoableAction(action);
            logger.info("Added element '{}'" + " at [" + element.getPosition().x + ", " + element.getPosition().y + "]" + " with undo support.", element.getDisplayName());
        } else {
            internalAddElementToList(element, 0);
            logger.info("Added element '{}'" + " at [" + element.getPosition().getX() + ", " + element.getPosition().getY() + "]" + " without undo support.", element.getDisplayName());
        }
    }

    public void addPaintElement(PaintElement element) {
        logger.debug("addPaintElement(element) called");
        addPaintElement(element, true);
    }

    public void deletePaintElement(int listIndexInToolbox) {
        logger.debug("deletePaintElement called with index={}", listIndexInToolbox);
        if (listIndexInToolbox >= 0 && listIndexInToolbox < paintElements.size()) {
            PaintElement removedElement = paintElements.get(listIndexInToolbox);
            UndoableAction action = new DeleteElementAction(this, removedElement, listIndexInToolbox);
            internalRemoveElementFromList(listIndexInToolbox);
            addUndoableAction(action);
            logger.info("Deleted element '{}' at index {}.", removedElement.getDisplayName(), listIndexInToolbox);
            setLastActionStatus("Deleted '" + removedElement.getDisplayName() + "'");
        } else {
            logger.error("deletePaintElement: Invalid index {}", listIndexInToolbox);
        }
    }

    public void duplicatePaintElement(int selectedIndexInListModel) {
        logger.debug("duplicatePaintElement called with selectedIndexInListModel={}", selectedIndexInListModel);
        if (toolboxFrame == null || selectedIndexInListModel < 0 || selectedIndexInListModel >= toolboxFrame.getLayersListModel().getSize()) {
            logger.error("Error duplicating element: Invalid selected index {}", selectedIndexInListModel);
            JOptionPane.showMessageDialog(this, "Please select a layer to duplicate.", "No Layer Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int actualPaintElementIndex = paintElements.size() - 1 - selectedIndexInListModel;
        if (actualPaintElementIndex < 0 || actualPaintElementIndex >= paintElements.size()) {
            logger.error("Error duplicating element: Calculated paintElements index {} is out of bounds. List size: {}, JList index: {}", actualPaintElementIndex, paintElements.size(), selectedIndexInListModel);
            JOptionPane.showMessageDialog(this, "Error finding the selected layer. Please try again.", "Duplication Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        PaintElement originalElement = paintElements.get(actualPaintElementIndex);
        if (originalElement == null) {
            logger.error("Error duplicating element: Original element at index {} is null.", actualPaintElementIndex);
            JOptionPane.showMessageDialog(this, "Cannot duplicate a null element.", "Duplication Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        PaintElement duplicatedElement = originalElement.duplicate();
        if (duplicatedElement == null) {
            logger.error("Error duplicating element: duplicate() method returned null for {}", originalElement.getName());
            JOptionPane.showMessageDialog(this, "Failed to duplicate the element. Image could not be reloaded or other error.", "Duplication Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Point originalPosition = duplicatedElement.getPosition();
        int offsetX = 10;
        int offsetY = 10;
        duplicatedElement.setPosition(originalPosition.x + offsetX, originalPosition.y + offsetY);
        String uniqueDisplayName = generateUniqueDisplayName(duplicatedElement.getName());
        duplicatedElement.setDisplayName(uniqueDisplayName);
        paintElements.add(0, duplicatedElement);
        if (toolboxFrame != null) {
            toolboxFrame.addLayerToList(uniqueDisplayName, 0);
            toolboxFrame.selectLayerInList(0);
        }
        repaintDrawingPanel();
        setLastActionStatus("Duplicated '" + uniqueDisplayName + "'");
        logger.info("Element '{}' duplicated as '{}' at offset ({}, {})", originalElement.getDisplayName(), uniqueDisplayName, offsetX, offsetY);
    }

    public void updatePaintElementDisplayName(int listIndexInToolbox, String newDisplayName) {
        logger.debug("updatePaintElementDisplayName called with index={}, newDisplayName={}", listIndexInToolbox, newDisplayName);
        if (listIndexInToolbox >= 0 && listIndexInToolbox < paintElements.size()) {
            PaintElement element = paintElements.get(listIndexInToolbox);
            String oldDisplayName = element.getDisplayName();
            if (!oldDisplayName.equals(newDisplayName)) {
                UndoableAction action = new ChangeDisplayNameAction(this, element, oldDisplayName, newDisplayName, listIndexInToolbox);
                element.setDisplayName(newDisplayName);
                addUndoableAction(action);
                updateToolboxLayerList();
                drawingPanel.repaint();
                logger.info("Updated display name for element at index {} from '{}' to '{}'", listIndexInToolbox, oldDisplayName, newDisplayName);
                setLastActionStatus("Renamed to '" + newDisplayName + "'");
            }
        } else {
            logger.error("updatePaintElementDisplayName: Invalid index {}", listIndexInToolbox);
        }
    }

    // Add this method to support ChangeDisplayNameAction
    public void internalSetPaintElementDisplayName(PaintElement element, String displayName, int index) {
        if (element != null) {
            element.setDisplayName(displayName);
            // Optionally, update the toolbox list if needed
            updateToolboxLayerList();
            drawingPanel.repaint();
        }
    }

    public void moveLayerUp(int listIndexInToolbox) {
        if (listIndexInToolbox > 0 && listIndexInToolbox < paintElements.size()) {
            PaintElement element = paintElements.get(listIndexInToolbox);

            paintElements.remove(listIndexInToolbox);
            paintElements.add(listIndexInToolbox - 1, element);

            UndoableAction action = new ReorderLayerAction(this, element, listIndexInToolbox - 1, listIndexInToolbox);
            addUndoableAction(action);

            updateToolboxLayerList();
            drawingPanel.repaint();
            logger.info("Main: Moved element '" + element.getDisplayName() + "' from paintElements index " + listIndexInToolbox + " to " + (listIndexInToolbox - 1));
            setLastActionStatus("Reordered layer: '" + element.getDisplayName() + "'");
        } else {
            logger.error("Main.moveLayerUp: Invalid index: " + listIndexInToolbox);
        }
    }

    public void moveLayerDown(int listIndexInToolbox) {
        if (listIndexInToolbox >= 0 && listIndexInToolbox < paintElements.size() - 1) {
            PaintElement element = paintElements.get(listIndexInToolbox);

            paintElements.remove(listIndexInToolbox);
            paintElements.add(listIndexInToolbox + 1, element);

            UndoableAction action = new ReorderLayerAction(this, element, listIndexInToolbox + 1, listIndexInToolbox);
            addUndoableAction(action);

            updateToolboxLayerList();
            drawingPanel.repaint();
            logger.info("Main: Moved element '" + element.getDisplayName() + "' from paintElements index " + listIndexInToolbox + " to " + (listIndexInToolbox + 1));
            setLastActionStatus("Reordered layer: '" + element.getDisplayName() + "'");
        } else {
            logger.error("Main.moveLayerDown: Invalid index: " + listIndexInToolbox);
        }
    }

    public void clearPaintElements() {
        if (!paintElements.isEmpty()) {
            List<PaintElement> elementsCleared = new ArrayList<>(paintElements);
            UndoableAction action = new ClearAllAction(this, elementsCleared);

            paintElements.clear();
            addUndoableAction(action);

            updateToolboxLayerList();
            drawingPanel.repaint();
            logger.info("Main: Cleared all paint elements.");
            setLastActionStatus("Cleared all paint elements");
        } else {
            logger.info("Main: No paint elements to clear.");
        }
    }

    // Add this method to support ClearAllAction
    public void internalClearAllElements() {
        paintElements.clear();
        updateToolboxLayerList();
        drawingPanel.repaint();
    }

    public void setDrawRSInterface(boolean visible) {
        this.rsInterfaceVisible = visible;
        repaintDrawingPanel();
        logger.info("Draw RS Interface set to: " + visible);
    }

    public boolean isDrawRSInterfaceVisible() {
        return this.rsInterfaceVisible;
    }

    public void handleChangeBackgroundColor() {
        if (drawingPanel == null) return;
        Color newBgColor = JColorChooser.showDialog(this, "Choose Background Color", drawingPanel.getBackground());
        if (newBgColor != null) {
            drawingPanel.setBackground(newBgColor);
            repaintDrawingPanel();
            logger.info("Background color changed to: " + newBgColor);
        }
    }

    public boolean isGridVisible() {
        return gridManager != null && gridManager.isGridVisible();
    }

    public void setGridVisible(boolean visible) {
        if (gridManager != null) {
            gridManager.setGridVisible(visible);
            repaintDrawingPanel();
            logger.info("Grid visibility set to: " + visible);
        }
    }

    public void showGridOptionsDialog() {
        if (gridManager != null) {
            gridManager.showGridOptionsDialog(this);
        }
    }

    public void resetGridToDefaults() {
        if (gridManager != null) {
            gridManager.resetGridToDefaults();
            repaintDrawingPanel();
            logger.info("Grid reset to defaults.");
        }
    }

    public boolean isSnapToGridActive() {
        return this.snapToGridActive;
    }

    public void setSnapToGrid(boolean active) {
        this.snapToGridActive = active;
        logger.info("Snap to Grid set to: " + active);
    }

    public boolean isAntiAliasingActive() {
        return this.antiAliasingActive;
    }

    public void setAntiAliasing(boolean active) {
        this.antiAliasingActive = active;
        repaintDrawingPanel();
        logger.info("Anti-aliasing set to: " + active);
    }

    private String generateUniqueDisplayName(String baseName) {
        int count = 1;
        String currentName = baseName + " " + count;
        boolean nameExists;

        do {
            nameExists = false;
            for (PaintElement pe : paintElements) {
                if (pe.getDisplayName() != null && pe.getDisplayName().equals(currentName)) {
                    nameExists = true;
                    break;
                }
            }
            if (nameExists) {
                count++;
                currentName = baseName + " " + count;
            }
        } while (nameExists);
        return currentName;
    }

    public void handleGenerateCode() {
        // Use the new CodeGenerator class for code generation
        CodeGenerator generator = new CodeGenerator();
        String codeText = generator.generateCode(paintElements);
        String importsText = generator.getImportsText();
        GeneratedCodeDialog dialog = new GeneratedCodeDialog(this, "Generated Code", true, importsText, codeText);
        dialog.setVisible(true);
    }

    class DrawingPanel extends JPanel {

        private int snapToGridValue(int value, int gridSize) {
            if (snapToGridActive && gridSize > 0) {
                return (int) (Math.round((double) value / gridSize) * gridSize);
            }
            return value;
        }

        private Point snapPointToGrid(Point p) {
            if (snapToGridActive && gridManager.isGridVisible()) {
                int snappedX = snapToGridValue(p.x, gridManager.getGridWidth());
                int snappedY = snapToGridValue(p.y, gridManager.getGridHeight());
                return new Point(snappedX, snappedY);
            }
            return p;
        }

        public DrawingPanel() {
            setPreferredSize(new Dimension(765, 503));
            setBackground(Color.LIGHT_GRAY);
            setFocusable(true);
            currentPolygonPoints = new ArrayList<>();

            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    statusLabel.setText("X: " + e.getX() + ", Y: " + e.getY());
                    ToolboxFrame.ToolType selectedTool = (toolboxFrame != null) ? toolboxFrame.getSelectedTool() : null;
                    if (selectedTool == ToolboxFrame.ToolType.POLYGON) {
                        Point currentMousePoint = e.getPoint();
                        if (snapToGridActive && gridManager.isGridVisible()) {
                            endPoint = snapPointToGrid(currentMousePoint);
                        } else {
                            endPoint = currentMousePoint;
                        }
                        repaint();
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    statusLabel.setText("X: " + e.getX() + ", Y: " + e.getY());
                    ToolboxFrame.ToolType selectedTool = (toolboxFrame != null) ? toolboxFrame.getSelectedTool() : null;
                    Point currentMousePoint = e.getPoint();

                    if (snapToGridActive && gridManager.isGridVisible()) {
                        endPoint = snapPointToGrid(currentMousePoint);
                    } else {
                        endPoint = currentMousePoint;
                    }

                    if (selectedTool == ToolboxFrame.ToolType.MOVE && selectedElementForMove != null) {
                        int newX = currentMousePoint.x - dragOffset.x;
                        int newY = currentMousePoint.y - dragOffset.y;

                        if (snapToGridActive && gridManager.isGridVisible()) {
                            Point snappedPosition = snapPointToGrid(new Point(newX, newY));
                            selectedElementForMove.setPosition(snappedPosition.x, snappedPosition.y);
                        } else {
                            selectedElementForMove.setPosition(newX, newY);
                        }
                        repaint();
                    } else if (selectedTool == ToolboxFrame.ToolType.LINE) {
                        repaint();
                    } else if ((selectedTool == ToolboxFrame.ToolType.RECTANGLE ||
                                selectedTool == ToolboxFrame.ToolType.ROUND_RECTANGLE ||
                                selectedTool == ToolboxFrame.ToolType.CIRCLE) && dragStartPoint != null) {
                        
                        Point effectiveDragStartPoint = dragStartPoint;
                        Point effectiveEndPoint = endPoint;

                        int x = Math.min(effectiveDragStartPoint.x, effectiveEndPoint.x);
                        int y = Math.min(effectiveDragStartPoint.y, effectiveEndPoint.y);
                        int width = Math.abs(effectiveDragStartPoint.x - effectiveEndPoint.x);
                        int height = Math.abs(effectiveDragStartPoint.y - effectiveEndPoint.y);
                        currentDrawingRectangle = new Rectangle(x, y, width, height);
                        repaint();
                    }
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    ToolboxFrame.ToolType selectedTool = (toolboxFrame != null) ? toolboxFrame.getSelectedTool() : null;
                    Point currentPoint = e.getPoint();

                    if (selectedTool == ToolboxFrame.ToolType.MOVE) {
                        selectedElementForMove = null;
                        for (int i = 0; i < paintElements.size(); i++) {
                            PaintElement element = paintElements.get(i);
                            if (element.contains(currentPoint)) {
                                selectedElementForMove = element;
                                Point elementPos = selectedElementForMove.getPosition();
                                dragOffset = new Point(currentPoint.x - elementPos.x, currentPoint.y - elementPos.y);
                                // Select the corresponding layer in the layers list
                                if (toolboxFrame != null) {
                                    int layerIndex = paintElements.size() - 1 - i;
                                    toolboxFrame.selectLayerInList(layerIndex);
                                }
                                repaint();
                                break;
                            }
                        }
                    } else {
                        selectedElementForMove = null;
                        if (snapToGridActive && gridManager.isGridVisible()) {
                            startPoint = snapPointToGrid(currentPoint);
                        } else {
                            startPoint = currentPoint;
                        }
                        currentDrawingRectangle = null;
                        if (selectedTool == ToolboxFrame.ToolType.LINE) {
                            endPoint = startPoint;
                        } else if (selectedTool == ToolboxFrame.ToolType.RECTANGLE ||
                                   selectedTool == ToolboxFrame.ToolType.ROUND_RECTANGLE ||
                                   selectedTool == ToolboxFrame.ToolType.CIRCLE) {
                            dragStartPoint = startPoint;
                            endPoint = startPoint;
                        }
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    ToolboxFrame.ToolType selectedTool = (toolboxFrame != null) ? toolboxFrame.getSelectedTool() : null;
                    
                    if (selectedTool == ToolboxFrame.ToolType.MOVE && selectedElementForMove != null) {
                        Point finalMousePosition = e.getPoint();
                        int newX = finalMousePosition.x - dragOffset.x;
                        int newY = finalMousePosition.y - dragOffset.y;
                        Point finalElementPos = new Point(newX, newY);

                        if (snapToGridActive && gridManager.isGridVisible()) {
                            finalElementPos = snapPointToGrid(finalElementPos);
                        }
                        selectedElementForMove.setPosition(finalElementPos.x, finalElementPos.y);
                        
                        int selectedIndexInPaintElements = paintElements.indexOf(selectedElementForMove);
                        if (selectedIndexInPaintElements == 0 && toolboxFrame != null) {
                            selectedElementForMove = null;
                            dragOffset = null;
                            repaint();
                            return;
                        }
                    }
                    
                    Point originalEndPoint = e.getPoint();
                    
                    if (snapToGridActive && gridManager.isGridVisible()) {
                        endPoint = snapPointToGrid(originalEndPoint);
                    } else {
                        endPoint = originalEndPoint;
                    }

                    if (startPoint == null) return;

                    PaintElement newElement = null;
                    String uniqueDisplayName = "";

                    if ((selectedTool == ToolboxFrame.ToolType.RECTANGLE || 
                         selectedTool == ToolboxFrame.ToolType.ROUND_RECTANGLE || 
                         selectedTool == ToolboxFrame.ToolType.CIRCLE) && dragStartPoint != null && toolboxFrame != null) {
                        
                        Point effectiveDragStartPoint = dragStartPoint;
                        Point effectiveEndPoint = endPoint;

                        int x = Math.min(effectiveDragStartPoint.x, effectiveEndPoint.x);
                        int y = Math.min(effectiveDragStartPoint.y, effectiveEndPoint.y);
                        int width = Math.abs(effectiveDragStartPoint.x - effectiveEndPoint.x);
                        int height = Math.abs(effectiveDragStartPoint.y - effectiveEndPoint.y);
                        currentDrawingRectangle = new Rectangle(x, y, width, height);
                    }

                    switch (selectedTool) {
                        case RECTANGLE:
                            if (currentDrawingRectangle == null || currentDrawingRectangle.width == 0 || currentDrawingRectangle.height == 0 || toolboxFrame == null) break;
                            newElement = new RectangleElement(
                                    currentDrawingRectangle.x, currentDrawingRectangle.y,
                                    currentDrawingRectangle.width, currentDrawingRectangle.height,
                                    toolboxFrame.isFillEnabled() ? toolboxFrame.getFillColor() : null,
                                    toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : null,
                                    (float) toolboxFrame.getCurrentStrokeWidth(),
                                    toolboxFrame.isFillEnabled(),
                                    toolboxFrame.isStrokeEnabled());
                            break;
                        case ROUND_RECTANGLE:
                            if (currentDrawingRectangle == null || currentDrawingRectangle.width == 0 || currentDrawingRectangle.height == 0 || toolboxFrame == null) break;
                            newElement = new RoundRectangleElement(
                                    currentDrawingRectangle.x, currentDrawingRectangle.y,
                                    currentDrawingRectangle.width, currentDrawingRectangle.height,
                                    toolboxFrame.getArcWidth(), toolboxFrame.getArcHeight(),
                                    toolboxFrame.isFillEnabled() ? toolboxFrame.getFillColor() : null,
                                    toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : null,
                                    (float) toolboxFrame.getCurrentStrokeWidth(),
                                    toolboxFrame.isFillEnabled(),
                                    toolboxFrame.isStrokeEnabled());
                            break;
                        case CIRCLE:
                            if (currentDrawingRectangle == null || currentDrawingRectangle.width == 0 || currentDrawingRectangle.height == 0 || toolboxFrame == null) break;
                            newElement = new CircleElement(
                                    currentDrawingRectangle.x, currentDrawingRectangle.y,
                                    currentDrawingRectangle.width, currentDrawingRectangle.height,
                                    toolboxFrame.isFillEnabled() ? toolboxFrame.getFillColor() : null,
                                    toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : null,
                                    (float) toolboxFrame.getCurrentStrokeWidth(),
                                    toolboxFrame.isFillEnabled(),
                                    toolboxFrame.isStrokeEnabled());
                            break;
                        case LINE:
                            if (toolboxFrame == null) break;
                            Color strokeColor = toolboxFrame.getStrokeColor();
                            float strokeWidth = (float) toolboxFrame.getCurrentStrokeWidth();
                            if (strokeColor != null && strokeWidth > 0) {
                                newElement = new LineElement(startPoint.x, startPoint.y, endPoint.x, endPoint.y, strokeColor, strokeWidth);
                            }
                            break;
                        default:
                            break;
                    }

                    if (newElement != null) {
                        if (toolboxFrame != null) {
                            newElement.setShadow(toolboxFrame.isShadowEnabled());
                            uniqueDisplayName = generateUniqueDisplayName(newElement.getName());
                            newElement.setDisplayName(uniqueDisplayName);
                            addPaintElement(newElement);
                            // Show element name and coordinates with a space after the comma
                            if (currentDrawingRectangle != null && (selectedTool == ToolboxFrame.ToolType.RECTANGLE || selectedTool == ToolboxFrame.ToolType.ROUND_RECTANGLE || selectedTool == ToolboxFrame.ToolType.CIRCLE)) {
                                setLastActionStatus("Drew " + uniqueDisplayName + " at " + currentDrawingRectangle.x + ", " + currentDrawingRectangle.y);
                            } else if (selectedTool == ToolboxFrame.ToolType.LINE && startPoint != null && endPoint != null) {
                                setLastActionStatus("Drew line from " + startPoint.x + ", " + startPoint.y + " to " + endPoint.x + ", " + endPoint.y);
                            } else {
                                setLastActionStatus("Drew " + uniqueDisplayName);
                            }
                        }
                    }

                    startPoint = null;
                    dragStartPoint = null;
                    currentDrawingRectangle = null;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    logger.info("[DEBUG] mouseClicked entered. Tool: " + ((toolboxFrame != null) ? toolboxFrame.getSelectedTool() : "N/A") + ", Click at: " + e.getPoint());
                    ToolboxFrame.ToolType selectedTool = (toolboxFrame != null) ? toolboxFrame.getSelectedTool() : null;
                    Point clickedPoint = e.getPoint();

                    if (snapToGridActive && gridManager.isGridVisible()) {
                        clickedPoint = snapPointToGrid(clickedPoint);
                    }

                    if (isDrawingPolygon && selectedTool != ToolboxFrame.ToolType.POLYGON) {
                        if (currentPolygonPoints.size() >= 3) {
                            logger.info("Polygon finalized due to tool change.");
                            finalizePolygon();
                        } else {
                            logger.info("Polygon drawing cancelled due to tool change (not enough points).");
                            isDrawingPolygon = false;
                            currentPolygonPoints.clear();
                            repaint();
                        }
                    }

                    if (selectedTool == ToolboxFrame.ToolType.TEXT) {
                        if (toolboxFrame == null) return;
                        String text = toolboxFrame.getTextInput();
                        if (text.isEmpty()) {
                            JOptionPane.showMessageDialog(Main.this, "Please enter text in the toolbox first.", "Text Input Empty", JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }
                        Font font = toolboxFrame.getSelectedFont();
                        Color color = toolboxFrame.getFillColor();
                        if (!toolboxFrame.isFillEnabled()) {
                            color = toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : Color.BLACK;
                        }

                        TextElement textElement = new TextElement(text, clickedPoint.x, clickedPoint.y, font, color);
                        textElement.setShadow(toolboxFrame.isShadowEnabled());
                        String uniqueDisplayName = generateUniqueDisplayName(textElement.getName());
                        textElement.setDisplayName(uniqueDisplayName);
                        
                        addPaintElement(textElement);
                        repaint();

                    } else if (selectedTool == ToolboxFrame.ToolType.POLYGON) {
                        isDrawingPolygon = true;

                        if (!currentPolygonPoints.isEmpty() && currentPolygonPoints.size() >= 2) { 
                            Point firstPoint = currentPolygonPoints.get(0);
                            final int CLOSING_TOLERANCE = 8;
                            if (clickedPoint.distance(firstPoint) <= CLOSING_TOLERANCE) {
                                logger.info("Polygon closed by clicking first point.");
                                finalizePolygon();
                                return;
                            }
                        }

                        currentPolygonPoints.add(clickedPoint);
                        logger.info("Added polygon point: " + clickedPoint + ", total points: " + currentPolygonPoints.size());
                        
                        if (e.getClickCount() == 2 && currentPolygonPoints.size() >= 3) {
                            logger.info("Polygon finalized by double-click.");
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
                if (toolboxFrame == null) return;
                Color fillColor = toolboxFrame.isFillEnabled() ? toolboxFrame.getFillColor() : null;
                Color strokeColor = toolboxFrame.isStrokeEnabled() ? toolboxFrame.getStrokeColor() : null;
                float strokeWidth = (float) toolboxFrame.getCurrentStrokeWidth();

                PolygonElement newPolygon = new PolygonElement(currentPolygonPoints, 
                                                               fillColor, strokeColor, strokeWidth, 
                                                               toolboxFrame.isFillEnabled(), toolboxFrame.isStrokeEnabled());
                newPolygon.setShadow(toolboxFrame.isShadowEnabled());
                String uniqueDisplayName = generateUniqueDisplayName(newPolygon.getName());
                newPolygon.setDisplayName(uniqueDisplayName);
                addPaintElement(newPolygon);
                setLastActionStatus("Drew polygon '" + uniqueDisplayName + "' with " + currentPolygonPoints.size() + " points");
                logger.info("Polygon finalized with " + currentPolygonPoints.size() + " points. Name: " + uniqueDisplayName);
            }
            currentPolygonPoints.clear();
            isDrawingPolygon = false;
            startPoint = null;
            endPoint = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            if (antiAliasingActive) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            } else {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            }

            if (rsInterfaceVisible && rsInterfaceImage != null) {
                g2d.drawImage(rsInterfaceImage, 0, 0, this);
            }

            if (gridManager.isGridVisible()) {
                gridManager.drawGrid(g2d, getWidth(), getHeight());
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

            // Draw selection bounding box for MOVE tool
            if (toolboxFrame != null && toolboxFrame.getSelectedTool() == ToolboxFrame.ToolType.MOVE && selectedElementForMove != null) {
                Rectangle bounds = selectedElementForMove.getBounds();
                if (bounds != null) {
                    g2d.setColor(Color.BLUE);
                    float[] dash = {4f, 4f};
                    Stroke oldStroke = g2d.getStroke();
                    g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                    g2d.drawRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4);
                    g2d.setStroke(oldStroke);
                }
            }
            // Draw bounding box for preview when drawing new elements
            if (startPoint != null && endPoint != null) {
                ToolboxFrame.ToolType previewTool = (toolboxFrame != null) ? toolboxFrame.getSelectedTool() : null;
                if (previewTool == null || toolboxFrame == null) return;
                if ((previewTool == ToolboxFrame.ToolType.RECTANGLE ||
                     previewTool == ToolboxFrame.ToolType.ROUND_RECTANGLE ||
                     previewTool == ToolboxFrame.ToolType.CIRCLE ||
                     previewTool == ToolboxFrame.ToolType.LINE) && currentDrawingRectangle != null) {
                    g2d.setColor(new Color(0, 120, 255, 128)); // semi-transparent blue
                    float[] dash = {4f, 4f};
                    Stroke oldStroke = g2d.getStroke();
                    g2d.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                    g2d.drawRect(currentDrawingRectangle.x - 2, currentDrawingRectangle.y - 2, currentDrawingRectangle.width + 4, currentDrawingRectangle.height + 4);
                    g2d.setStroke(oldStroke);
                }
            }

            if (startPoint != null && endPoint != null) {
                ToolboxFrame.ToolType previewTool = (toolboxFrame != null) ? toolboxFrame.getSelectedTool() : null;
                if (previewTool == null || toolboxFrame == null) return;

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
                                g2d.fillRect(currentDrawingRectangle.x, currentDrawingRectangle.y, currentDrawingRectangle.width, currentDrawingRectangle.height);
                                break;
                            case ROUND_RECTANGLE:
                                g2d.fillRoundRect(currentDrawingRectangle.x, currentDrawingRectangle.y, currentDrawingRectangle.width, currentDrawingRectangle.height, toolboxFrame.getArcWidth(), toolboxFrame.getArcHeight());
                                break;
                            case CIRCLE:
                                g2d.fillOval(currentDrawingRectangle.x, currentDrawingRectangle.y, currentDrawingRectangle.width, currentDrawingRectangle.height);
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
                                g2d.drawRect(currentDrawingRectangle.x, currentDrawingRectangle.y, currentDrawingRectangle.width, currentDrawingRectangle.height);
                                break;
                            case ROUND_RECTANGLE:
                                g2d.drawRoundRect(currentDrawingRectangle.x, currentDrawingRectangle.y, currentDrawingRectangle.width, currentDrawingRectangle.height, toolboxFrame.getArcWidth(), toolboxFrame.getArcHeight());
                                break;
                            case CIRCLE:
                                g2d.drawOval(currentDrawingRectangle.x, currentDrawingRectangle.y, currentDrawingRectangle.width, currentDrawingRectangle.height);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
            
            if (isDrawingPolygon && !currentPolygonPoints.isEmpty()) {
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
        }
    }

    public static void main(String[] args) {
        // Set FlatLaf theme before any Swing UI is created
        ToolboxFrame.setupInitialTheme();
        SwingUtilities.invokeLater(() -> {
            Main frame = new Main();
            ToolboxFrame toolbox = new ToolboxFrame(frame);
            frame.setToolboxFrame(toolbox);

            // Show main window first
            frame.setVisible(true);

            // Move toolbox to the left of the main window
            Point mainWindowLocation = frame.getLocation();
            int toolboxWidth = toolbox.getWidth();
            int mainWindowY = mainWindowLocation.y;
            int mainWindowX = mainWindowLocation.x;
            toolbox.setLocation(mainWindowX - toolboxWidth, mainWindowY);
            toolbox.setVisible(true);
        });
    }

    // Add this getter for MoveElementAction
    public List<PaintElement> getPaintElements() {
        return paintElements;
    }

    // Add this stub for MoveElementAction (returns null for now, or implement selection logic if needed)
    public PaintElement getCurrentlySelectedElementInList() {
        // If you have a selection model in ToolboxFrame, return the selected element here
        // For now, return null to avoid compilation error
        return null;
    }

    // --- BEGIN: Methods required by ToolboxFrame and ReorderLayerAction ---
    // For ReorderLayerAction
    public void internalMoveElementInList(PaintElement element, int fromIndex, int toIndex) {
        if (element == null || fromIndex < 0 || fromIndex >= paintElements.size() || toIndex < 0 || toIndex > paintElements.size()) {
            logger.error("internalMoveElementInList: Invalid arguments");
            return;
        }
        paintElements.remove(fromIndex);
        if (toIndex > paintElements.size()) toIndex = paintElements.size();
        paintElements.add(toIndex, element);
        updateToolboxLayerList();
        drawingPanel.repaint();
    }

    // For ToolboxFrame
    public void handleLayerSelectionChanged(int selectedIndex) {
        // Optionally, update UI or selection state. No-op stub for now.
    }

    public void updateFrameTitle() {
        String base = "PFeasel Paint Creator";
        if (currentSaveFile != null) {
            setTitle(base + " - " + currentSaveFile.getName());
        } else {
            setTitle(base);
        }
    }
}