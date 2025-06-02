package actions;

public interface UndoableAction {
    /**
     * Reverts the action.
     */
    void undo();

    /**
     * Performs or re-performs the action.
     */
    void redo();

    /**
     * Gets a user-friendly description of the action.
     * @return A string describing the action.
     */
    String getActionName();
}
