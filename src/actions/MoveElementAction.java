package actions;

import app.Main;
import paintcomponents.PaintElement;
import java.awt.Point;

public class MoveElementAction implements UndoableAction {
    private Main mainApp;
    private PaintElement element;
    private Point oldPosition;
    private Point newPosition;

    public MoveElementAction(Main mainApp, PaintElement element, Point oldPosition, Point newPosition) {
        this.mainApp = mainApp;
        this.element = element;
        this.oldPosition = new Point(oldPosition); // Store copies
        this.newPosition = new Point(newPosition); // Store copies
    }

    @Override
    public void undo() {
        element.setPosition(oldPosition.x, oldPosition.y);
        // If the element moved was the currently selected one, ensure highlight updates
        if (mainApp.getCurrentlySelectedElementInList() == element) {
            mainApp.repaintDrawingPanel();
        } else {
            // If a different element is selected, or no element, still repaint
            mainApp.repaintDrawingPanel();
        }
        // Optionally, re-select the element in the toolbox if its selection was tied to this action
        // For a simple move, usually just repainting is enough.
        // mainApp.selectElementInToolbox(element); // Example if needed
        System.out.println("Undo Move: " + element.getDisplayName() + " to " + oldPosition);
    }

    @Override
    public void redo() {
        element.setPosition(newPosition.x, newPosition.y);
        if (mainApp.getCurrentlySelectedElementInList() == element) {
            mainApp.repaintDrawingPanel();
        } else {
            mainApp.repaintDrawingPanel();
        }
        // mainApp.selectElementInToolbox(element); // Example if needed
        System.out.println("Redo Move: " + element.getDisplayName() + " to " + newPosition);
    }

    @Override
    public String getActionName() {
        return "Move " + element.getDisplayName();
    }
}
