package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import paintcomponents.PaintElement;

public class ProjectIOService {

    public void save(File targetFile, List<PaintElement> elements) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(targetFile))) {
            oos.writeObject(elements);
        }
    }

    public List<PaintElement> load(File sourceFile) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(sourceFile))) {
            Object obj = ois.readObject();
            if (!(obj instanceof List)) {
                throw new IOException("File does not contain a valid drawing.");
            }

            List<?> loaded = (List<?>) obj;
            List<PaintElement> loadedElements = new ArrayList<>();
            for (Object entry : loaded) {
                if (entry instanceof PaintElement) {
                    loadedElements.add((PaintElement) entry);
                }
            }
            return loadedElements;
        }
    }
}
