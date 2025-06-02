package app;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;

public class GeneratedCodeDialog extends JDialog {
    private JTextArea importsArea;
    private JTextArea codeArea;

    public GeneratedCodeDialog(Frame owner, String title, boolean modal, String importsText, String codeText) {
        super(owner, title, modal);
        initComponents(importsText, codeText);
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void initComponents(String importsText, String codeText) {
        setLayout(new BorderLayout(10, 10));
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel textAreasPanel = new JPanel(new BorderLayout(10,10)); // Use BorderLayout for more control

        // Imports Area
        JPanel importsPanel = new JPanel(new BorderLayout(5,5));
        importsPanel.setBorder(BorderFactory.createTitledBorder("Needed Imports"));
        importsArea = new JTextArea(importsText, 4, 70); // Adjusted rows and columns
        importsArea.setEditable(false);
        importsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane importsScrollPane = new JScrollPane(importsArea);
        importsPanel.add(importsScrollPane, BorderLayout.CENTER);
        JButton copyImportsButton = new JButton("Copy Imports to Clipboard");
        copyImportsButton.addActionListener(e -> copyToClipboard(importsArea.getText(), "Imports"));
        importsPanel.add(copyImportsButton, BorderLayout.SOUTH);
        textAreasPanel.add(importsPanel, BorderLayout.NORTH);

        // Code Area
        JPanel codePanel = new JPanel(new BorderLayout(5,5));
        codePanel.setBorder(BorderFactory.createTitledBorder("Generated Code (onRepaint method & declarations)"));
        codeArea = new JTextArea(codeText, 20, 70); // Adjusted rows and columns
        codeArea.setEditable(false);
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane codeScrollPane = new JScrollPane(codeArea);
        codePanel.add(codeScrollPane, BorderLayout.CENTER);
        JButton copyCodeButton = new JButton("Copy Code to Clipboard");
        copyCodeButton.addActionListener(e -> copyToClipboard(codeArea.getText(), "Code"));
        codePanel.add(copyCodeButton, BorderLayout.SOUTH);
        textAreasPanel.add(codePanel, BorderLayout.CENTER);

        add(textAreasPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> setVisible(false));
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void copyToClipboard(String text, String type) {
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
        JOptionPane.showMessageDialog(this, type + " copied to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}
