package ui;

import javax.swing.*;
import java.awt.*;

public class GridManager {

    public static final int DEFAULT_GRID_SPACING_WIDTH = 20;
    public static final int DEFAULT_GRID_SPACING_HEIGHT = 20;
    public static final Color DEFAULT_GRID_COLOR = new Color(200, 200, 200);

    private boolean gridVisible = false; // Renamed from showGrid, matches Main.java expectation
    private int gridSpacingWidth = DEFAULT_GRID_SPACING_WIDTH;
    private int gridSpacingHeight = DEFAULT_GRID_SPACING_HEIGHT;
    private Color gridColor = DEFAULT_GRID_COLOR;

    private Runnable repaintCallback; // Changed from Canvas to Runnable

    public GridManager(Runnable repaintCallback) { // Constructor updated
        this.repaintCallback = repaintCallback;
    }

    public boolean isGridVisible() { // Renamed from isShowGrid
        return gridVisible;
    }

    public void setGridVisible(boolean gridVisible) { // Renamed from setShowGrid
        this.gridVisible = gridVisible;
        if (repaintCallback != null) {
            repaintCallback.run();
        }
    }

    public int getGridSpacingWidth() {
        return gridSpacingWidth;
    }

    public void setGridSpacingWidth(int gridSpacingWidth) {
        if (gridSpacingWidth > 0) {
            this.gridSpacingWidth = gridSpacingWidth;
            if (repaintCallback != null) {
                repaintCallback.run();
            }
        }
    }

    public int getGridSpacingHeight() {
        return gridSpacingHeight;
    }

    public void setGridSpacingHeight(int gridSpacingHeight) {
        if (gridSpacingHeight > 0) {
            this.gridSpacingHeight = gridSpacingHeight;
            if (repaintCallback != null) {
                repaintCallback.run();
            }
        }
    }

    public int getGridWidth() { // Added getter
        return gridSpacingWidth;
    }

    public int getGridHeight() { // Added getter
        return gridSpacingHeight;
    }

    public Color getGridColor() {
        return gridColor;
    }

    public void setGridColor(Color gridColor) {
        this.gridColor = gridColor;
        if (repaintCallback != null) {
            repaintCallback.run();
        }
    }

    public void resetGridToDefaults() { // Ensure this is the correct name
        this.gridSpacingWidth = DEFAULT_GRID_SPACING_WIDTH;
        this.gridSpacingHeight = DEFAULT_GRID_SPACING_HEIGHT;
        this.gridColor = DEFAULT_GRID_COLOR;
        // gridVisible is not reset by default, only its properties
        if (repaintCallback != null) {
            repaintCallback.run();
        }
    }

    public void drawGrid(Graphics g, int canvasWidth, int canvasHeight) {
        if (!gridVisible) { // Used renamed field
            return;
        }
        g.setColor(gridColor);
        for (int x = 0; x < canvasWidth; x += gridSpacingWidth) {
            g.drawLine(x, 0, x, canvasHeight);
        }
        for (int y = 0; y < canvasHeight; y += gridSpacingHeight) {
            g.drawLine(0, y, canvasWidth, y);
        }
    }

    // Method to snap a point to the grid
    public Point snapToGrid(Point p, boolean snapActive) {
        if (!snapActive || gridSpacingWidth <= 0 || gridSpacingHeight <= 0) {
            return p; // Return original point if snap is not active or grid spacing is invalid
        }
        int snappedX = Math.round((float) p.x / gridSpacingWidth) * gridSpacingWidth;
        int snappedY = Math.round((float) p.y / gridSpacingHeight) * gridSpacingHeight;
        return new Point(snappedX, snappedY);
    }

    // Re-added showGridOptionsDialog method
    public void showGridOptionsDialog(Component parent) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Grid Options", true);
        dialog.setLayout(new BorderLayout(10, 10));

        // Temporary variables to hold dialog values until "OK" is pressed
        int tempGridSpacingWidth = getGridSpacingWidth();
        int tempGridSpacingHeight = getGridSpacingHeight();
        Color tempGridColor = getGridColor();

        // Panel for spinners
        JPanel spinnersPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        spinnersPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        spinnersPanel.add(new JLabel("Grid Width:"));
        JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(tempGridSpacingWidth, 1, 500, 1));
        spinnersPanel.add(widthSpinner);
        spinnersPanel.add(new JLabel("Grid Height:"));
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(tempGridSpacingHeight, 1, 500, 1));
        spinnersPanel.add(heightSpinner);

        // Panel for color chooser
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        colorPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        JButton colorButton = new JButton("Choose Grid Color");
        JPanel colorPreview = new JPanel();
        colorPreview.setPreferredSize(new Dimension(20, 20));
        colorPreview.setBackground(tempGridColor);
        colorPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        colorButton.addActionListener(e -> {
            Color chosenColor = JColorChooser.showDialog(dialog, "Choose Grid Color", colorPreview.getBackground());
            if (chosenColor != null) {
                colorPreview.setBackground(chosenColor);
            }
        });
        colorPanel.add(colorButton);
        colorPanel.add(colorPreview);

        // Live preview checkbox (optional, could be more complex to implement fully live)
        // For now, changes apply on OK.

        // Panel for buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        JButton resetButton = new JButton("Reset to Defaults");

        okButton.addActionListener(e -> {
            setGridSpacingWidth((Integer) widthSpinner.getValue());
            setGridSpacingHeight((Integer) heightSpinner.getValue());
            setGridColor(colorPreview.getBackground());
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        resetButton.addActionListener(e -> {
            widthSpinner.setValue(DEFAULT_GRID_SPACING_WIDTH);
            heightSpinner.setValue(DEFAULT_GRID_SPACING_HEIGHT);
            colorPreview.setBackground(DEFAULT_GRID_COLOR);
            // These changes are only in the dialog until OK is pressed
        });

        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);
        buttonsPanel.add(resetButton);

        // Main content panel
        JPanel contentPanel = new JPanel(new BorderLayout(5,5));
        contentPanel.add(spinnersPanel, BorderLayout.CENTER);
        contentPanel.add(colorPanel, BorderLayout.SOUTH);


        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonsPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
