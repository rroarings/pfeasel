package paintcomponents;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Color;
import java.io.Serializable; // Added import

/**
 * Interface for all drawable elements on the canvas.
 */
public interface PaintElement extends Serializable { // Added extends Serializable
    /**
     * Draws the element on the canvas.
     * @param g2d The Graphics2D context to draw on.
     */
    void draw(Graphics2D g2d);

    /**
     * Checks if the element contains the specified point.
     * @param p The point to check.
     * @return True if the element contains the point, false otherwise.
     */
    boolean contains(Point p);

    /**
     * Sets the position of the element.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     */
    void setPosition(int x, int y);

    /**
     * Gets the position of the element.
     * @return The position as a Point object.
     */
    Point getPosition();

    /**
     * Draws the shadow of the element on the canvas.
     * @param g2d The Graphics2D context to draw on.
     * @param shadowColor The color of the shadow.
     * @param shadowXOffset The x offset of the shadow.
     * @param shadowYOffset The y offset of the shadow.
     */
    void drawShadow(Graphics2D g2d, Color shadowColor, int shadowXOffset, int shadowYOffset);

    /**
     * Checks if the element has a shadow.
     * @return True if the element has a shadow, false otherwise.
     */
    boolean hasShadow();

    /**
     * Sets whether the element has a shadow.
     * @param hasShadow True to enable shadow, false to disable.
     */
    void setShadow(boolean hasShadow);

    /**
     * Gets the name of the element, typically for layer identification.
     * @return The name of the element.
     */
    String getName();

    /**
     * Creates a duplicate of the element.
     * @return A new PaintElement that is a duplicate of the current element.
     */
    PaintElement duplicate();

    /**
     * Gets the display name of the element.
     * @return The display name of the element.
     */
    String getDisplayName();

    /**
     * Sets the display name of the element.
     * @param name The display name to set.
     */
    void setDisplayName(String name);

    /**
     * Gets the bounding rectangle of the element.
     * @return A Rectangle object representing the bounds.
     */
    java.awt.Rectangle getBounds();
}
