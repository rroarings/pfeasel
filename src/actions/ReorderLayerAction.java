package actions;

import app.Main;
import paintcomponents.PaintElement;

public class ReorderLayerAction implements UndoableAction {
    private Main mainApp;
    private PaintElement element;
    private int oldIndex;
    private int newIndex;

    public ReorderLayerAction(Main mainApp, PaintElement element, int oldIndex, int newIndex) {
        this.mainApp = mainApp;
        this.element = element;
        this.oldIndex = oldIndex;
        this.newIndex = newIndex;
    }

    @Override
    public void undo() {
        mainApp.internalMoveElementInList(element, newIndex, oldIndex);
        // System.out.println("Undo: Moved element '" + element.getDisplayName() + "' from index " + newIndex + " to " + oldIndex);
    }

    @Override
    public void redo() {
        mainApp.internalMoveElementInList(element, oldIndex, newIndex);
        // System.out.println("Redo: Moved element '" + element.getDisplayName() + "' from index " + oldIndex + " to " + newIndex);
    }

    @Override
    public String getActionName() {
        return "Reorder Layer: " + element.getDisplayName() + " from " + oldIndex + " to " + newIndex;
    }
}
