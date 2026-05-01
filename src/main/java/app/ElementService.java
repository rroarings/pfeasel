package app;

import java.util.List;

import paintcomponents.PaintElement;

public class ElementService {

    public boolean isValidIndex(List<PaintElement> elements, int index) {
        return index >= 0 && index < elements.size();
    }

    public int toPaintElementIndex(int paintElementCount, int toolboxIndex) {
        return toolboxIndex;
    }

    public void moveElement(List<PaintElement> elements, int fromIndex, int toIndex) {
        PaintElement element = elements.remove(fromIndex);
        elements.add(toIndex, element);
    }
}
