package paintcomponents;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;

public class TextElement implements PaintElement {
    private String text;
    private int x;
    private int y;
    private Font font;
    private Color color;
    private boolean hasShadow;
    private String displayName;
    private Rectangle bounds; // For contains method, calculated when needed

    public TextElement(String text, int x, int y, Font font, Color color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.font = font;
        this.color = color;
        this.hasShadow = false; // Default shadow state
        // Bounds will be calculated on demand or when properties change
    }

    @Override
    public void draw(Graphics2D g2d) {
        if (text == null || text.isEmpty()) {
            return;
        }
        g2d.setFont(font);
        g2d.setColor(color);
        g2d.drawString(text, x, y);
    }

    @Override
    public void drawShadow(Graphics2D g2d, Color shadowColor, int shadowXOffset, int shadowYOffset) {
        if (hasShadow && text != null && !text.isEmpty() && shadowColor != null) {
            g2d.setFont(font);
            g2d.setColor(shadowColor);
            g2d.drawString(text, x + shadowXOffset, y + shadowYOffset);
        }
    }

    private void calculateBounds(Graphics2D g2d) {
        if (text == null || text.isEmpty() || font == null) {
            this.bounds = new Rectangle();
            return;
        }
        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D stringBounds = font.getStringBounds(text, frc);
        // x, y is the baseline start. stringBounds.getY() is typically negative (ascent).
        this.bounds = new Rectangle(
            x + (int) stringBounds.getX(),
            y + (int) stringBounds.getY(),
            (int) stringBounds.getWidth(),
            (int) stringBounds.getHeight()
        );
    }
    
    // Call this method if Graphics2D is not available for contains check, 
    // e.g. by creating a temporary BufferedImage to get Graphics2D
    public void recacheBounds() {
        // This is a simplified approach. For accurate bounds without a direct g2d,
        // one might need to create a temporary BufferedImage and get its Graphics2D context.
        // Or, store FontMetrics if possible.
        // For now, this might lead to issues if called without a valid g2d context prior.
        // A better approach would be to pass g2d to contains or ensure bounds are fresh.
        // Let's assume for now that contains will have access to a g2d or bounds are fresh.
        // This method is a placeholder for a more robust cache update.
        if (this.font != null && this.text != null && !this.text.isEmpty()) {
             // Create a dummy graphics context to calculate bounds
            java.awt.image.BufferedImage tempImage = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D tempG2d = tempImage.createGraphics();
            tempG2d.setFont(this.font);
            calculateBounds(tempG2d);
            tempG2d.dispose();
        } else {
            this.bounds = new Rectangle();
        }
    }


    @Override
    public boolean contains(Point p) {
        // This is problematic if g2d is not available.
        // For simplicity, we'll assume bounds are up-to-date.
        // A robust solution would pass g2d or ensure recacheBounds is called appropriately.
        if (this.bounds == null) {
            // Fallback: if bounds not calculated, try to calculate them.
            // This requires a Graphics2D context. If we are in a context without one (e.g. mouse click before paint)
            // this will be an issue.
            // For now, we'll rely on bounds being calculated during draw or explicitly.
            // This is a common challenge in Swing component hit detection.
            // A simple solution for now: if bounds are null, consider it not contained.
            // Or, if we absolutely need it, we'd call recacheBounds, but that's inefficient on every check.
            // Let's assume bounds are calculated during draw and are reasonably fresh for move operations.
            // If not, the move tool might not pick up text elements correctly until after a repaint.
            recacheBounds(); // Attempt to calculate bounds if not already set.
        }
        return bounds != null && bounds.contains(p);
    }

    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        if (this.bounds != null) { // If bounds were cached, update their position too
             recacheBounds(); // Recalculate bounds based on new position
        }
    }

    @Override
    public Point getPosition() {
        return new Point(x, y);
    }

    @Override
    public boolean hasShadow() {
        return hasShadow;
    }

    @Override
    public void setShadow(boolean hasShadow) {
        this.hasShadow = hasShadow;
    }

    @Override
    public String getName() {
        // Return the text itself or a truncated version as the base name
        if (text == null) return "Text";
        if (text.length() > 15) return "\"" + text.substring(0, 12) + "...\"";
        return "\"" + text + "\"";
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String name) {
        this.displayName = name;
    }

    // Additional methods if needed, e.g., to update text or font
    public void setText(String text) {
        this.text = text;
        recacheBounds();
    }

    public void setFont(Font font) {
        this.font = font;
        recacheBounds();
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public PaintElement duplicate() {
        TextElement newText = new TextElement(text, x, y, font, color);
        newText.setShadow(this.hasShadow);
        newText.setDisplayName(this.displayName); // Will be updated by caller
        return newText;
    }

    @Override
    public Rectangle getBounds() {
        if (bounds == null) {
            // This might be problematic if called before a draw operation
            // or without a valid Graphics context for recacheBounds.
            // Consider ensuring recacheBounds is called appropriately elsewhere
            // or pass Graphics2D to getBounds if necessary for on-the-fly calculation.
            recacheBounds(); 
        }
        return bounds != null ? new Rectangle(bounds) : new Rectangle(x,y,0,0);
    }
}
