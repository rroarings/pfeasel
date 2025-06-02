package actions;

import paintcomponents.PaintElement;
import java.io.Serializable;
// Import the Main class (adjust the package if needed)
// import main.Main; // <-- Update this import to the correct package if needed, or uncomment and fix below

// Example: If Main is in the default package, use:
// import Main;

// Example: If Main is in 'app' package, use:
// import app.Main;
import app.Main;

public class AddElementAction implements UndoableAction, Serializable {
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
