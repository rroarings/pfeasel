package app;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatNordIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ToolboxFrame extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(ToolboxFrame.class);
    // Define an enum for the tools
    public enum ToolType {
        SELECT, TEXT, RECTANGLE, ROUND_RECTANGLE, CIRCLE, LINE, POLYGON, FREEHAND, BEZIER, IMAGE_URL, IMAGE_LOCAL, MOVE
    }

    private ToolType selectedTool = ToolType.SELECT; // Default tool changed to SELECT
    private Color fillColor = Color.BLACK;
    private Color strokeColor = Color.BLACK;
    private Color shadowColor = Color.BLACK;

    private Font currentFont;
    private String selectedFontFamily = "Arial";
    private int selectedFontSize = 12; // Changed default from 9 to 12
    private boolean isBold = false;
    private boolean isItalic = false;
    private boolean isUnderline = false; // Note: Underline is often a text attribute, not a direct font style

    private DefaultListModel<String> layersModel = new DefaultListModel<>();
    private JList<String> layersList; // Made layersList a field to access it for selection
    private boolean suppressLayerSelectionEvents = false;

    // Fields for remaining controls
    private boolean isFillEnabled = true;
    private boolean isStrokeEnabled = true;
    private int currentStrokeWidth = 1;
    private String currentText = "";
    private int shadowXOffset = 3;
    private int shadowYOffset = 3;
    private boolean isShadowEnabled = false; // Added for shadow toggle
    private int arcWidth = 20; // Retained for storing value
    private int arcHeight = 20; // Retained for storing value

    private Map<ToolType, JToggleButton> toolButtons = new HashMap<>(); // Changed JButton to JToggleButton
    private Main mainFrame;

    // Fields for menu items that need to be accessed (e.g., to update their state)
    private JCheckBoxMenuItem drawRSInterfaceMenuItem;
    private JCheckBoxMenuItem showGridMenuItem;
    private JRadioButtonMenuItem antiAliasingOffMenuItem;
    private JRadioButtonMenuItem antiAliasingOnMenuItem;
    private JCheckBoxMenuItem snapToGridMenuItem;
    private JMenuItem undoMenuItem; // Field for Undo
    private JMenuItem redoMenuItem; // Field for Redo
    private JMenuItem generateCodeMenuItem; // Added for Generate Code

    private JSpinner fontSizeSpinner; // Added for font size selection

    private String currentTheme = "Flat Light";

    public ToolboxFrame(Main mainFrame) {
        super("Toolbox");
        this.mainFrame = mainFrame;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Changed from EXIT_ON_CLOSE
        setSize(320, 600); // Increased height slightly for better layout
        setLocationRelativeTo(null);

        setupMenuBar(); // Call to setup menu bar

        // Main container
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Sidebar (tools)
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        sidebar.setPreferredSize(new Dimension(40, 0));

        // Tool buttons
        JToggleButton textTool = createToolButton("Text", "/img/ui/text.png", ToolType.TEXT);
        JToggleButton rectTool = createToolButton("Rectangle", "/img/ui/rectangle.png", ToolType.RECTANGLE);
        JToggleButton roundRectTool = createToolButton("Rounded Rectangle", "/img/ui/rectangle-rounded.png", ToolType.ROUND_RECTANGLE);
        JToggleButton circleTool = createToolButton("Circle", "/img/ui/circle.png", ToolType.CIRCLE);
        JToggleButton lineTool = createToolButton("Line", "/img/ui/line.png", ToolType.LINE);
        JToggleButton polygonTool = createToolButton("Polygon", "/img/ui/polygon.png", ToolType.POLYGON);
        JToggleButton imageUrlTool = createToolButton("Image URL", "/img/ui/image-search.png", ToolType.IMAGE_URL);
        JToggleButton imageLocalTool = createToolButton("Local Image", "/img/ui/image.png", ToolType.IMAGE_LOCAL);
        JToggleButton moveTool = createToolButton("Move", "/img/ui/cursor-move.png", ToolType.MOVE);
        JToggleButton freehandTool = createToolButton("Freehand", "/img/ui/pen.png", ToolType.FREEHAND);
        JToggleButton bezierTool = createToolButton("Bezier Curve", "/img/ui/bezier-curve.png", ToolType.BEZIER);

        // Add tool buttons to sidebar
        registerToolButton(sidebar, ToolType.TEXT, textTool);
        registerToolButton(sidebar, ToolType.RECTANGLE, rectTool);
        registerToolButton(sidebar, ToolType.ROUND_RECTANGLE, roundRectTool);
        registerToolButton(sidebar, ToolType.CIRCLE, circleTool);
        registerToolButton(sidebar, ToolType.LINE, lineTool);
        registerToolButton(sidebar, ToolType.POLYGON, polygonTool);
        registerToolButton(sidebar, ToolType.FREEHAND, freehandTool);
        registerToolButton(sidebar, ToolType.BEZIER, bezierTool);
        registerToolButton(sidebar, ToolType.IMAGE_URL, imageUrlTool);
        registerToolButton(sidebar, ToolType.IMAGE_LOCAL, imageLocalTool);
        registerToolButton(sidebar, ToolType.MOVE, moveTool);

        // Right panel (controls)
        JPanel controlsPanel = new JPanel(new GridBagLayout()); // Changed to GridBagLayout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(8, 0, 8, 0); // Added 5px top and bottom padding for each component group

        // Fill and Stroke
        gbc.gridy = 0;
        JPanel fillStrokePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JCheckBox fillCheck = new JCheckBox("Fill", isFillEnabled);
        fillCheck.addActionListener(e -> {
            isFillEnabled = fillCheck.isSelected();
            logger.info("Fill enabled: " + isFillEnabled);
        });

        JCheckBox strokeCheck = new JCheckBox("Stroke", isStrokeEnabled);
        strokeCheck.addActionListener(e -> {
            isStrokeEnabled = strokeCheck.isSelected();
            logger.info("Stroke enabled: " + isStrokeEnabled);
        });

        JButton fillColorBtn = new JButton();
        fillColorBtn.setBackground(fillColor);
        fillColorBtn.setPreferredSize(new Dimension(20, 20));
        fillColorBtn.setToolTipText("Select Fill Color");
        fillColorBtn.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(ToolboxFrame.this, "Choose Fill Color", fillColor);
            if (newColor != null) {
                fillColor = newColor;
                fillColorBtn.setBackground(fillColor);
                logger.info("Fill color set to: " + fillColor);
            }
        });

        JButton strokeColorBtn = new JButton();
        strokeColorBtn.setBackground(strokeColor);
        strokeColorBtn.setPreferredSize(new Dimension(20, 20));
        strokeColorBtn.setToolTipText("Select Stroke Color");
        strokeColorBtn.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(ToolboxFrame.this, "Choose Stroke Color", strokeColor);
            if (newColor != null) {
                strokeColor = newColor;
                strokeColorBtn.setBackground(strokeColor);
                logger.info("Stroke color set to: " + strokeColor);
            }
        });

        JSpinner strokeWidthSpinner = new JSpinner(new SpinnerNumberModel(currentStrokeWidth, 1, 20, 1));
        strokeWidthSpinner.addChangeListener(e -> {
            currentStrokeWidth = (int) strokeWidthSpinner.getValue();
            logger.info("Stroke width: " + currentStrokeWidth);
        });

        fillStrokePanel.add(fillColorBtn);
        fillStrokePanel.add(fillCheck);
        fillStrokePanel.add(strokeColorBtn);
        fillStrokePanel.add(strokeCheck);
        fillStrokePanel.add(strokeWidthSpinner);
        controlsPanel.add(fillStrokePanel, gbc);

        // Arc Options Button (Moved up)
        gbc.gridy = 1;
        gbc.insets = new Insets(10, 0, 8, 0); // Specific top inset, global bottom
        JPanel arcButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton arcOptionsButton = new JButton("Arc Options...");
        arcOptionsButton.addActionListener(e -> {
            ArcOptionsDialog dialog = new ArcOptionsDialog(ToolboxFrame.this, "Arc Options", true, arcWidth, arcHeight);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                arcWidth = dialog.getNewArcWidth();
                arcHeight = dialog.getNewArcHeight();
                logger.info("Arc Width updated to: " + arcWidth);
                logger.info("Arc Height updated to: " + arcHeight);
                if (mainFrame != null) mainFrame.repaintDrawingPanel(); // Basic repaint trigger
            }
        });
        arcButtonPanel.add(arcOptionsButton);
        controlsPanel.add(arcButtonPanel, gbc);

        // Text input
        gbc.gridy = 2; // Adjusted gridy
        gbc.insets = new Insets(8, 0, 8, 0); // Reset to global insets
        JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        textPanel.add(new JLabel("Text:"));
        JTextField textFieldComponent = new JTextField(currentText, 12);
        textFieldComponent.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateText();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateText();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateText();
            }

            private void updateText() {
                currentText = textFieldComponent.getText();
                logger.info("Current text: " + currentText);
            }
        });
        textPanel.add(textFieldComponent);
        controlsPanel.add(textPanel, gbc);

        // Font selection
        gbc.gridy = 3; // Adjusted gridy
        gbc.insets = new Insets(8, 0, 10, 0); // Reset to global insets
        gbc.ipady = 30; // Increased internal vertical padding for fontPanel
        JPanel fontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 5));
        fontPanel.setBorder(BorderFactory.createTitledBorder("Font")); // Added TitledBorder
        JComboBox<String> fontCombo = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        fontCombo.setSelectedItem(selectedFontFamily);
        fontCombo.addActionListener(e -> {
            selectedFontFamily = (String) fontCombo.getSelectedItem();
            updateCurrentFont();
            logger.info("Font family: " + selectedFontFamily);
        });
        fontPanel.add(fontCombo);

        JToggleButton boldBtn = new JToggleButton("B");
        boldBtn.setToolTipText("Bold");
        boldBtn.addActionListener(e -> {
            isBold = boldBtn.isSelected();
            updateCurrentFont();
            logger.info("Bold: " + isBold);
        });

        JToggleButton italicBtn = new JToggleButton("I");
        italicBtn.setToolTipText("Italic");
        italicBtn.addActionListener(e -> {
            isItalic = italicBtn.isSelected();
            updateCurrentFont();
            logger.info("Italic: " + isItalic);
        });

        JToggleButton underlineBtn = new JToggleButton("U");
        underlineBtn.setToolTipText("Underline");
        underlineBtn.addActionListener(e -> {
            isUnderline = underlineBtn.isSelected();
            logger.info("Underline: " + isUnderline);
        });

        fontPanel.add(boldBtn);
        fontPanel.add(italicBtn);
        fontPanel.add(underlineBtn);

        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(selectedFontSize, 6, 72, 1));
        fontSizeSpinner.addChangeListener(e -> {
            selectedFontSize = (int) fontSizeSpinner.getValue();
            updateCurrentFont();
            logger.info("Font size: " + selectedFontSize);
        });
        fontPanel.add(fontSizeSpinner);
        controlsPanel.add(fontPanel, gbc);
        gbc.ipady = 0; // Reset ipady for subsequent components

        // Initialize currentFont
        updateCurrentFont();

        // Layers list
        gbc.gridy = 4; // Adjusted gridy
        gbc.insets = new Insets(8, 0, 8, 0); // Use global insets
        gbc.fill = GridBagConstraints.BOTH; // Changed from HORIZONTAL to BOTH
        gbc.weighty = 0.7; // Give it more weight to expand vertically
        JPanel layersPanel = new JPanel();
        layersPanel.setLayout(new BoxLayout(layersPanel, BoxLayout.Y_AXIS));
        layersPanel.setBorder(BorderFactory.createTitledBorder("Layers:"));
        layersList = new JList<>(layersModel);
        layersList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // Initialize buttons here so the listener can access them
        JButton upBtn = createLayerButton("Move Layer Up", "/img/ui/layer-up.png", "▲");
        JButton downBtn = createLayerButton("Move Layer Down", "/img/ui/layer-down.png", "▼");
        JButton duplicateBtn = createLayerButton("Duplicate Selected Layer", "/img/ui/layer-duplicate.png", "❏");
        JButton editBtn = createLayerButton("Edit Layer Name", "/img/ui/layer-edit.png", "✎");
        JButton deleteBtn = createLayerButton("Delete Layer", "/img/ui/layer-delete.png", "🗑");
        JButton clearBtn = createLayerButton("Clear All Layers", "/img/ui/layer-clear.png", "✕");

        layersList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && !suppressLayerSelectionEvents) {
                    int[] selectedIndices = layersList.getSelectedIndices();
                    int selectedCount = selectedIndices.length;
                    boolean anySelected = selectedCount > 0;
                    boolean singleSelected = selectedCount == 1;

                    editBtn.setEnabled(singleSelected);
                    deleteBtn.setEnabled(anySelected);
                    duplicateBtn.setEnabled(anySelected);
                    clearBtn.setEnabled(!layersModel.isEmpty());

                    if (anySelected) {
                        boolean canMoveUp = false;
                        boolean canMoveDown = false;
                        for (int selectedIndex : selectedIndices) {
                            if (selectedIndex > 0) {
                                canMoveUp = true;
                            }
                            if (selectedIndex < layersModel.getSize() - 1) {
                                canMoveDown = true;
                            }
                        }
                        upBtn.setEnabled(canMoveUp);
                        downBtn.setEnabled(canMoveDown);
                    } else {
                        upBtn.setEnabled(false);
                        downBtn.setEnabled(false);
                    }

                    if (mainFrame != null) {
                        mainFrame.handleLayerSelectionChanged(selectedIndices, layersList.getLeadSelectionIndex());
                    }
                }
            }
        });
        JScrollPane layersScroll = new JScrollPane(layersList);
        layersScroll.setPreferredSize(new Dimension(180, 200)); // Increased height further
        layersPanel.add(layersScroll);

        // Layer controls
        JPanel layerBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));

        // Initially disable buttons that require selection
        editBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
        upBtn.setEnabled(false);
        downBtn.setEnabled(false);
        duplicateBtn.setEnabled(false);
        clearBtn.setEnabled(false);

        duplicateBtn.addActionListener(e -> { // Changed from addBtn
            int[] selectedIndices = layersList.getSelectedIndices();
            if (selectedIndices.length > 0) {
                mainFrame.duplicatePaintElements(selectedIndices);
            } else {
                JOptionPane.showMessageDialog(ToolboxFrame.this, "Please select a layer to duplicate.", "No Layer Selected", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        editBtn.addActionListener(e -> {
            int selectedIndex = layersList.getSelectedIndex();
            if (selectedIndex != -1) {
                String currentLabel = layersModel.getElementAt(selectedIndex);
                String currentName = stripLayerIndexPrefix(currentLabel);
                Object newNameInputObj = JOptionPane.showInputDialog(ToolboxFrame.this, "Enter new layer name:", "Edit Layer Name", JOptionPane.PLAIN_MESSAGE, null, null, currentName);

                if (newNameInputObj != null) { // User clicked OK or entered text
                    String newName = ((String) newNameInputObj).trim();
                    if (!newName.isEmpty() && !newName.equals(currentName)) {
                        // Check for name uniqueness (optional, Main.java could also enforce this or handle conflicts)
                        boolean nameExists = false;
                        for (int i = 0; i < layersModel.getSize(); i++) {
                            String existingName = stripLayerIndexPrefix(layersModel.getElementAt(i));
                            if (i != selectedIndex && existingName.equals(newName)) {
                                nameExists = true;
                                break;
                            }
                        }
                        if (nameExists) {
                            JOptionPane.showMessageDialog(ToolboxFrame.this, "Layer name '" + newName + "' already exists.", "Name Conflict", JOptionPane.ERROR_MESSAGE);
                        } else {
                            // Notify Main.java to update the PaintElement's displayName
                            if (mainFrame != null) {
                                mainFrame.updatePaintElementDisplayName(selectedIndex, newName);
                            }
                            logger.info("Edited layer: '" + currentLabel + "' to '" + newName + "' at JList index " + selectedIndex);
                        }
                    } else if (newName.isEmpty()) {
                        JOptionPane.showMessageDialog(ToolboxFrame.this, "Layer name cannot be empty.", "Invalid Name", JOptionPane.ERROR_MESSAGE);
                    }
                    // If newName is same as currentName, do nothing.
                }
                // If user clicked Cancel, newNameInput will be null, do nothing.
            }
        });

        deleteBtn.addActionListener(e -> {
            int[] selectedIndices = layersList.getSelectedIndices();
            if (selectedIndices.length > 0) {
                String layerName = selectedIndices.length == 1
                        ? layersModel.getElementAt(selectedIndices[0])
                        : selectedIndices.length + " selected layers";
                int confirm = JOptionPane.showConfirmDialog(ToolboxFrame.this,
                        "Are you sure you want to delete layer '" + layerName + "'?",
                        "Delete Layer",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    mainFrame.deletePaintElements(selectedIndices);
                    logger.info("Delete request for {} layer(s)", selectedIndices.length);
                }
            }
        });

        upBtn.addActionListener(e -> {
            int[] selectedIndices = layersList.getSelectedIndices();
            if (selectedIndices.length > 0) {
                mainFrame.moveLayersUp(selectedIndices);
            }
        });

        downBtn.addActionListener(e -> {
            int[] selectedIndices = layersList.getSelectedIndices();
            if (selectedIndices.length > 0) {
                mainFrame.moveLayersDown(selectedIndices);
            }
        });

        clearBtn.addActionListener(e -> {
            if (!layersModel.isEmpty()) {
                int confirm = JOptionPane.showConfirmDialog(ToolboxFrame.this,
                        "Are you sure you want to clear all layers?",
                        "Clear All Layers",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (mainFrame != null) mainFrame.clearPaintElements();
                    logger.info("Cleared all layers");
                }
            }
        });

        layerBtns.add(upBtn);
        layerBtns.add(downBtn);
        layerBtns.add(duplicateBtn);
        layerBtns.add(editBtn);
        layerBtns.add(deleteBtn);
        layerBtns.add(clearBtn);
        layersPanel.add(layerBtns);
        controlsPanel.add(layersPanel, gbc);

        // Shadow controls
        gbc.gridy = 5; // Adjusted gridy
        gbc.insets = new Insets(8, 0, 8, 0); // Use global insets
        gbc.fill = GridBagConstraints.HORIZONTAL; // Reset for shadow panel
        gbc.weighty = 0; // Reset weighty for shadow panel
        JPanel shadowPanel = new JPanel(); // Main shadow panel
        shadowPanel.setLayout(new BoxLayout(shadowPanel, BoxLayout.Y_AXIS)); // Use BoxLayout for vertical arrangement
        shadowPanel.setBorder(BorderFactory.createTitledBorder("Shadow"));

        // Row 1: Enabled checkbox and Color button
        JPanel shadowRow1Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JCheckBox shadowEnabledCheck = new JCheckBox("Enabled", isShadowEnabled);
        shadowEnabledCheck.addActionListener(e -> {
            isShadowEnabled = shadowEnabledCheck.isSelected();
            logger.info("Shadow enabled: " + isShadowEnabled);
            if (mainFrame != null) mainFrame.repaintDrawingPanel();
        });
        shadowRow1Panel.add(shadowEnabledCheck);

        shadowRow1Panel.add(new JLabel("Color:"));
        JButton shadowColorBtn = new JButton();
        shadowColorBtn.setBackground(shadowColor);
        shadowColorBtn.setPreferredSize(new Dimension(20, 20));
        shadowColorBtn.setToolTipText("Select Shadow Color");
        shadowColorBtn.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(ToolboxFrame.this, "Choose Shadow Color", shadowColor);
            if (newColor != null) {
                shadowColor = newColor;
                shadowColorBtn.setBackground(shadowColor);
                logger.info("Shadow color set to: " + shadowColor);
            }
        });
        shadowRow1Panel.add(shadowColorBtn);
        shadowPanel.add(shadowRow1Panel); // Add first row to main shadow panel

        // Row 2: X and Y offset spinners
        JPanel shadowRow2Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        shadowRow2Panel.setBorder(new EmptyBorder(5, 0, 8, 0)); // Added 5px top padding, kept existing 8px bottom padding
        shadowRow2Panel.add(new JLabel("X:"));
        JSpinner shadowXSpinner = new JSpinner(new SpinnerNumberModel(shadowXOffset, -20, 20, 1));
        shadowXSpinner.addChangeListener(e -> {
            shadowXOffset = (int) shadowXSpinner.getValue();
            logger.info("Shadow X Offset: " + shadowXOffset);
        });
        shadowRow2Panel.add(shadowXSpinner);

        shadowRow2Panel.add(new JLabel("Y:"));
        JSpinner shadowYSpinner = new JSpinner(new SpinnerNumberModel(shadowYOffset, -20, 20, 1));
        shadowYSpinner.addChangeListener(e -> {
            shadowYOffset = (int) shadowYSpinner.getValue();
            logger.info("Shadow Y Offset: " + shadowYOffset);
        });
        shadowRow2Panel.add(shadowYSpinner);
        shadowPanel.add(shadowRow2Panel); // Add second row to main shadow panel

        controlsPanel.add(shadowPanel, gbc);

        // Spacer panel to push everything up
        gbc.gridy = 6; // Adjusted gridy
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weighty = 0.3; // Reduced weight, layersPanel will take more space
        gbc.fill = GridBagConstraints.VERTICAL;
        JPanel spacerPanel = new JPanel();
        controlsPanel.add(spacerPanel, gbc);

        // Add sidebar and controls to main panel
        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(controlsPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);

        setSelectedTool(ToolType.MOVE); // Set default selected tool and apply its style
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        newItem.addActionListener(e -> {
            // Prompt the user to save current work if necessary
            int response = JOptionPane.showConfirmDialog(mainFrame,
                    "Do you want to save the current changes before creating a new file?",
                    "New File",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (response == JOptionPane.CANCEL_OPTION) {
                return; // User cancelled
            }
            if (response == JOptionPane.YES_OPTION) {
                mainFrame.handleSave(); // Save current work
                // Check if save was successful or if user cancelled save, though handleSave doesn't directly return status
            }
            // Proceed to clear current drawing
            mainFrame.clearPaintElements(); // This should also clear undo/redo, update title, etc.
            mainFrame.setCurrentSaveFile(null); // Reset current save file
            mainFrame.updateFrameTitle();
            mainFrame.setLastActionStatus("New drawing created");
        });

        JMenuItem openItem = new JMenuItem("Open...");
        openItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.handleOpen();
            }
        });

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.handleSave(); // Call Main's save handler
            }
        });
        JMenuItem saveAsItem = new JMenuItem("Save As...");
        saveAsItem.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.handleSaveAs(); // Call Main's save as handler
            }
        });
        generateCodeMenuItem = new JMenuItem("Generate Code..."); // Initialize here
        generateCodeMenuItem.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.handleGenerateCode();
            }
        });

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(generateCodeMenuItem); // Add to menu
        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                ToolboxFrame.this,
                "Are you sure you want to quit?",
                "Exit Confirmation",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        undoMenuItem = new JMenuItem("Undo"); // Assign to field
        undoMenuItem.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.undo();
            }
        });
        undoMenuItem.setEnabled(false); // Initially disabled

        redoMenuItem = new JMenuItem("Redo"); // Assign to field
        redoMenuItem.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.redo();
            }
        });
        redoMenuItem.setEnabled(false); // Initially disabled

        editMenu.add(undoMenuItem);
        editMenu.add(redoMenuItem); // Corrected: Add to editMenu
        editMenu.addSeparator();
        JMenuItem clearItem = new JMenuItem("Clear");
        clearItem.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.clearPaintElements(); // Assuming a method in Main to clear elements and repaint
                clearLayersList(); // Clear layers in ToolboxFrame
                JOptionPane.showMessageDialog(this, "Canvas and layers cleared.");
            }
        });
        editMenu.add(clearItem);
        menuBar.add(editMenu);

        // View Menu
        JMenu viewMenu = new JMenu("View");
        drawRSInterfaceMenuItem = new JCheckBoxMenuItem("Draw RS Interface", mainFrame != null && mainFrame.isDrawRSInterfaceVisible());
        drawRSInterfaceMenuItem.addActionListener(e -> {
            if (mainFrame != null) mainFrame.setDrawRSInterface(drawRSInterfaceMenuItem.isSelected());
        });
        viewMenu.add(drawRSInterfaceMenuItem);

        JMenuItem changeBgColorItem = new JMenuItem("Change Background Color...");
        changeBgColorItem.addActionListener(e -> {
            if (mainFrame != null) mainFrame.handleChangeBackgroundColor();
        });
        viewMenu.add(changeBgColorItem);
        viewMenu.addSeparator();

        showGridMenuItem = new JCheckBoxMenuItem("Show Grid", mainFrame != null && mainFrame.isGridVisible());
        showGridMenuItem.addActionListener(e -> {
            if (mainFrame != null) mainFrame.setGridVisible(showGridMenuItem.isSelected());
        });
        viewMenu.add(showGridMenuItem);

        JMenuItem gridOptionsMenuItem = new JMenuItem("Grid options...");
        gridOptionsMenuItem.addActionListener(e -> {
            if (mainFrame != null) mainFrame.showGridOptionsDialog();
        });
        viewMenu.add(gridOptionsMenuItem);

        JMenuItem resetGridMenuItem = new JMenuItem("Reset Grid to Defaults");
        resetGridMenuItem.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.resetGridToDefaults();
                // Update the checkbox state after resetting defaults in Main
                showGridMenuItem.setSelected(mainFrame.isGridVisible());
                snapToGridMenuItem.setSelected(mainFrame.isSnapToGridActive()); // Also update snap if it depends on grid defaults
            }
        });
        viewMenu.add(resetGridMenuItem);
        viewMenu.addSeparator();

        snapToGridMenuItem = new JCheckBoxMenuItem("Snap to Grid", mainFrame != null && mainFrame.isSnapToGridActive());
        snapToGridMenuItem.addActionListener(e -> {
            if (mainFrame != null) mainFrame.setSnapToGrid(snapToGridMenuItem.isSelected());
            logger.info("Snap to Grid selected: " + snapToGridMenuItem.isSelected());
        });
        viewMenu.add(snapToGridMenuItem);
        viewMenu.addSeparator();

        JMenu antiAliasingMenu = new JMenu("Anti-aliasing");
        ButtonGroup antiAliasingGroup = new ButtonGroup();
        boolean antiAliasingCurrentlyOn = mainFrame != null && mainFrame.isAntiAliasingActive();
        antiAliasingOffMenuItem = new JRadioButtonMenuItem("Off", !antiAliasingCurrentlyOn);
        antiAliasingOnMenuItem = new JRadioButtonMenuItem("On", antiAliasingCurrentlyOn);

        antiAliasingOffMenuItem.addActionListener(e -> {
            if (mainFrame != null && antiAliasingOffMenuItem.isSelected()) mainFrame.setAntiAliasing(false);
        });
        antiAliasingOnMenuItem.addActionListener(e -> {
            if (mainFrame != null && antiAliasingOnMenuItem.isSelected()) mainFrame.setAntiAliasing(true);
        });

        antiAliasingGroup.add(antiAliasingOffMenuItem);
        antiAliasingGroup.add(antiAliasingOnMenuItem);
        antiAliasingMenu.add(antiAliasingOffMenuItem);
        antiAliasingMenu.add(antiAliasingOnMenuItem);
        viewMenu.add(antiAliasingMenu);

        // Theme Switcher
        JMenu themeMenu = new JMenu("Theme");
        ButtonGroup themeGroup = new ButtonGroup();
        JRadioButtonMenuItem lightTheme = new JRadioButtonMenuItem("Flat Light");
        JRadioButtonMenuItem intellijTheme = new JRadioButtonMenuItem("IntelliJ");
        JRadioButtonMenuItem darkTheme = new JRadioButtonMenuItem("Flat Dark");
        JRadioButtonMenuItem darculaTheme = new JRadioButtonMenuItem("Darcula");
        JRadioButtonMenuItem arcDarkTheme = new JRadioButtonMenuItem("Arc Dark");
        JRadioButtonMenuItem nordTheme = new JRadioButtonMenuItem("Nord");
        JRadioButtonMenuItem spacegrayTheme = new JRadioButtonMenuItem("Spacegray");
        lightTheme.setSelected(true);
        themeGroup.add(lightTheme);
        themeGroup.add(intellijTheme);
        themeGroup.add(darkTheme);
        themeGroup.add(darculaTheme);
        themeGroup.add(arcDarkTheme);
        themeGroup.add(nordTheme);
        themeGroup.add(spacegrayTheme);
        // Add light themes
        themeMenu.add(lightTheme);
        themeMenu.add(intellijTheme);
        themeMenu.addSeparator();
        // Add dark themes
        themeMenu.add(darkTheme);
        themeMenu.add(darculaTheme);
        themeMenu.add(arcDarkTheme);
        themeMenu.add(nordTheme);
        themeMenu.add(spacegrayTheme);
        viewMenu.addSeparator();
        viewMenu.add(themeMenu);
        // Theme switching actions
        lightTheme.addActionListener(e -> switchTheme("Flat Light"));
        intellijTheme.addActionListener(e -> switchTheme("IntelliJ"));
        darkTheme.addActionListener(e -> switchTheme("Flat Dark"));
        darculaTheme.addActionListener(e -> switchTheme("Darcula"));
        arcDarkTheme.addActionListener(e -> switchTheme("Arc Dark"));
        nordTheme.addActionListener(e -> switchTheme("Nord"));
        spacegrayTheme.addActionListener(e -> switchTheme("Spacegray"));

        menuBar.add(viewMenu);
        setJMenuBar(menuBar);
    }

    private void switchTheme(String themeName) {
        try {
            String previousTheme = currentTheme;
            switch (themeName) {
                case "Flat Light":
                    UIManager.setLookAndFeel(new FlatLightLaf());
                    break;
                case "Flat Dark":
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                    break;
                case "IntelliJ":
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    break;
                case "Darcula":
                    UIManager.setLookAndFeel(new FlatDarculaLaf());
                    break;
                case "Arc Dark":
                    UIManager.setLookAndFeel(new FlatArcDarkIJTheme());
                    break;
                case "Nord":
                    UIManager.setLookAndFeel(new FlatNordIJTheme());
                    break;
                case "Spacegray":
                    UIManager.setLookAndFeel(new FlatSpacegrayIJTheme());
                    break;
                default:
                    UIManager.setLookAndFeel(new FlatLightLaf());
            }
            currentTheme = themeName;
            logger.info("Switched theme: from {} to {}", previousTheme, currentTheme);
            SwingUtilities.updateComponentTreeUI(this);
            if (mainFrame != null) {
                SwingUtilities.updateComponentTreeUI(mainFrame);
            }
        } catch (Exception ex) {
            logger.error("Failed to set theme: " + themeName, ex);
            JOptionPane.showMessageDialog(this, "Failed to apply theme: " + themeName, "Theme Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton createLayerButton(String tooltip, String iconPath, String fallbackText) {
        JButton button = new JButton();
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        try {
            java.net.URL resource = getClass().getResource(iconPath);
            if (resource != null) {
                button.setIcon(new ImageIcon(resource));
            } else {
                button.setText(fallbackText);
            }
        } catch (Exception e) {
            button.setText(fallbackText);
        }
        return button;
    }

    private JToggleButton createToolButton(String tooltip, String iconPath, ToolType toolType) {
        JToggleButton button = new JToggleButton();
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        java.net.URL resource = getClass().getResource(iconPath);
        if (resource != null) {
            button.setIcon(new ImageIcon(resource));
        } else {
            logger.warn("Icon not found: {}", iconPath);
            button.setText(tooltip.substring(0, Math.min(tooltip.length(), 3)));
        }
        button.addActionListener(e -> setSelectedTool(toolType));
        return button;
    }

    private void registerToolButton(JPanel sidebar, ToolType toolType, JToggleButton button) {
        sidebar.add(button);
        toolButtons.put(toolType, button);
    }

    private void updateCurrentFont() {
        int style = Font.PLAIN;
        if (isBold) style |= Font.BOLD;
        if (isItalic) style |= Font.ITALIC;
        currentFont = new Font(selectedFontFamily, style, selectedFontSize);
    }

    private void setSelectedTool(ToolType toolType) {
        this.selectedTool = toolType;
        logger.info("Selected tool: " + toolType);

        // Update button appearances using JToggleButton's selected state
        for (Map.Entry<ToolType, JToggleButton> entry : toolButtons.entrySet()) { // Changed JButton to JToggleButton
            JToggleButton button = entry.getValue(); // Changed JButton to JToggleButton
            button.setSelected(entry.getKey() == toolType);
        }
        // Notify Main frame to update cursor
        if (mainFrame != null) {
            mainFrame.updateCursorForTool(toolType);

            // Immediately trigger dialogs for image tools
            if (toolType == ToolType.IMAGE_URL) {
                mainFrame.handleImageUrlInput(); // Call without arguments
            } else if (toolType == ToolType.IMAGE_LOCAL) {
                mainFrame.handleLocalImageInput(); // Call without arguments
            }
        }
    }

    public ToolType getSelectedTool() {
        return selectedTool;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public Color getShadowColor() {
        return shadowColor;
    }

    public Font getCurrentFont() {
        return currentFont;
    }

    public String getSelectedFontFamily() {
        return selectedFontFamily;
    }

    public int getSelectedFontSize() {
        return selectedFontSize;
    }

    public boolean isBold() {
        return isBold;
    }

    public boolean isItalic() {
        return isItalic;
    }

    public boolean isUnderline() {
        return isUnderline;
    }

    // Getter for the layers model (JList model)
    public DefaultListModel<String> getLayersListModel() {
        return layersModel;
    }

    // Getter for layersList
    public JList<String> getLayersList() {
        return layersList;
    }

    // Method to update the entire layers list from a list of names
    public void updateLayersList(java.util.List<String> layerNames) {
        layersModel.clear();
        for (String layerName : layerNames) {
            layersModel.addElement(layerName);
        }
    }

    // Method to select a layer in the list by index (from paintElements list)
    public void selectLayerInList(int paintElementIndex) {
        int jListIndex = paintElementIndex;
        if (layersList != null && jListIndex >= 0 && jListIndex < layersModel.size()) {
            suppressLayerSelectionEvents = true;
            try {
                layersList.setSelectedIndex(jListIndex);
                layersList.ensureIndexIsVisible(jListIndex);
            } finally {
                suppressLayerSelectionEvents = false;
            }
        }
    }

    public void selectLayersInList(java.util.List<Integer> paintElementIndices, int primaryPaintElementIndex) {
        if (layersList == null) {
            return;
        }

        suppressLayerSelectionEvents = true;
        try {
            layersList.clearSelection();
            if (paintElementIndices == null || paintElementIndices.isEmpty()) {
                return;
            }

            for (Integer paintIndex : paintElementIndices) {
                if (paintIndex == null) {
                    continue;
                }
                int jListIndex = paintIndex;
                if (jListIndex >= 0 && jListIndex < layersModel.size()) {
                    layersList.addSelectionInterval(jListIndex, jListIndex);
                }
            }

            int leadIndex = primaryPaintElementIndex;
            if (leadIndex >= 0 && leadIndex < layersModel.size()) {
                layersList.getSelectionModel().setLeadSelectionIndex(leadIndex);
                layersList.ensureIndexIsVisible(leadIndex);
            }
        } finally {
            suppressLayerSelectionEvents = false;
        }
    }

    public void clearLayerSelection() {
        suppressLayerSelectionEvents = true;
        try {
            layersList.clearSelection();
        } finally {
            suppressLayerSelectionEvents = false;
        }
    }

    // Method to clear all layers from the list
    public void clearLayersList() {
        layersModel.clear();
    }

    // Methods to enable/disable Undo/Redo menu items
    public void setUndoEnabled(boolean enabled) {
        if (undoMenuItem != null) {
            undoMenuItem.setEnabled(enabled);
        }
    }

    public void setRedoEnabled(boolean enabled) {
        if (redoMenuItem != null) {
            redoMenuItem.setEnabled(enabled);
        }
    }

    // Method to add layer name at a specific index
    public void addLayerToList(String layerName, int index) {
        if (index >= 0 && index <= layersModel.getSize()) {
            layersModel.add(index, layerName);
        } else {
            layersModel.add(0, layerName); // Default to adding at the top if index is out of bounds
            logger.info("addLayerToList: index " + index + " out of bounds (" + layersModel.getSize() + "), adding to top (0).");
        }
    }

    // Overloaded method to add to the top (index 0) by default, for new shapes
    public void addLayerToList(String layerName) {
        addLayerToList(layerName, 0);
    }

    private String stripLayerIndexPrefix(String label) {
        if (label == null) return "";
        return label.replaceFirst("^\\[\\d+\\]\\s*", "").trim();
    }

    public void moveLayerToTopInList(int oldIndex) {
        if (oldIndex > 0 && oldIndex < layersModel.getSize()) {
            String layerName = layersModel.remove(oldIndex);
            layersModel.add(0, layerName);
            // If you have a JList component named layersList that displays this model:
            // layersList.setSelectedIndex(0); // Uncomment and adapt if you have a direct reference to layersList
            logger.info("Moved layer in ToolboxFrame: '" + layerName + "' from index " + oldIndex + " to 0");
        } else if (oldIndex == 0) {
            // Already at the top, do nothing or log
            if (!layersModel.isEmpty()) {
                logger.info("Layer in ToolboxFrame is already at the top: " + layersModel.getElementAt(0));
            } else {
                logger.info("moveLayerToTopInList: Attempted to access element at index 0, but layersModel is empty.");
            }
        } else {
            logger.info("moveLayerToTopInList: Invalid oldIndex " + oldIndex + " for layersModel size " + layersModel.getSize());
        }
    }

    public void updateLayerName(int index, String newName) {
        if (index >= 0 && index < layersModel.getSize()) {
            layersModel.setElementAt(newName, index);
            logger.info("Updated layer name at index " + index + " to: " + newName);
        } else {
            logger.info("updateLayerName: Invalid index " + index + " for layersModel size " + layersModel.getSize());
        }
    }

    // Getter methods for the new controls
    public boolean isFillEnabled() {
        return isFillEnabled;
    }

    public boolean isStrokeEnabled() {
        return isStrokeEnabled;
    }

    public int getCurrentStrokeWidth() {
        return currentStrokeWidth;
    }

    public String getCurrentText() {
        return currentText;
    }

    public int getShadowXOffset() {
        return shadowXOffset;
    }

    public int getShadowYOffset() {
        return shadowYOffset;
    }

    public boolean isShadowEnabled() { // Added getter
        return isShadowEnabled;
    }

    public int getArcWidth() { // Added getter
        return arcWidth;
    }

    public int getArcHeight() { // Added getter
        return arcHeight;
    }

    public int getCurrentFontSize() {
        if (fontSizeSpinner != null) {
            Object value = fontSizeSpinner.getValue();
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        return 12; // Default font size if spinner not available or value is not a number
    }

    // Inner class for Arc Options Dialog
    private class ArcOptionsDialog extends JDialog {
        private JSpinner arcWidthSpinner;
        private JSpinner arcHeightSpinner;
        private int newArcWidth;
        private int newArcHeight;
        private boolean confirmed = false;

        public ArcOptionsDialog(Frame owner, String title, boolean modal, int currentArcWidth, int currentArcHeight) {
            super(owner, title, modal);
            this.newArcWidth = currentArcWidth;
            this.newArcHeight = currentArcHeight;

            setLayout(new GridBagLayout());
            GridBagConstraints gbcDialog = new GridBagConstraints();
            gbcDialog.insets = new Insets(5, 5, 5, 5);
            gbcDialog.anchor = GridBagConstraints.WEST;

            gbcDialog.gridx = 0;
            gbcDialog.gridy = 0;
            add(new JLabel("Arc Width:"), gbcDialog);

            gbcDialog.gridx = 1;
            arcWidthSpinner = new JSpinner(new SpinnerNumberModel(currentArcWidth, 0, 200, 1));
            arcWidthSpinner.setPreferredSize(new Dimension(80, arcWidthSpinner.getPreferredSize().height));
            add(arcWidthSpinner, gbcDialog);

            gbcDialog.gridx = 0;
            gbcDialog.gridy = 1;
            add(new JLabel("Arc Height:"), gbcDialog);

            gbcDialog.gridx = 1;
            arcHeightSpinner = new JSpinner(new SpinnerNumberModel(currentArcHeight, 0, 200, 1));
            arcHeightSpinner.setPreferredSize(new Dimension(80, arcHeightSpinner.getPreferredSize().height));
            add(arcHeightSpinner, gbcDialog);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton okButton = new JButton("OK");
            okButton.addActionListener(e -> {
                newArcWidth = (int) arcWidthSpinner.getValue();
                newArcHeight = (int) arcHeightSpinner.getValue();
                confirmed = true;
                dispose();
            });
            buttonPanel.add(okButton);

            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> {
                confirmed = false;
                dispose();
            });
            buttonPanel.add(cancelButton);

            gbcDialog.gridx = 0;
            gbcDialog.gridy = 2;
            gbcDialog.gridwidth = 2;
            gbcDialog.anchor = GridBagConstraints.CENTER;
            add(buttonPanel, gbcDialog);

            pack();
            setLocationRelativeTo(owner);
        }

        public int getNewArcWidth() {
            return newArcWidth;
        }

        public int getNewArcHeight() {
            return newArcHeight;
        }

        public boolean isConfirmed() {
            return confirmed;
        }
    }

    public static void setupInitialTheme() {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex.getMessage());
        }
    }
}
