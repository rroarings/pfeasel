package app;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import paintcomponents.PaintElement;
import paintcomponents.RectangleElement;
import paintcomponents.RoundRectangleElement;
import paintcomponents.CircleElement;
import paintcomponents.LineElement;
import paintcomponents.PolygonElement;
import paintcomponents.TextElement;
import paintcomponents.ImageElement;

import java.util.HashMap;

public class CodeGenerator {
    private static final String INDENT1 = "    ";
    private static final String INDENT2 = INDENT1 + INDENT1;
    private static final String INDENT3 = INDENT2 + INDENT1;
    private static final String NL = "\n";
    private final List<String> imports = new ArrayList<>();
    private final List<String> colorDeclarations = new ArrayList<>();
    private final List<String> strokeDeclarations = new ArrayList<>();
    private final List<String> fontDeclarations = new ArrayList<>();
    private final List<String> pathDeclarations = new ArrayList<>();
    private final Map<Color, String> colorVarNames = new HashMap<>();
    private final Map<Float, String> strokeVarNames = new HashMap<>();
    private final Map<Font, String> fontVarNames = new HashMap<>();
    private int colorCounter = 1;
    private int strokeCounter = 1;
    private int fontCounter = 1;
    private int pathCounter = 1;

    public String generateCode(List<PaintElement> paintElements) {
        imports.clear();
        colorDeclarations.clear();
        strokeDeclarations.clear();
        fontDeclarations.clear();
        pathDeclarations.clear();
        colorVarNames.clear();
        strokeVarNames.clear();
        fontVarNames.clear();
        colorCounter = 1;
        strokeCounter = 1;
        fontCounter = 1;
        pathCounter = 1;

        imports.add("import java.awt.*;");
        imports.add("import java.awt.geom.GeneralPath;");

        StringBuilder drawingCode = new StringBuilder();
        drawingCode.append(NL);
        appendLine(drawingCode, INDENT1, "public void onRepaint(Graphics g1) {");
        appendLine(drawingCode, INDENT2, "Graphics2D g = (Graphics2D)g1;");
        drawingCode.append(NL);

        for (int i = paintElements.size() - 1; i >= 0; i--) {
            PaintElement element = paintElements.get(i);
            if (element != null) {
                generateElementCode(drawingCode, element);
                drawingCode.append(NL);
            }
        }
        appendLine(drawingCode, INDENT1, "}");

        StringBuilder fullCode = new StringBuilder();
        fullCode.append("// START: Code generated using PFeasel Paint Creator").append(NL).append(NL);

        if (!pathDeclarations.isEmpty()) {
            appendLine(fullCode, INDENT1, "private GeneralPath pathFrom(int[] xs, int[] ys) {");
            appendLine(fullCode, INDENT2, "GeneralPath gp = new GeneralPath();");
            appendLine(fullCode, INDENT2, "gp.moveTo(xs[0], ys[0]);");
            appendLine(fullCode, INDENT2, "for(int i = 1; i < xs.length; i++)");
            appendLine(fullCode, INDENT3, "gp.lineTo(xs[i], ys[i]);");
            appendLine(fullCode, INDENT2, "gp.closePath();");
            appendLine(fullCode, INDENT2, "return gp;");
            appendLine(fullCode, INDENT1, "}");
            fullCode.append(NL);
        }
        if (!colorDeclarations.isEmpty()) {
            colorDeclarations.forEach(decl -> appendLine(fullCode, decl));
            fullCode.append(NL);
        }
        if (!strokeDeclarations.isEmpty()) {
            strokeDeclarations.forEach(decl -> appendLine(fullCode, decl));
            fullCode.append(NL);
        }
        if (!fontDeclarations.isEmpty()) {
            fontDeclarations.forEach(decl -> appendLine(fullCode, decl));
            fullCode.append(NL);
        }
        if (!pathDeclarations.isEmpty()) {
            pathDeclarations.forEach(decl -> appendLine(fullCode, decl));
            fullCode.append(NL);
        }
        fullCode.append(drawingCode);
        fullCode.append("// END: Code generated using PFeasel Paint Creator");
        return fullCode.toString();
    }

    public String getImportsText() {
        return String.join("\n", imports);
    }

    private String getColorVarName(Color color) {
        if (color == null) return null;
        return colorVarNames.computeIfAbsent(color, k -> {
            String varName = "color" + colorCounter++;
            colorDeclarations.add(String.format("    private final Color %s = new Color(%d, %d, %d);",
                    varName, color.getRed(), color.getGreen(), color.getBlue()));
            return varName;
        });
    }

    private String getStrokeVarName(float width) {
        return strokeVarNames.computeIfAbsent(width, k -> {
            String varName = "stroke" + strokeCounter++;
            strokeDeclarations.add(String.format("    private final BasicStroke %s = new BasicStroke(%f);",
                    varName, width));
            return varName;
        });
    }

    private String getFontVarName(Font font) {
        if (font == null) return null;
        return fontVarNames.computeIfAbsent(font, k -> {
            String varName = "font" + fontCounter++;
            int style = font.getStyle();
            String styleStr = "Font.PLAIN";
            if ((style & Font.BOLD) != 0 && (style & Font.ITALIC) != 0) styleStr = "Font.BOLD | Font.ITALIC";
            else if ((style & Font.BOLD) != 0) styleStr = "Font.BOLD";
            else if ((style & Font.ITALIC) != 0) styleStr = "Font.ITALIC";
            fontDeclarations.add(String.format("    private final Font %s = new Font(\"%s\", %s, %d);",
                    varName, font.getFamily(), styleStr, font.getSize()));
            return varName;
        });
    }

    private void generateElementCode(StringBuilder code, PaintElement element) {
        if (element instanceof RectangleElement) {
            generateRectangleCode(code, (RectangleElement) element);
        } else if (element instanceof RoundRectangleElement) {
            generateRoundRectCode(code, (RoundRectangleElement) element);
        } else if (element instanceof CircleElement) {
            generateCircleCode(code, (CircleElement) element);
        } else if (element instanceof LineElement) {
            generateLineCode(code, (LineElement) element);
        } else if (element instanceof PolygonElement) {
            generatePolygonCode(code, (PolygonElement) element);
        } else if (element instanceof TextElement) {
            generateTextCode(code, (TextElement) element);
        } else if (element instanceof ImageElement) {
            generateImageCode(code, (ImageElement) element);
        }
    }

    private void generateRectangleCode(StringBuilder code, RectangleElement rect) {
        Color fillColor = rect.getFillColor();
        Color strokeColor = rect.getStrokeColor();
        float strokeWidth = rect.getStrokeWidth();
        boolean fillEnabled = rect.isFillEnabled();
        boolean strokeEnabled = rect.isStrokeEnabled();
        Rectangle bounds = rect.getBounds();
        String fillColorVar = fillEnabled && fillColor != null ? getColorVarName(fillColor) : null;
        String strokeColorVar = strokeEnabled && strokeColor != null ? getColorVarName(strokeColor) : null;
        String strokeVar = strokeEnabled && strokeWidth > 0 ? getStrokeVarName(strokeWidth) : null;
        if (fillColorVar != null) {
            appendLine(code, INDENT2, String.format("g.setColor(%s);", fillColorVar));
            appendLine(code, INDENT2, String.format("g.fillRect(%d, %d, %d, %d);", bounds.x, bounds.y, bounds.width, bounds.height));
        }
        if (strokeColorVar != null && strokeVar != null) {
            appendLine(code, INDENT2, String.format("g.setColor(%s);", strokeColorVar));
            appendLine(code, INDENT2, String.format("g.setStroke(%s);", strokeVar));
            appendLine(code, INDENT2, String.format("g.drawRect(%d, %d, %d, %d);", bounds.x, bounds.y, bounds.width, bounds.height));
        }
    }

    private void generateRoundRectCode(StringBuilder code, RoundRectangleElement rect) {
        Color fillColor = rect.getFillColor();
        Color strokeColor = rect.getStrokeColor();
        float strokeWidth = rect.getStrokeWidth();
        boolean fillEnabled = rect.isFillEnabled();
        boolean strokeEnabled = rect.isStrokeEnabled();
        int arcWidth = rect.getArcWidth();
        int arcHeight = rect.getArcHeight();
        Rectangle bounds = rect.getBounds();
        String fillColorVar = fillEnabled && fillColor != null ? getColorVarName(fillColor) : null;
        String strokeColorVar = strokeEnabled && strokeColor != null ? getColorVarName(strokeColor) : null;
        String strokeVar = strokeEnabled && strokeWidth > 0 ? getStrokeVarName(strokeWidth) : null;
        if (fillColorVar != null) {
            appendLine(code, INDENT2, String.format("g.setColor(%s);", fillColorVar));
            appendLine(code, INDENT2, String.format("g.fillRoundRect(%d, %d, %d, %d, %d, %d);", bounds.x, bounds.y, bounds.width, bounds.height, arcWidth, arcHeight));
        }
        if (strokeColorVar != null && strokeVar != null) {
            appendLine(code, INDENT2, String.format("g.setColor(%s);", strokeColorVar));
            appendLine(code, INDENT2, String.format("g.setStroke(%s);", strokeVar));
            appendLine(code, INDENT2, String.format("g.drawRoundRect(%d, %d, %d, %d, %d, %d);", bounds.x, bounds.y, bounds.width, bounds.height, arcWidth, arcHeight));
        }
    }

    private void generateCircleCode(StringBuilder code, CircleElement circle) {
        Color fillColor = circle.getFillColor();
        Color strokeColor = circle.getStrokeColor();
        float strokeWidth = circle.getStrokeWidth();
        boolean fillEnabled = circle.isFillEnabled();
        boolean strokeEnabled = circle.isStrokeEnabled();
        Rectangle bounds = circle.getBounds();
        String fillColorVar = fillEnabled && fillColor != null ? getColorVarName(fillColor) : null;
        String strokeColorVar = strokeEnabled && strokeColor != null ? getColorVarName(strokeColor) : null;
        String strokeVar = strokeEnabled && strokeWidth > 0 ? getStrokeVarName(strokeWidth) : null;
        if (fillColorVar != null) {
            appendLine(code, INDENT2, String.format("g.setColor(%s);", fillColorVar));
            appendLine(code, INDENT2, String.format("g.fillOval(%d, %d, %d, %d);", bounds.x, bounds.y, bounds.width, bounds.height));
        }
        if (strokeColorVar != null && strokeVar != null) {
            appendLine(code, INDENT2, String.format("g.setColor(%s);", strokeColorVar));
            appendLine(code, INDENT2, String.format("g.setStroke(%s);", strokeVar));
            appendLine(code, INDENT2, String.format("g.drawOval(%d, %d, %d, %d);", bounds.x, bounds.y, bounds.width, bounds.height));
        }
    }

    private void generateLineCode(StringBuilder code, LineElement line) {
        int x1 = line.getX1();
        int y1 = line.getY1();
        int x2 = line.getX2();
        int y2 = line.getY2();
        Color strokeColor = line.getStrokeColor();
        float strokeWidth = line.getStrokeWidth();
        String strokeColorVar = strokeColor != null ? getColorVarName(strokeColor) : null;
        String strokeVar = strokeWidth > 0 ? getStrokeVarName(strokeWidth) : null;
        if (strokeColorVar != null && strokeVar != null) {
            appendLine(code, INDENT2, String.format("g.setColor(%s);", strokeColorVar));
            appendLine(code, INDENT2, String.format("g.setStroke(%s);", strokeVar));
            appendLine(code, INDENT2, String.format("g.drawLine(%d, %d, %d, %d);", x1, y1, x2, y2));
        }
    }

    private void generatePolygonCode(StringBuilder code, PolygonElement poly) {
        Polygon polygon = poly.getPolygon();
        Color fillColor = poly.getFillColor();
        Color strokeColor = poly.getStrokeColor();
        float strokeWidth = poly.getStrokeWidth();
        boolean fillEnabled = poly.isFillEnabled();
        boolean strokeEnabled = poly.isStrokeEnabled();
        if (polygon != null && polygon.npoints >= 3) {
            String pathVar = "polygon" + pathCounter++;
            StringBuilder pathDecl = new StringBuilder();
            pathDecl.append(String.format("    private final GeneralPath %s = pathFrom(new int[]{", pathVar));
            for (int i = 0; i < polygon.npoints; i++) {
                if (i > 0) pathDecl.append(",");
                pathDecl.append(polygon.xpoints[i]);
            }
            pathDecl.append("}, new int[]{");
            for (int i = 0; i < polygon.npoints; i++) {
                if (i > 0) pathDecl.append(",");
                pathDecl.append(polygon.ypoints[i]);
            }
            pathDecl.append("});");
            pathDeclarations.add(pathDecl.toString());
            String fillColorVar = fillEnabled && fillColor != null ? getColorVarName(fillColor) : null;
            String strokeColorVar = strokeEnabled && strokeColor != null ? getColorVarName(strokeColor) : null;
            String strokeVar = strokeEnabled && strokeWidth > 0 ? getStrokeVarName(strokeWidth) : null;
            if (fillColorVar != null) {
                appendLine(code, INDENT2, String.format("g.setColor(%s);", fillColorVar));
                appendLine(code, INDENT2, String.format("g.fill(%s);", pathVar));
            }
            if (strokeColorVar != null && strokeVar != null) {
                appendLine(code, INDENT2, String.format("g.setColor(%s);", strokeColorVar));
                appendLine(code, INDENT2, String.format("g.setStroke(%s);", strokeVar));
                appendLine(code, INDENT2, String.format("g.draw(%s);", pathVar));
            }
        }
    }

    private void generateTextCode(StringBuilder code, TextElement text) {
        String textContent = text.getText();
        Font font = text.getFont();
        Color color = text.getColor();
        Point pos = text.getPosition();
        if (textContent != null && !textContent.isEmpty() && font != null && color != null) {
            String fontVar = getFontVarName(font);
            String colorVar = getColorVarName(color);
            appendLine(code, INDENT2, String.format("g.setFont(%s);", fontVar));
            appendLine(code, INDENT2, String.format("g.setColor(%s);", colorVar));
            appendLine(code, INDENT2, String.format("g.drawString(\"%s\", %d, %d);",
                    textContent.replace("\"", "\\\""), pos.x, pos.y));
        }
    }

    private void generateImageCode(StringBuilder code, ImageElement image) {
        Rectangle bounds = image.getBounds();
        Point pos = image.getPosition();
        appendLine(code, INDENT2, "// Image elements require external resources and cannot be");
        appendLine(code, INDENT2, "// automatically generated. You would need to load the image");
        appendLine(code, INDENT2, "// manually and use g.drawImage() here.");
        appendLine(code, INDENT2, String.format("// Image bounds: x=%d, y=%d, width=%d, height=%d",
                pos.x, pos.y, bounds.width, bounds.height));
        appendLine(code, INDENT2, String.format("// Image path: %s", image.getImagePath()));
    }

    private void appendLine(StringBuilder out, String line) {
        out.append(line).append(NL);
    }

    private void appendLine(StringBuilder out, String indent, String line) {
        out.append(indent).append(line).append(NL);
    }

}
