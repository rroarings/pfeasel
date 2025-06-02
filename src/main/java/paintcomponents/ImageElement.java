package paintcomponents;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.imageio.ImageIO;

public class ImageElement implements PaintElement {
    private static final long serialVersionUID = 1L;

    private transient BufferedImage image;
    private Point position;
    private String imagePath;
    private String displayName;
    private boolean hasShadow = false;

    public ImageElement(BufferedImage image, Point position, String imagePath, String displayName) {
        this.image = image;
        this.position = new Point(position);
        this.imagePath = imagePath;
        this.displayName = displayName;
    }

    public BufferedImage getImage() {
        return image;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void paint(Graphics g, Point offset) {
        if (image != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            if (offset != null) {
                g2d.translate(offset.x, offset.y);
            }
            g2d.drawImage(image, position.x, position.y, null);
            g2d.dispose();
        }
    }

    @Override
    public void draw(Graphics2D g2d) {
        if (image != null) {
            if (hasShadow()) {
                // Shadow logic can be implemented here
            }
            g2d.drawImage(image, position.x, position.y, null);
        }
    }

    @Override
    public boolean contains(Point p) {
        if (image == null) return false;
        Rectangle bounds = getBounds();
        return bounds.contains(p);
    }

    @Override
    public Rectangle getBounds() {
        if (image != null) {
            return new Rectangle(position.x, position.y, image.getWidth(), image.getHeight());
        }
        return new Rectangle(position.x, position.y, 0, 0);
    }

    @Override
    public void setPosition(int x, int y) {
        this.position.setLocation(x, y);
    }

    @Override
    public Point getPosition() {
        return new Point(this.position);
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getName() {
        return displayName;
    }

    @Override
    public PaintElement duplicate() {
        BufferedImage newImage = null;
        if (this.image != null) {
            newImage = new BufferedImage(this.image.getWidth(), this.image.getHeight(), this.image.getType());
            Graphics2D g = newImage.createGraphics();
            g.drawImage(this.image, 0, 0, null);
            g.dispose();
        }
        ImageElement duplicate = new ImageElement(newImage, new Point(this.position.x + 10, this.position.y + 10), this.imagePath, this.displayName + " (copy)");
        duplicate.setShadow(this.hasShadow());
        return duplicate;
    }

    @Override
    public void drawShadow(Graphics2D g2d, Color shadowColor, int shadowXOffset, int shadowYOffset) {
        if (image != null && hasShadow) {
            g2d.drawImage(image, position.x + shadowXOffset, position.y + shadowYOffset, null);
        }
    }

    @Override
    public boolean hasShadow() {
        return hasShadow;
    }

    @Override
    public void setShadow(boolean hasShadow) {
        this.hasShadow = hasShadow;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        if (image != null) {
            ImageIO.write(image, "png", oos);
        } else {
            oos.writeObject(null);
        }
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        this.image = ImageIO.read(ois);
    }
}
