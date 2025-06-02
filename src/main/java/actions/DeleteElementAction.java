package actions;

import paintcomponents.PaintElement;
import java.io.Serializable;

import app.Main;

public class DeleteElementAction implements UndoableAction, Serializable {
    private Main mainApp;
    private PaintElement elementToRemove;
    private int originalIndex;

    public DeleteElementAction(Main mainApp, PaintElement elementToRemove, int originalIndex) {
        this.mainApp = mainApp;
        this.elementToRemove = elementToRemove;
        this.originalIndex = originalIndex;
    }

    @Override
    public void undo() {
        mainApp.internalAddElementToList(elementToRemove, originalIndex);
        // System.out.println("Undo: Re-added element '" + elementToRemove.getDisplayName() + "' at index " + originalIndex);
    }

    @Override
    public void redo() {
        mainApp.internalRemoveElementFromList(elementToRemove); // or by index if more robust
        // System.out.println("Redo: Removed element '" + elementToRemove.getDisplayName() + "'");
    }

    // Ensure this matches the interface exactly
    @Override
    public String getActionName() {
        return "Delete Element: " + elementToRemove.getDisplayName();
    }
}
