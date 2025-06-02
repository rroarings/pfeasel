package app;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(CodeGenerator.class);
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
        drawingCode.append("\n    public void onRepaint(Graphics g1) {\n");
        drawingCode.append("        Graphics2D g = (Graphics2D)g1;\n\n");

        for (int i = paintElements.size() - 1; i >= 0; i--) {
            PaintElement element = paintElements.get(i);
            if (element != null) {
                generateElementCode(drawingCode, element);
                drawingCode.append("\n");
            }
        }
        drawingCode.append("    }\n");

        StringBuilder fullCode = new StringBuilder();
        fullCode.append("// START: Code generated using PFeasel Paint Creator\n\n");

        if (!pathDeclarations.isEmpty()) {
            fullCode.append("    private GeneralPath pathFrom(int[] xs, int[] ys) {\n");
            fullCode.append("        GeneralPath gp = new GeneralPath();\n");
            fullCode.append("        gp.moveTo(xs[0], ys[0]);\n");
            fullCode.append("        for(int i = 1; i < xs.length; i++)\n");
            fullCode.append("            gp.lineTo(xs[i], ys[i]);\n");
            fullCode.append("        gp.closePath();\n");
            fullCode.append("        return gp;\n");
            fullCode.append("    }\n\n");
        }
        if (!colorDeclarations.isEmpty()) {
            colorDeclarations.forEach(decl -> fullCode.append(decl).append("\n"));
            fullCode.append("\n");
        }
        if (!strokeDeclarations.isEmpty()) {
            strokeDeclarations.forEach(decl -> fullCode.append(decl).append("\n"));
            fullCode.append("\n");
        }
        if (!fontDeclarations.isEmpty()) {
            fontDeclarations.forEach(decl -> fullCode.append(decl).append("\n"));
            fullCode.append("\n");
        }
        if (!pathDeclarations.isEmpty()) {
            pathDeclarations.forEach(decl -> fullCode.append(decl).append("\n"));
            fullCode.append("\n");
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
        try {
            java.lang.reflect.Field fillColorField = RectangleElement.class.getDeclaredField("fillColor");
            java.lang.reflect.Field strokeColorField = RectangleElement.class.getDeclaredField("strokeColor");
            java.lang.reflect.Field strokeWidthField = RectangleElement.class.getDeclaredField("strokeWidth");
            java.lang.reflect.Field fillEnabledField = RectangleElement.class.getDeclaredField("fillEnabled");
            java.lang.reflect.Field strokeEnabledField = RectangleElement.class.getDeclaredField("strokeEnabled");
            java.lang.reflect.Field xField = RectangleElement.class.getDeclaredField("x");
            java.lang.reflect.Field yField = RectangleElement.class.getDeclaredField("y");
            java.lang.reflect.Field widthField = RectangleElement.class.getDeclaredField("width");
            java.lang.reflect.Field heightField = RectangleElement.class.getDeclaredField("height");
            fillColorField.setAccessible(true);
            strokeColorField.setAccessible(true);
            strokeWidthField.setAccessible(true);
            fillEnabledField.setAccessible(true);
            strokeEnabledField.setAccessible(true);
            xField.setAccessible(true);
            yField.setAccessible(true);
            widthField.setAccessible(true);
            heightField.setAccessible(true);
            Color fillColor = (Color) fillColorField.get(rect);
            Color strokeColor = (Color) strokeColorField.get(rect);
            float strokeWidth = strokeWidthField.getFloat(rect);
            boolean fillEnabled = fillEnabledField.getBoolean(rect);
            boolean strokeEnabled = strokeEnabledField.getBoolean(rect);
            int x = xField.getInt(rect);
            int y = yField.getInt(rect);
            int width = widthField.getInt(rect);
            int height = heightField.getInt(rect);
            String fillColorVar = fillEnabled && fillColor != null ? getColorVarName(fillColor) : null;
            String strokeColorVar = strokeEnabled && strokeColor != null ? getColorVarName(strokeColor) : null;
            String strokeVar = strokeEnabled && strokeWidth > 0 ? getStrokeVarName(strokeWidth) : null;
            if (fillColorVar != null) {
                code.append(String.format("        g.setColor(%s);\n", fillColorVar));
                code.append(String.format("        g.fillRect(%d, %d, %d, %d);\n", x, y, width, height));
            }
            if (strokeColorVar != null && strokeVar != null) {
                code.append(String.format("        g.setColor(%s);\n", strokeColorVar));
                code.append(String.format("        g.setStroke(%s);\n", strokeVar));
                code.append(String.format("        g.drawRect(%d, %d, %d, %d);\n", x, y, width, height));
            }
        } catch (Exception e) {
            logger.error("Error accessing RectangleElement fields: " + e.getMessage());
        }
    }

    private void generateRoundRectCode(StringBuilder code, RoundRectangleElement rect) {
        try {
            java.lang.reflect.Field fillColorField = RoundRectangleElement.class.getDeclaredField("fillColor");
            java.lang.reflect.Field strokeColorField = RoundRectangleElement.class.getDeclaredField("strokeColor");
            java.lang.reflect.Field strokeWidthField = RoundRectangleElement.class.getDeclaredField("strokeWidth");
            java.lang.reflect.Field fillEnabledField = RoundRectangleElement.class.getDeclaredField("fillEnabled");
            java.lang.reflect.Field strokeEnabledField = RoundRectangleElement.class.getDeclaredField("strokeEnabled");
            java.lang.reflect.Field arcWidthField = RoundRectangleElement.class.getDeclaredField("arcWidth");
            java.lang.reflect.Field arcHeightField = RoundRectangleElement.class.getDeclaredField("arcHeight");
            java.lang.reflect.Field xField = RoundRectangleElement.class.getDeclaredField("x");
            java.lang.reflect.Field yField = RoundRectangleElement.class.getDeclaredField("y");
            java.lang.reflect.Field widthField = RoundRectangleElement.class.getDeclaredField("width");
            java.lang.reflect.Field heightField = RoundRectangleElement.class.getDeclaredField("height");
            fillColorField.setAccessible(true);
            strokeColorField.setAccessible(true);
            strokeWidthField.setAccessible(true);
            fillEnabledField.setAccessible(true);
            strokeEnabledField.setAccessible(true);
            arcWidthField.setAccessible(true);
            arcHeightField.setAccessible(true);
            xField.setAccessible(true);
            yField.setAccessible(true);
            widthField.setAccessible(true);
            heightField.setAccessible(true);
            Color fillColor = (Color) fillColorField.get(rect);
            Color strokeColor = (Color) strokeColorField.get(rect);
            float strokeWidth = strokeWidthField.getFloat(rect);
            boolean fillEnabled = fillEnabledField.getBoolean(rect);
            boolean strokeEnabled = strokeEnabledField.getBoolean(rect);
            int arcWidth = arcWidthField.getInt(rect);
            int arcHeight = arcHeightField.getInt(rect);
            int x = xField.getInt(rect);
            int y = yField.getInt(rect);
            int width = widthField.getInt(rect);
            int height = heightField.getInt(rect);
            String fillColorVar = fillEnabled && fillColor != null ? getColorVarName(fillColor) : null;
            String strokeColorVar = strokeEnabled && strokeColor != null ? getColorVarName(strokeColor) : null;
            String strokeVar = strokeEnabled && strokeWidth > 0 ? getStrokeVarName(strokeWidth) : null;
            if (fillColorVar != null) {
                code.append(String.format("        g.setColor(%s);\n", fillColorVar));
                code.append(String.format("        g.fillRoundRect(%d, %d, %d, %d, %d, %d);\n", x, y, width, height, arcWidth, arcHeight));
            }
            if (strokeColorVar != null && strokeVar != null) {
                code.append(String.format("        g.setColor(%s);\n", strokeColorVar));
                code.append(String.format("        g.setStroke(%s);\n", strokeVar));
                code.append(String.format("        g.drawRoundRect(%d, %d, %d, %d, %d, %d);\n", x, y, width, height, arcWidth, arcHeight));
            }
        } catch (Exception e) {
            logger.error("Error accessing RoundRectangleElement fields: " + e.getMessage());
        }
    }

    private void generateCircleCode(StringBuilder code, CircleElement circle) {
        try {
            java.lang.reflect.Field fillColorField = CircleElement.class.getDeclaredField("fillColor");
            java.lang.reflect.Field strokeColorField = CircleElement.class.getDeclaredField("strokeColor");
            java.lang.reflect.Field strokeWidthField = CircleElement.class.getDeclaredField("strokeWidth");
            java.lang.reflect.Field fillEnabledField = CircleElement.class.getDeclaredField("fillEnabled");
            java.lang.reflect.Field strokeEnabledField = CircleElement.class.getDeclaredField("strokeEnabled");
            java.lang.reflect.Field xField = CircleElement.class.getDeclaredField("x");
            java.lang.reflect.Field yField = CircleElement.class.getDeclaredField("y");
            java.lang.reflect.Field widthField = CircleElement.class.getDeclaredField("width");
            java.lang.reflect.Field heightField = CircleElement.class.getDeclaredField("height");
            fillColorField.setAccessible(true);
            strokeColorField.setAccessible(true);
            strokeWidthField.setAccessible(true);
            fillEnabledField.setAccessible(true);
            strokeEnabledField.setAccessible(true);
            xField.setAccessible(true);
            yField.setAccessible(true);
            widthField.setAccessible(true);
            heightField.setAccessible(true);
            Color fillColor = (Color) fillColorField.get(circle);
            Color strokeColor = (Color) strokeColorField.get(circle);
            float strokeWidth = strokeWidthField.getFloat(circle);
            boolean fillEnabled = fillEnabledField.getBoolean(circle);
            boolean strokeEnabled = strokeEnabledField.getBoolean(circle);
            int x = xField.getInt(circle);
            int y = yField.getInt(circle);
            int width = widthField.getInt(circle);
            int height = heightField.getInt(circle);
            String fillColorVar = fillEnabled && fillColor != null ? getColorVarName(fillColor) : null;
            String strokeColorVar = strokeEnabled && strokeColor != null ? getColorVarName(strokeColor) : null;
            String strokeVar = strokeEnabled && strokeWidth > 0 ? getStrokeVarName(strokeWidth) : null;
            if (fillColorVar != null) {
                code.append(String.format("        g.setColor(%s);\n", fillColorVar));
                code.append(String.format("        g.fillOval(%d, %d, %d, %d);\n", x, y, width, height));
            }
            if (strokeColorVar != null && strokeVar != null) {
                code.append(String.format("        g.setColor(%s);\n", strokeColorVar));
                code.append(String.format("        g.setStroke(%s);\n", strokeVar));
                code.append(String.format("        g.drawOval(%d, %d, %d, %d);\n", x, y, width, height));
            }
        } catch (Exception e) {
            logger.error("Error accessing CircleElement fields: " + e.getMessage());
        }
    }

    private void generateLineCode(StringBuilder code, LineElement line) {
        try {
            java.lang.reflect.Field x1Field = LineElement.class.getDeclaredField("x1");
            java.lang.reflect.Field y1Field = LineElement.class.getDeclaredField("y1");
            java.lang.reflect.Field x2Field = LineElement.class.getDeclaredField("x2");
            java.lang.reflect.Field y2Field = LineElement.class.getDeclaredField("y2");
            java.lang.reflect.Field strokeColorField = LineElement.class.getDeclaredField("strokeColor");
            java.lang.reflect.Field strokeWidthField = LineElement.class.getDeclaredField("strokeWidth");
            x1Field.setAccessible(true);
            y1Field.setAccessible(true);
            x2Field.setAccessible(true);
            y2Field.setAccessible(true);
            strokeColorField.setAccessible(true);
            strokeWidthField.setAccessible(true);
            int x1 = x1Field.getInt(line);
            int y1 = y1Field.getInt(line);
            int x2 = x2Field.getInt(line);
            int y2 = y2Field.getInt(line);
            Color strokeColor = (Color) strokeColorField.get(line);
            float strokeWidth = strokeWidthField.getFloat(line);
            String strokeColorVar = strokeColor != null ? getColorVarName(strokeColor) : null;
            String strokeVar = strokeWidth > 0 ? getStrokeVarName(strokeWidth) : null;
            if (strokeColorVar != null && strokeVar != null) {
                code.append(String.format("        g.setColor(%s);\n", strokeColorVar));
                code.append(String.format("        g.setStroke(%s);\n", strokeVar));
                code.append(String.format("        g.drawLine(%d, %d, %d, %d);\n", x1, y1, x2, y2));
            }
        } catch (Exception e) {
            logger.error("Error accessing LineElement fields: " + e.getMessage());
        }
    }

    private void generatePolygonCode(StringBuilder code, PolygonElement poly) {
        try {
            java.lang.reflect.Field polygonField = PolygonElement.class.getDeclaredField("polygon");
            java.lang.reflect.Field fillColorField = PolygonElement.class.getDeclaredField("fillColor");
            java.lang.reflect.Field strokeColorField = PolygonElement.class.getDeclaredField("strokeColor");
            java.lang.reflect.Field strokeWidthField = PolygonElement.class.getDeclaredField("strokeWidth");
            java.lang.reflect.Field fillEnabledField = PolygonElement.class.getDeclaredField("fillEnabled");
            java.lang.reflect.Field strokeEnabledField = PolygonElement.class.getDeclaredField("strokeEnabled");
            polygonField.setAccessible(true);
            fillColorField.setAccessible(true);
            strokeColorField.setAccessible(true);
            strokeWidthField.setAccessible(true);
            fillEnabledField.setAccessible(true);
            strokeEnabledField.setAccessible(true);
            Polygon polygon = (Polygon) polygonField.get(poly);
            Color fillColor = (Color) fillColorField.get(poly);
            Color strokeColor = (Color) strokeColorField.get(poly);
            float strokeWidth = strokeWidthField.getFloat(poly);
            boolean fillEnabled = fillEnabledField.getBoolean(poly);
            boolean strokeEnabled = strokeEnabledField.getBoolean(poly);
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
                    code.append(String.format("        g.setColor(%s);\n", fillColorVar));
                    code.append(String.format("        g.fill(%s);\n", pathVar));
                }
                if (strokeColorVar != null && strokeVar != null) {
                    code.append(String.format("        g.setColor(%s);\n", strokeColorVar));
                    code.append(String.format("        g.setStroke(%s);\n", strokeVar));
                    code.append(String.format("        g.draw(%s);\n", pathVar));
                }
            }
        } catch (Exception e) {
            logger.error("Error accessing PolygonElement fields: " + e.getMessage());
        }
    }

    private void generateTextCode(StringBuilder code, TextElement text) {
        try {
            java.lang.reflect.Field textField = TextElement.class.getDeclaredField("text");
            java.lang.reflect.Field fontField = TextElement.class.getDeclaredField("font");
            java.lang.reflect.Field colorField = TextElement.class.getDeclaredField("color");
            java.lang.reflect.Field xField = TextElement.class.getDeclaredField("x");
            java.lang.reflect.Field yField = TextElement.class.getDeclaredField("y");
            textField.setAccessible(true);
            fontField.setAccessible(true);
            colorField.setAccessible(true);
            xField.setAccessible(true);
            yField.setAccessible(true);
            String textContent = (String) textField.get(text);
            Font font = (Font) fontField.get(text);
            Color color = (Color) colorField.get(text);
            int x = xField.getInt(text);
            int y = yField.getInt(text);
            if (textContent != null && !textContent.isEmpty() && font != null && color != null) {
                String fontVar = getFontVarName(font);
                String colorVar = getColorVarName(color);
                code.append(String.format("        g.setFont(%s);\n", fontVar));
                code.append(String.format("        g.setColor(%s);\n", colorVar));
                code.append(String.format("        g.drawString(\"%s\", %d, %d);\n",
                        textContent.replace("\"", "\\\""), x, y));
            }
        } catch (Exception e) {
            logger.error("Error accessing TextElement fields: " + e.getMessage());
        }
    }

    private void generateImageCode(StringBuilder code, ImageElement image) {
        Rectangle bounds = image.getBounds();
        Point pos = image.getPosition();
        code.append("        // Image elements require external resources and cannot be\n");
        code.append("        // automatically generated. You would need to load the image\n");
        code.append("        // manually and use g.drawImage() here.\n");
        code.append(String.format("        // Image bounds: x=%d, y=%d, width=%d, height=%d\n",
                pos.x, pos.y, bounds.width, bounds.height));
        code.append(String.format("        // Image path: %s\n", image.getImagePath()));
    }
}
