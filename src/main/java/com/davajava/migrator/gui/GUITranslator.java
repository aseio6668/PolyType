package com.davajava.migrator.gui;

import com.davajava.migrator.core.ast.ASTNode;
import com.davajava.migrator.core.ast.FunctionCallNode;
import com.davajava.migrator.core.ast.ClassDeclarationNode;
import com.davajava.migrator.core.ast.VariableDeclarationNode;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class GUITranslator {
    private final GUIComponentMapper mapper;
    private GUIFramework detectedFramework;
    private final StringBuilder output;

    public GUITranslator() {
        this.mapper = new GUIComponentMapper();
        this.output = new StringBuilder();
    }

    public String translateGUICode(String sourceCode, List<String> imports) {
        // Detect GUI framework from imports
        detectedFramework = detectGUIFramework(imports);
        
        if (detectedFramework == null) {
            return null; // Not a GUI application
        }

        output.setLength(0);
        
        // Add appropriate imports for target framework
        String targetFramework = detectedFramework.getTargetFramework();
        output.append(mapper.generateImports(targetFramework)).append("\n\n");
        
        // Generate GUI application boilerplate
        generateApplicationBoilerplate(targetFramework);
        
        // Translate GUI-specific patterns
        translateGUIPatterns(sourceCode);
        
        return output.toString();
    }

    private GUIFramework detectGUIFramework(List<String> imports) {
        for (String importStatement : imports) {
            GUIFramework framework = GUIFramework.detectFromImport(importStatement);
            if (framework != null) {
                return framework;
            }
        }
        return null;
    }

    private void generateApplicationBoilerplate(String targetFramework) {
        switch (targetFramework) {
            case "javax.swing":
                output.append("public class GUIApplication extends JFrame {\n");
                output.append("    public GUIApplication() {\n");
                output.append("        initializeComponents();\n");
                output.append("    }\n\n");
                output.append("    private void initializeComponents() {\n");
                output.append("        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);\n");
                output.append("        setTitle(\"Migrated GUI Application\");\n");
                output.append("        // TODO: Add components here\n");
                break;
                
            case "javafx":
                output.append("public class GUIApplication extends Application {\n");
                output.append("    @Override\n");
                output.append("    public void start(Stage primaryStage) {\n");
                output.append("        primaryStage.setTitle(\"Migrated GUI Application\");\n");
                output.append("        // TODO: Add components here\n");
                output.append("        primaryStage.show();\n");
                output.append("    }\n\n");
                output.append("    public static void main(String[] args) {\n");
                output.append("        launch(args);\n");
                output.append("    }\n");
                break;
                
            case "java.awt":
                output.append("public class GUIApplication extends Frame {\n");
                output.append("    public GUIApplication() {\n");
                output.append("        initializeComponents();\n");
                output.append("    }\n\n");
                output.append("    private void initializeComponents() {\n");
                output.append("        setTitle(\"Migrated GUI Application\");\n");
                output.append("        // TODO: Add components here\n");
                break;
        }
    }

    private void translateGUIPatterns(String sourceCode) {
        // Pattern for Tkinter widget creation: widget_name = Widget(parent, options)
        Pattern widgetPattern = Pattern.compile("(\\w+)\\s*=\\s*(\\w+)\\(([^)]*)\\)");
        Matcher matcher = widgetPattern.matcher(sourceCode);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String widgetType = matcher.group(2);
            String options = matcher.group(3);

            String javaComponent = mapper.mapComponent(detectedFramework, widgetType);
            
            output.append("        ").append(javaComponent).append(" ").append(variableName);
            output.append(" = new ").append(javaComponent).append("();\n");
            
            // Translate common options
            if (options.contains("text=")) {
                String text = extractOption(options, "text");
                output.append("        ").append(variableName).append(".setText(").append(text).append(");\n");
            }
            
            if (options.contains("command=")) {
                String command = extractOption(options, "command");
                output.append("        ").append(variableName).append(".addActionListener(e -> ").append(command).append("());\n");
            }
        }

        // Pattern for pack/grid operations
        Pattern packPattern = Pattern.compile("(\\w+)\\.pack\\(\\)");
        Matcher packMatcher = packPattern.matcher(sourceCode);
        while (packMatcher.find()) {
            String widgetName = packMatcher.group(1);
            output.append("        add(").append(widgetName).append(");\n");
        }
    }

    private String extractOption(String options, String optionName) {
        Pattern optionPattern = Pattern.compile(optionName + "\\s*=\\s*([^,)]+)");
        Matcher matcher = optionPattern.matcher(options);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "\"\"";
    }

    public boolean isGUIApplication(List<String> imports) {
        return detectGUIFramework(imports) != null;
    }

    public GUIFramework getDetectedFramework() {
        return detectedFramework;
    }
}