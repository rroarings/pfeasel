# PFeasel Paint Creator

## Description
PFeasel Paint Creator is a Java Swing-based 2D vector graphics application. It allows users to draw various shapes, add text and images, manipulate these elements on a canvas, and manage them through a layer system.

## Features
* **Drawing Tools**: Rectangle, Rounded Rectangle, Circle, Line, Polygon, Text.
* **Image Support**: Load images from local files or URLs onto the canvas.
* **Element Manipulation**:
    * Select, Move, Duplicate, Delete, Rename elements.
    * Reorder elements (bring to front, send to back, etc.) via a layers list.
* **Properties Toolbox**:
    * Select active drawing tool.
    * Control fill color, stroke color, and stroke width.
    * Adjust font properties (family, size, bold, italic, underline) for text elements.
    * Set arc dimensions for rounded rectangles.
    * Configure shadow properties (enable, color, offset X/Y).
    * Manage layers (view list, reorder, duplicate, rename, delete).
* **Main Drawing Panel**:
    * Canvas for drawing and interacting with elements.
    * Status bar displaying mouse coordinates and last action performed.
* **File Operations**:
    * New (prompts to save unsaved changes).
    * Open (supports `.pfd` custom format).
    * Save (supports `.pfd` custom format).
    * Save As... (supports `.pfd` custom format).
* **Visual Aids**:
    * Toggleable grid display.
    * Snap-to-grid functionality.
    * Toggleable "RS Interface" overlay.
    * Toggleable anti-aliasing for smoother rendering.
* **Undo/Redo**: Comprehensive undo/redo support for most drawing and manipulation actions.
* **Generate Code**: Generate vanilla Java2D code for your drawing (see File > Generate Code).

## How to Build and Run

### Prerequisites
* Java Development Kit (JDK) 11 or newer.

### Steps

1. **Clone the Repository**:
    ```bash
    git clone <repository-url>
    cd pfeasel-paint-creator
    ```
2. **Open in IDE**:
    * Open the project directory in your preferred Java IDE (e.g., IntelliJ IDEA, VS Code with Java Extension Pack, Eclipse).
3. **Run**:
    * Locate and run the `main` method in `src/app/Main.java`.


## How to Use

* Select drawing tools from the toolbox window (opens to the right of the main window).
* Adjust element properties (colors, stroke, font, shadow, etc.) using the controls in the toolbox.
* Draw elements directly on the main canvas.
* Manage drawing elements as layers using the "Layers" list in the toolbox.
* Use the "File" menu for creating new drawings, opening existing ones, or saving your work.
* Use the "Edit" menu for Undo and Redo actions.
* Toggle visual aids like the grid or anti-aliasing via the "View" menu in the toolbox.
* Use "Generate Code..." from the File menu to export Java2D code for your drawing.

## File Format

* Drawings are saved in a custom `.pfd` format.
* Only open `.pfd` files created by this application.

## Dependencies

* Java Development Kit (JDK) 11 or newer.
* No external libraries required.

## Future Enhancements

* More advanced shape tools (e.g., freehand drawing, bezier curves).
* Multi-element selection and group operations.
* Zoom and pan capabilities for the canvas.
* Export drawings to common image formats (e.g., PNG, JPG, SVG).
* Gradient fills and more advanced styling options.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.

## License

[MIT License](LICENSE)

---

*For questions or support, please open an issue on GitHub.*