package actions;

import app.Main;
import paintcomponents.PaintElement;

public class AddElementAction implements UndoableAction {
    private Main mainApp;
    private PaintElement elementToAdd;
    private int addIndex; // Store the index where the element was added

    public AddElementAction(Main mainApp, PaintElement elementToAdd, int addIndex) {
        this.mainApp = mainApp;
        this.elementToAdd = elementToAdd;
        this.addIndex = addIndex;
    }

    @Override
    public void undo() {
        mainApp.internalRemoveElementFromList(elementToAdd);
        // System.out.println("Undo: Removed element '" + elementToAdd.getDisplayName() + "'");
    }

    @Override
    public void redo() {
        mainApp.internalAddElementToList(elementToAdd, addIndex);
        // System.out.println("Redo: Added element '" + elementToAdd.getDisplayName() + "' at index " + addIndex);
    }

    @Override
    public String getActionName() {
        return "Add Element: " + elementToAdd.getDisplayName();
    }
}
