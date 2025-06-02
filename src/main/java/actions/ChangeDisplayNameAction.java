package actions;
import app.Main;
import paintcomponents.PaintElement;

public class ChangeDisplayNameAction implements UndoableAction {
    private Main mainApp;
    private PaintElement element;
    private String oldDisplayName;
    private String newDisplayName;
    private int originalPaintElementIndex; // Store the original index in paintElements

    public ChangeDisplayNameAction(Main mainApp, PaintElement element, String oldDisplayName, String newDisplayName, int originalPaintElementIndex) {
        this.mainApp = mainApp;
        this.element = element;
        this.oldDisplayName = oldDisplayName;
        this.newDisplayName = newDisplayName;
        this.originalPaintElementIndex = originalPaintElementIndex;
    }

    @Override
    public void undo() {
        mainApp.internalSetPaintElementDisplayName(element, oldDisplayName, originalPaintElementIndex);
        // System.out.println("Undo: Changed display name of '\" + newDisplayName + \"' back to '\" + oldDisplayName + \"' for element at original index " + originalPaintElementIndex);
    }

    @Override
    public void redo() {
        mainApp.internalSetPaintElementDisplayName(element, newDisplayName, originalPaintElementIndex);
        // System.out.println("Redo: Changed display name of '\" + oldDisplayName + \"' to '\" + newDisplayName + \"' for element at original index " + originalPaintElementIndex);
    }

    @Override
    public String getActionName() {
        return "Rename: " + oldDisplayName + " -> " + newDisplayName;
    }
}
