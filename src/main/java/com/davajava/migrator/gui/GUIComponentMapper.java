package com.davajava.migrator.gui;

import java.util.HashMap;
import java.util.Map;

public class GUIComponentMapper {
    private final Map<String, String> tkinterToSwing;
    private final Map<String, String> wpfToJavaFX;
    private final Map<String, String> win32ToAWT;

    public GUIComponentMapper() {
        this.tkinterToSwing = initializeTkinterMapping();
        this.wpfToJavaFX = initializeWPFMapping();
        this.win32ToAWT = initializeWin32Mapping();
    }

    private Map<String, String> initializeTkinterMapping() {
        Map<String, String> mapping = new HashMap<>();
        
        // Basic components
        mapping.put("Tk", "JFrame");
        mapping.put("Frame", "JPanel");
        mapping.put("Label", "JLabel");
        mapping.put("Button", "JButton");
        mapping.put("Entry", "JTextField");
        mapping.put("Text", "JTextArea");
        mapping.put("Listbox", "JList");
        mapping.put("Canvas", "JPanel");
        mapping.put("Menu", "JMenuBar");
        mapping.put("Menubutton", "JMenu");
        mapping.put("Checkbutton", "JCheckBox");
        mapping.put("Radiobutton", "JRadioButton");
        mapping.put("Scale", "JSlider");
        mapping.put("Scrollbar", "JScrollBar");
        
        // Layout managers
        mapping.put("pack", "add");
        mapping.put("grid", "setLayout(new GridLayout())");
        mapping.put("place", "setBounds");
        
        return mapping;
    }

    private Map<String, String> initializeWPFMapping() {
        Map<String, String> mapping = new HashMap<>();
        
        // Basic components
        mapping.put("Window", "Stage");
        mapping.put("Button", "Button");
        mapping.put("Label", "Label");
        mapping.put("TextBox", "TextField");
        mapping.put("TextBlock", "Text");
        mapping.put("ListView", "ListView");
        mapping.put("Canvas", "Pane");
        mapping.put("Grid", "GridPane");
        mapping.put("StackPanel", "VBox");
        mapping.put("CheckBox", "CheckBox");
        mapping.put("RadioButton", "RadioButton");
        mapping.put("Slider", "Slider");
        mapping.put("ScrollBar", "ScrollBar");
        
        return mapping;
    }

    private Map<String, String> initializeWin32Mapping() {
        Map<String, String> mapping = new HashMap<>();
        
        // Basic components
        mapping.put("CreateWindow", "new JFrame");
        mapping.put("BUTTON", "JButton");
        mapping.put("STATIC", "JLabel");
        mapping.put("EDIT", "JTextField");
        mapping.put("LISTBOX", "JList");
        
        return mapping;
    }

    public String mapComponent(GUIFramework sourceFramework, String componentName) {
        switch (sourceFramework) {
            case TKINTER:
                return tkinterToSwing.getOrDefault(componentName, "JComponent");
            case WPF:
                return wpfToJavaFX.getOrDefault(componentName, "Node");
            case WIN32:
                return win32ToAWT.getOrDefault(componentName, "Component");
            default:
                return "Object";
        }
    }

    public String generateImports(String targetFramework) {
        switch (targetFramework) {
            case "javax.swing":
                return "import javax.swing.*;\nimport java.awt.*;\nimport java.awt.event.*;";
            case "javafx":
                return "import javafx.application.Application;\nimport javafx.scene.*;\nimport javafx.stage.*;\nimport javafx.scene.control.*;";
            case "java.awt":
                return "import java.awt.*;\nimport java.awt.event.*;";
            default:
                return "";
        }
    }
}