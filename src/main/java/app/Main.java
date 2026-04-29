package app;

import javax.imageio.ImageIO;
import javax.swing.*;

import actions.AddElementAction;
import actions.ChangeDisplayNameAction;
import actions.ClearAllAction;
import actions.DeleteElementAction;
import actions.ReorderLayerAction;
import actions.UndoableAction;
import paintcomponents.ImageElement;
import ui.GridManager;

import paintcomponents.PaintElement;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
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
    private final ElementService elementService;
    private final ProjectIOService projectIOService;
    private final ImageImportService imageImportService;
    private final CanvasRenderer canvasRenderer;
    private final DrawingController drawingController;
    private final ShapeCreationService shapeCreationService;

    private List<PaintElement> paintElements = new ArrayList<>();

    // New state fields
    private boolean rsInterfaceVisible = true;
    private boolean snapToGridActive = false;
    private boolean antiAliasingActive = true; // Default to on

    // Undo/Redo stacks
    private final Deque<UndoableAction> undoStack = new ArrayDeque<>();
    private final Deque<UndoableAction> redoStack = new ArrayDeque<>();

    private GridManager gridManager;

    public Main() {
        setTitle("PFeasel Paint Creator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        elementService = new ElementService();
        projectIOService = new ProjectIOService();
        imageImportService = new ImageImportService();
        canvasRenderer = new CanvasRenderer();
        shapeCreationService = new ShapeCreationService();
        gridManager = new GridManager(this::repaintDrawingPanel);
        drawingController = new DrawingController(this);

        loadRSInterfaceImage();
        drawingPanel = new DrawingPanel(this, drawingController, canvasRenderer, shapeCreationService);
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
        try {
            projectIOService.save(currentSaveFile, paintElements);
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
            try {
                List<PaintElement> loadedElements = projectIOService.load(file);
                paintElements.clear();
                paintElements.addAll(loadedElements);
                setCurrentSaveFile(file);
                updateToolboxLayerList();
                drawingPanel.repaint();
                setStatus("Opened " + file.getName());
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
            BufferedImage img = imageImportService.loadFromUrlString(url);
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
                BufferedImage img = imageImportService.loadFromFile(file);
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
                                                 .map(this::getEffectiveDisplayName)
                                                 .collect(Collectors.toList());
            Collections.reverse(layerNames); // To display top layer at the top of the list
            toolboxFrame.updateLayersList(layerNames);
        }
    }

    private String getEffectiveDisplayName(PaintElement element) {
        if (element == null) {
            return "Unnamed";
        }
        String displayName = element.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            return element.getName();
        }
        return displayName;
    }

    public String getElementDisplayName(PaintElement element) {
        return getEffectiveDisplayName(element);
    }

    public String generateUniqueDisplayName(String baseName) {
        return generateUniqueDisplayNameInternal(baseName);
    }

    public void updateMouseCoordinates(int x, int y) {
        if (statusLabel != null) {
            statusLabel.setText("X: " + x + ", Y: " + y);
        }
    }

    public GridManager getGridManager() {
        return gridManager;
    }

    public ToolboxFrame getToolboxFrame() {
        return toolboxFrame;
    }

    public BufferedImage getRsInterfaceImage() {
        return rsInterfaceImage;
    }

    private boolean isValidPaintElementIndex(int index) {
        return elementService.isValidIndex(paintElements, index);
    }

    private int toPaintElementIndex(int toolboxIndex) {
        return elementService.toPaintElementIndex(paintElements.size(), toolboxIndex);
    }

    private void reorderPaintElements(int fromIndex, int toIndex) {
        elementService.moveElement(paintElements, fromIndex, toIndex);
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
        if (isValidPaintElementIndex(index)) {
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
        int actualPaintElementIndex = toPaintElementIndex(selectedIndexInListModel);
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

            reorderPaintElements(listIndexInToolbox, listIndexInToolbox - 1);

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

            reorderPaintElements(listIndexInToolbox, listIndexInToolbox + 1);

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

    private String generateUniqueDisplayNameInternal(String baseName) {
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