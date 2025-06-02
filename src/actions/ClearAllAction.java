package actions;

import app.Main;
import paintcomponents.PaintElement;
import java.util.List;
import java.util.ArrayList;

public class ClearAllAction implements UndoableAction {
    private Main mainApp;
    private List<PaintElement> originalElements;

    public ClearAllAction(Main mainApp, List<PaintElement> elementsToClear) {
        this.mainApp = mainApp;
        // Deep copy to preserve the state at the time of action
        this.originalElements = new ArrayList<>(elementsToClear);
    }

    @Override
    public void undo() {
        mainApp.internalRestoreElementsList(originalElements);
        // System.out.println("Undo: Restored all elements (" + originalElements.size() + " elements)");
    }

    @Override
    public void redo() {
        mainApp.internalClearAllElements();
        // System.out.println("Redo: Cleared all elements");
    }

    @Override
    public String getActionName() {
        return "Clear All Elements";
    }
}
