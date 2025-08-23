package com.davajava.migrator.parser.kotlin;

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

public class KotlinParser implements Parser {
    private static final Logger logger = Logger.getLogger(KotlinParser.class.getName());
    
    // Kotlin specific patterns
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
        "^\\s*package\\s+([\\w\\.]+)",
        Pattern.MULTILINE
    );
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "^\\s*(?:(open|abstract|sealed|final|inner)\\s+)?class\\s+(\\w+)(?:\\s*<([^>]+)>)?(?:\\s*\\(([^)]*)\\))?(?:\\s*:\\s*([\\w\\s,<>()]+))?\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern DATA_CLASS_PATTERN = Pattern.compile(
        "^\\s*data\\s+class\\s+(\\w+)(?:\\s*<([^>]+)>)?\\s*\\(([^)]*)\\)",
        Pattern.MULTILINE
    );
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "^\\s*(?:(override|open|final|abstract)\\s+)?fun\\s+(?:<([^>]+)>\\s+)?(\\w+)\\s*\\(([^)]*)\\)(?:\\s*:\\s*([^\\{\\=]+))?(?:\\s*\\{|\\s*=)",
        Pattern.MULTILINE
    );
    
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
        "^\\s*(?:(val|var)\\s+)(\\w+)(?:\\s*:\\s*([^\\=\\n]+))?(?:\\s*=\\s*([^\\n]+))?",
        Pattern.MULTILINE
    );
    
    private static final Pattern EXTENSION_FUNCTION_PATTERN = Pattern.compile(
        "^\\s*(?:(override|open|final)\\s+)?fun\\s+([\\w\\.<>]+)\\.(\\w+)\\s*\\(([^)]*)\\)(?:\\s*:\\s*([^\\{\\=]+))?(?:\\s*\\{|\\s*=)",
        Pattern.MULTILINE
    );

    @Override
    public ASTNode parse(String sourceCode) throws ParseException {
        try {
            sourceCode = preprocessSource(sourceCode);
            ProgramNode program = new ProgramNode(1, 1);
            
            // Parse data classes first (simpler structure)
            Matcher dataClassMatcher = DATA_CLASS_PATTERN.matcher(sourceCode);
            while (dataClassMatcher.find()) {
                String className = dataClassMatcher.group(1);
                String generics = dataClassMatcher.group(2);
                String parameters = dataClassMatcher.group(3);
                
                ClassDeclarationNode dataClass = parseDataClass(className, parameters, 
                    getLineNumber(sourceCode, dataClassMatcher.start()));
                program.addChild(dataClass);
            }
            
            // Parse regular classes
            Matcher classMatcher = CLASS_PATTERN.matcher(sourceCode);
            while (classMatcher.find()) {
                String modifiers = classMatcher.group(1);
                String className = classMatcher.group(2);
                String generics = classMatcher.group(3);
                String constructorParams = classMatcher.group(4);
                String superTypes = classMatcher.group(5);
                String classBody = classMatcher.group(6);
                
                ClassDeclarationNode classNode = parseClass(className, modifiers, constructorParams, 
                    superTypes, classBody, getLineNumber(sourceCode, classMatcher.start()));
                program.addChild(classNode);
            }
            
            // Parse top-level functions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(sourceCode);
            while (functionMatcher.find()) {
                String modifiers = functionMatcher.group(1);
                String generics = functionMatcher.group(2);
                String functionName = functionMatcher.group(3);
                String parameters = functionMatcher.group(4);
                String returnType = functionMatcher.group(5);
                
                returnType = returnType != null ? mapKotlinTypeToJava(returnType.trim()) : "void";
                List<ParameterNode> paramList = parseParameters(parameters);
                
                FunctionDeclarationNode funcNode = new FunctionDeclarationNode(
                    functionName, returnType, paramList, true, true,
                    getLineNumber(sourceCode, functionMatcher.start()), 1
                );
                
                program.addChild(funcNode);
            }
            
            // Parse extension functions
            Matcher extensionMatcher = EXTENSION_FUNCTION_PATTERN.matcher(sourceCode);
            while (extensionMatcher.find()) {
                String modifiers = extensionMatcher.group(1);
                String receiverType = extensionMatcher.group(2);
                String functionName = extensionMatcher.group(3);
                String parameters = extensionMatcher.group(4);
                String returnType = extensionMatcher.group(5);
                
                returnType = returnType != null ? mapKotlinTypeToJava(returnType.trim()) : "void";
                List<ParameterNode> paramList = parseParameters(parameters);
                
                // Add receiver as first parameter for Java static method
                ParameterNode receiver = new ParameterNode("receiver", mapKotlinTypeToJava(receiverType), false, 1, 1);
                paramList.add(0, receiver);
                
                FunctionDeclarationNode extensionFunc = new FunctionDeclarationNode(
                    functionName, returnType, paramList, true, true,
                    getLineNumber(sourceCode, extensionMatcher.start()), 1
                );
                
                program.addChild(extensionFunc);
            }
            
            return program;
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse Kotlin source code", e);
        }
    }

    @Override
    public ASTNode parseFile(String filePath) throws IOException, ParseException {
        String content = Files.readString(Paths.get(filePath));
        return parse(content);
    }

    @Override
    public SourceLanguage getSupportedLanguage() {
        return SourceLanguage.KOTLIN;
    }

    @Override
    public boolean canHandle(String fileName) {
        return fileName.endsWith(".kt") || fileName.endsWith(".kts");
    }
    
    private String preprocessSource(String sourceCode) {
        // Remove comments
        sourceCode = sourceCode.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        sourceCode = sourceCode.replaceAll("//.*", "");
        
        // Remove annotations for simplicity
        sourceCode = sourceCode.replaceAll("@\\w+(?:\\([^)]*\\))?", "");
        
        return sourceCode;
    }
    
    private ClassDeclarationNode parseDataClass(String className, String parameters, int lineNumber) {
        ClassDeclarationNode dataClass = new ClassDeclarationNode(className, true, lineNumber, 1);
        
        // Parse primary constructor parameters as properties
        List<ParameterNode> params = parseParameters(parameters);
        for (ParameterNode param : params) {
            // Data class parameters become fields
            VariableDeclarationNode field = new VariableDeclarationNode(
                param.getName(), param.getDataType(), false, null, lineNumber, 1
            );
            dataClass.addChild(field);
            
            // Generate getter
            FunctionDeclarationNode getter = new FunctionDeclarationNode(
                "get" + capitalize(param.getName()), param.getDataType(), new ArrayList<>(), true, false, lineNumber, 1
            );
            dataClass.addChild(getter);
        }
        
        // Add equals, hashCode, toString methods (Kotlin data class features)
        addDataClassMethods(dataClass, className);
        
        return dataClass;
    }
    
    private ClassDeclarationNode parseClass(String className, String modifiers, String constructorParams, 
                                          String superTypes, String classBody, int lineNumber) {
        ClassDeclarationNode classNode = new ClassDeclarationNode(className, true, lineNumber, 1);
        
        // Parse primary constructor if exists
        if (constructorParams != null && !constructorParams.trim().isEmpty()) {
            List<ParameterNode> params = parseParameters(constructorParams);
            FunctionDeclarationNode constructor = new FunctionDeclarationNode(
                className, "void", params, true, false, lineNumber, 1
            );
            classNode.addChild(constructor);
        }
        
        // Parse class body
        if (classBody != null) {
            parseClassBody(classNode, classBody);
        }
        
        return classNode;
    }
    
    private void parseClassBody(ClassDeclarationNode classNode, String classBody) {
        // Parse properties
        Matcher propertyMatcher = PROPERTY_PATTERN.matcher(classBody);
        while (propertyMatcher.find()) {
            String keyword = propertyMatcher.group(1); // val or var
            String propertyName = propertyMatcher.group(2);
            String propertyType = propertyMatcher.group(3);
            String initializer = propertyMatcher.group(4);
            
            if (propertyType != null) {
                propertyType = mapKotlinTypeToJava(propertyType.trim());
            } else {
                propertyType = "Object"; // Type inference
            }
            
            boolean isMutable = "var".equals(keyword);
            
            VariableDeclarationNode property = new VariableDeclarationNode(
                propertyName, propertyType, isMutable, null, 1, 1
            );
            classNode.addChild(property);
            
            // Generate getter
            FunctionDeclarationNode getter = new FunctionDeclarationNode(
                "get" + capitalize(propertyName), propertyType, new ArrayList<>(), true, false, 1, 1
            );
            classNode.addChild(getter);
            
            // Generate setter for var properties
            if (isMutable) {
                List<ParameterNode> setterParams = new ArrayList<>();
                setterParams.add(new ParameterNode("value", propertyType, false, 1, 1));
                FunctionDeclarationNode setter = new FunctionDeclarationNode(
                    "set" + capitalize(propertyName), "void", setterParams, true, false, 1, 1
                );
                classNode.addChild(setter);
            }
        }
        
        // Parse methods
        Matcher methodMatcher = FUNCTION_PATTERN.matcher(classBody);
        while (methodMatcher.find()) {
            String modifiers = methodMatcher.group(1);
            String generics = methodMatcher.group(2);
            String methodName = methodMatcher.group(3);
            String parameters = methodMatcher.group(4);
            String returnType = methodMatcher.group(5);
            
            returnType = returnType != null ? mapKotlinTypeToJava(returnType.trim()) : "void";
            List<ParameterNode> paramList = parseParameters(parameters);
            
            FunctionDeclarationNode method = new FunctionDeclarationNode(
                methodName, returnType, paramList, true, false, 1, 1
            );
            classNode.addChild(method);
        }
    }
    
    private void addDataClassMethods(ClassDeclarationNode dataClass, String className) {
        // equals method
        List<ParameterNode> equalsParams = new ArrayList<>();
        equalsParams.add(new ParameterNode("other", "Object", false, 1, 1));
        FunctionDeclarationNode equals = new FunctionDeclarationNode(
            "equals", "boolean", equalsParams, true, false, 1, 1
        );
        dataClass.addChild(equals);
        
        // hashCode method
        FunctionDeclarationNode hashCode = new FunctionDeclarationNode(
            "hashCode", "int", new ArrayList<>(), true, false, 1, 1
        );
        dataClass.addChild(hashCode);
        
        // toString method
        FunctionDeclarationNode toString = new FunctionDeclarationNode(
            "toString", "String", new ArrayList<>(), true, false, 1, 1
        );
        dataClass.addChild(toString);
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
            
            // Handle Kotlin parameter format: name: Type = default
            String[] parts = param.split(":");
            if (parts.length >= 2) {
                String name = parts[0].trim();
                String type = parts[1].split("=")[0].trim(); // Remove default value
                
                // Remove val/var keywords
                name = name.replaceAll("^(val|var)\\s+", "");
                
                type = mapKotlinTypeToJava(type);
                parameters.add(new ParameterNode(name, type, false, 1, 1));
            }
        }
        
        return parameters;
    }
    
    private String mapKotlinTypeToJava(String kotlinType) {
        kotlinType = kotlinType.trim();
        
        // Handle nullable types
        if (kotlinType.endsWith("?")) {
            String baseType = kotlinType.substring(0, kotlinType.length() - 1);
            return mapBasicKotlinType(baseType); // In Java, we'll use Optional or regular type with null checks
        }
        
        return mapBasicKotlinType(kotlinType);
    }
    
    private String mapBasicKotlinType(String kotlinType) {
        switch (kotlinType) {
            case "Int": return "int";
            case "Long": return "long";
            case "Short": return "short";
            case "Byte": return "byte";
            case "Double": return "double";
            case "Float": return "float";
            case "Boolean": return "boolean";
            case "Char": return "char";
            case "String": return "String";
            case "Any": return "Object";
            case "Unit": return "void";
            case "Nothing": return "void";
            
            // Collections
            case "List": return "List";
            case "MutableList": return "List";
            case "Set": return "Set";
            case "MutableSet": return "Set";
            case "Map": return "Map";
            case "MutableMap": return "Map";
            case "Array": return "Array";
            
            default:
                // Handle generic types
                if (kotlinType.contains("<") && kotlinType.contains(">")) {
                    return kotlinType; // Keep generics syntax
                }
                
                // Custom types - keep as is
                return kotlinType;
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