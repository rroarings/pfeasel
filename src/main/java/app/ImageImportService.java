package app;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.imageio.ImageIO;

public class ImageImportService {

    public BufferedImage loadFromUrlString(String url) throws Exception {
        URL imageUrl = new File(new URI(url)).toURI().toURL();
        BufferedImage image = ImageIO.read(imageUrl);
        if (image == null) {
            throw new IOException("Could not load image from URL.");
        }
        return image;
    }

    public BufferedImage loadFromFile(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Could not load image file.");
        }
        return image;
    }
}
