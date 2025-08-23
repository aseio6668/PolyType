package com.davajava.migrator.parser.javascript;

import com.davajava.migrator.core.ParseException;
import com.davajava.migrator.core.Parser;
import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.ast.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

public class JavaScriptParser implements Parser {
    private static final Logger logger = Logger.getLogger(JavaScriptParser.class.getName());
    
    // JavaScript/ES6+ patterns
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "^\\s*(?:export\\s+)?(?:default\\s+)?class\\s+(\\w+)(?:\\s+extends\\s+(\\w+))?\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "^\\s*(?:export\\s+)?(?:async\\s+)?function\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*\\{",
        Pattern.MULTILINE
    );
    
    private static final Pattern ARROW_FUNCTION_PATTERN = Pattern.compile(
        "^\\s*(?:const|let|var)\\s+(\\w+)\\s*=\\s*(?:async\\s+)?\\(([^)]*)\\)\\s*=>",
        Pattern.MULTILINE
    );
    
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "^\\s*(?:(static|async)\\s+)?(\\w+)\\s*\\(([^)]*)\\)\\s*\\{",
        Pattern.MULTILINE
    );
    
    private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile(
        "^\\s*constructor\\s*\\(([^)]*)\\)\\s*\\{",
        Pattern.MULTILINE
    );
    
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
        "^\\s*(?:(static)\\s+)?(\\w+)\\s*=\\s*([^;\\n]+)",
        Pattern.MULTILINE
    );
    
    private static final Pattern INTERFACE_PATTERN = Pattern.compile(
        "^\\s*(?:export\\s+)?interface\\s+(\\w+)(?:\\s+extends\\s+([\\w\\s,<>]+))?\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );

    @Override
    public ASTNode parse(String sourceCode) throws ParseException {
        try {
            sourceCode = preprocessSource(sourceCode);
            ProgramNode program = new ProgramNode(1, 1);
            
            // Parse TypeScript interfaces first (if present)
            Matcher interfaceMatcher = INTERFACE_PATTERN.matcher(sourceCode);
            while (interfaceMatcher.find()) {
                String interfaceName = interfaceMatcher.group(1);
                String extendsTypes = interfaceMatcher.group(2);
                String interfaceBody = interfaceMatcher.group(3);
                
                ClassDeclarationNode interfaceNode = parseInterface(interfaceName, interfaceBody, 
                    getLineNumber(sourceCode, interfaceMatcher.start()));
                program.addChild(interfaceNode);
            }
            
            // Parse ES6 classes
            Matcher classMatcher = CLASS_PATTERN.matcher(sourceCode);
            while (classMatcher.find()) {
                String className = classMatcher.group(1);
                String superClass = classMatcher.group(2);
                String classBody = classMatcher.group(3);
                
                ClassDeclarationNode classNode = parseClass(className, superClass, classBody, 
                    getLineNumber(sourceCode, classMatcher.start()));
                program.addChild(classNode);
            }
            
            // Parse standalone functions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(sourceCode);
            while (functionMatcher.find()) {
                String functionName = functionMatcher.group(1);
                String parameters = functionMatcher.group(2);
                
                List<ParameterNode> paramList = parseParameters(parameters);
                
                FunctionDeclarationNode funcNode = new FunctionDeclarationNode(
                    functionName, "Object", paramList, true, true,
                    getLineNumber(sourceCode, functionMatcher.start()), 1
                );
                
                program.addChild(funcNode);
            }
            
            // Parse arrow functions assigned to variables
            Matcher arrowMatcher = ARROW_FUNCTION_PATTERN.matcher(sourceCode);
            while (arrowMatcher.find()) {
                String functionName = arrowMatcher.group(1);
                String parameters = arrowMatcher.group(2);
                
                List<ParameterNode> paramList = parseParameters(parameters);
                
                FunctionDeclarationNode arrowFunc = new FunctionDeclarationNode(
                    functionName, "Object", paramList, true, true,
                    getLineNumber(sourceCode, arrowMatcher.start()), 1
                );
                
                program.addChild(arrowFunc);
            }
            
            return program;
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse JavaScript source code", e);
        }
    }

    @Override
    public ASTNode parseFile(String filePath) throws IOException, ParseException {
        String content = Files.readString(Paths.get(filePath));
        return parse(content);
    }

    @Override
    public SourceLanguage getSupportedLanguage() {
        return SourceLanguage.JAVASCRIPT;
    }

    @Override
    public boolean canHandle(String fileName) {
        return fileName.endsWith(".js") || fileName.endsWith(".mjs") || fileName.endsWith(".jsx");
    }
    
    private String preprocessSource(String sourceCode) {
        // Remove single-line comments
        sourceCode = sourceCode.replaceAll("//.*", "");
        
        // Remove multi-line comments
        sourceCode = sourceCode.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        
        // Remove import/require statements for now
        sourceCode = sourceCode.replaceAll("^\\s*(?:import|const|let|var)\\s+[^\\n]*(?:from|require)[^\\n]*\\n", "");
        
        return sourceCode;
    }
    
    private ClassDeclarationNode parseInterface(String interfaceName, String interfaceBody, int lineNumber) {
        // TypeScript interfaces become abstract classes or interfaces in Java
        ClassDeclarationNode interfaceNode = new ClassDeclarationNode(interfaceName, true, lineNumber, 1);
        
        // Parse interface methods (they become abstract methods)
        Matcher methodMatcher = METHOD_PATTERN.matcher(interfaceBody);
        while (methodMatcher.find()) {
            String modifiers = methodMatcher.group(1);
            String methodName = methodMatcher.group(2);
            String parameters = methodMatcher.group(3);
            
            List<ParameterNode> paramList = parseParameters(parameters);
            
            FunctionDeclarationNode method = new FunctionDeclarationNode(
                methodName, "Object", paramList, true, false, lineNumber, 1
            );
            interfaceNode.addChild(method);
        }
        
        return interfaceNode;
    }
    
    private ClassDeclarationNode parseClass(String className, String superClass, String classBody, int lineNumber) {
        ClassDeclarationNode classNode = new ClassDeclarationNode(className, true, lineNumber, 1);
        
        // Parse constructor
        Matcher constructorMatcher = CONSTRUCTOR_PATTERN.matcher(classBody);
        if (constructorMatcher.find()) {
            String parameters = constructorMatcher.group(1);
            List<ParameterNode> paramList = parseParameters(parameters);
            
            FunctionDeclarationNode constructor = new FunctionDeclarationNode(
                className, "void", paramList, true, false, lineNumber, 1
            );
            classNode.addChild(constructor);
        }
        
        // Parse class properties
        Matcher propertyMatcher = PROPERTY_PATTERN.matcher(classBody);
        while (propertyMatcher.find()) {
            String isStatic = propertyMatcher.group(1);
            String propertyName = propertyMatcher.group(2);
            String initializer = propertyMatcher.group(3);
            
            // Infer type from initializer or default to Object
            String propertyType = inferTypeFromValue(initializer);
            
            VariableDeclarationNode property = new VariableDeclarationNode(
                propertyName, propertyType, true, null, lineNumber, 1
            );
            classNode.addChild(property);
            
            // Generate getter/setter for properties
            generatePropertyAccessors(classNode, propertyName, propertyType);
        }
        
        // Parse methods
        Matcher methodMatcher = METHOD_PATTERN.matcher(classBody);
        while (methodMatcher.find()) {
            String modifiers = methodMatcher.group(1);
            String methodName = methodMatcher.group(2);
            String parameters = methodMatcher.group(3);
            
            // Skip constructor (already handled)
            if ("constructor".equals(methodName)) continue;
            
            List<ParameterNode> paramList = parseParameters(parameters);
            boolean isStatic = "static".equals(modifiers);
            boolean isAsync = "async".equals(modifiers);
            
            String returnType = isAsync ? "CompletableFuture<Object>" : "Object";
            
            FunctionDeclarationNode method = new FunctionDeclarationNode(
                methodName, returnType, paramList, true, isStatic, lineNumber, 1
            );
            classNode.addChild(method);
        }
        
        return classNode;
    }
    
    private void generatePropertyAccessors(ClassDeclarationNode classNode, String propertyName, String propertyType) {
        // Getter
        FunctionDeclarationNode getter = new FunctionDeclarationNode(
            "get" + capitalize(propertyName), propertyType, new ArrayList<>(), true, false, 1, 1
        );
        classNode.addChild(getter);
        
        // Setter
        List<ParameterNode> setterParams = new ArrayList<>();
        setterParams.add(new ParameterNode("value", propertyType, false, 1, 1));
        FunctionDeclarationNode setter = new FunctionDeclarationNode(
            "set" + capitalize(propertyName), "void", setterParams, true, false, 1, 1
        );
        classNode.addChild(setter);
    }
    
    private List<ParameterNode> parseParameters(String paramString) {
        List<ParameterNode> parameters = new ArrayList<>();
        
        if (paramString == null || paramString.trim().isEmpty()) {
            return parameters;
        }
        
        String[] params = paramString.split(",");
        for (String param : params) {
            param = param.trim();
            if (param.isEmpty()) continue;
            
            // Handle TypeScript typed parameters: name: type = default
            String name, type = "Object";
            
            if (param.contains(":")) {
                String[] parts = param.split(":");
                name = parts[0].trim();
                if (parts.length > 1) {
                    type = mapJSTypeToJava(parts[1].split("=")[0].trim());
                }
            } else {
                name = param.split("=")[0].trim(); // Remove default value
            }
            
            // Handle destructuring parameters (basic)
            if (name.contains("{") || name.contains("[")) {
                name = "param" + parameters.size(); // Simplified destructuring handling
                type = "Object";
            }
            
            parameters.add(new ParameterNode(name, type, false, 1, 1));
        }
        
        return parameters;
    }
    
    private String inferTypeFromValue(String value) {
        if (value == null) return "Object";
        
        value = value.trim();
        
        if (value.startsWith("\"") || value.startsWith("'") || value.startsWith("`")) {
            return "String";
        } else if (value.matches("\\d+")) {
            return "int";
        } else if (value.matches("\\d*\\.\\d+")) {
            return "double";
        } else if ("true".equals(value) || "false".equals(value)) {
            return "boolean";
        } else if (value.startsWith("[")) {
            return "List";
        } else if (value.startsWith("{")) {
            return "Map";
        } else if ("null".equals(value) || "undefined".equals(value)) {
            return "Object";
        }
        
        return "Object";
    }
    
    private String mapJSTypeToJava(String jsType) {
        jsType = jsType.trim();
        
        switch (jsType) {
            case "string": return "String";
            case "number": return "double";
            case "boolean": return "boolean";
            case "object": return "Object";
            case "any": return "Object";
            case "void": return "void";
            case "undefined": return "Object";
            case "null": return "Object";
            
            // Arrays
            case "string[]": return "String[]";
            case "number[]": return "double[]";
            case "boolean[]": return "boolean[]";
            case "Array<string>": return "List<String>";
            case "Array<number>": return "List<Double>";
            case "Array<boolean>": return "List<Boolean>";
            
            // Common types
            case "Date": return "Date";
            case "Promise": return "CompletableFuture";
            case "Map": return "Map";
            case "Set": return "Set";
            
            default:
                // Handle generic types
                if (jsType.contains("<") && jsType.contains(">")) {
                    return jsType; // Keep generic syntax
                }
                
                // Handle union types (basic)
                if (jsType.contains("|")) {
                    return "Object"; // Union types become Object
                }
                
                // Custom types - assume they're classes
                return jsType;
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private int getLineNumber(String text, int position) {
        int lineNumber = 1;
        for (int i = 0; i < position && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineNumber++;
            }
        }
        return lineNumber;
    }
}