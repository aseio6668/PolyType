package com.polytype.migrator.parser.swift;

import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.parser.base.Parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Swift source code that converts it to AST representation.
 * Supports Swift's optionals, protocols, structs, classes, and modern language features.
 */
public class SwiftParser implements Parser {
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "(?:(open|public|internal|fileprivate|private)\\s+)?(?:(final)\\s+)?class\\s+(\\w+)(?:\\s*:\\s*([^{]+))?\\s*\\{"
    );
    
    private static final Pattern STRUCT_PATTERN = Pattern.compile(
        "(?:(public|internal|fileprivate|private)\\s+)?struct\\s+(\\w+)(?:\\s*:\\s*([^{]+))?\\s*\\{"
    );
    
    private static final Pattern ENUM_PATTERN = Pattern.compile(
        "(?:(public|internal|fileprivate|private)\\s+)?enum\\s+(\\w+)(?:\\s*:\\s*([^{]+))?\\s*\\{"
    );
    
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile(
        "(?:(public|internal|fileprivate|private)\\s+)?protocol\\s+(\\w+)(?:\\s*:\\s*([^{]+))?\\s*\\{"
    );
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "(?:(open|public|internal|fileprivate|private|static|class)\\s+)?func\\s+(\\w+)(?:<[^>]*>)?\\s*\\([^)]*\\)(?:\\s*(?:async\\s+)?(?:throws\\s+)?->\\s*([^{]+))?\\s*\\{"
    );
    
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
        "(?:(open|public|internal|fileprivate|private|static|class)\\s+)?(?:(var|let)\\s+)(\\w+)\\s*:\\s*([^=\\n{]+?)(?:\\s*=\\s*([^\\n{]+))?(?:\\s*\\{|$)"
    );
    
    private static final Pattern INIT_PATTERN = Pattern.compile(
        "(?:(convenience|required)\\s+)?init(?:\\?|!)?\\s*\\([^)]*\\)(?:\\s*throws)?\\s*\\{"
    );
    
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(
        "(?:(\\w+)\\s+)?(\\w+)\\s*:\\s*([^,)=]+)(?:\\s*=\\s*([^,)]+))?"
    );
    
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
        "import\\s+(?:(\\w+)\\s+)?([\\w.]+)"
    );
    
    @Override
    public ProgramNode parse(String filePath) throws IOException {
        ProgramNode program = new ProgramNode(1, 1);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            parseLines(lines, program);
        }
        
        return program;
    }
    
    private void parseLines(List<String> lines, ProgramNode program) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*")) {
                continue;
            }
            
            // Parse imports
            Matcher importMatcher = IMPORT_PATTERN.matcher(line);
            if (importMatcher.find()) {
                // Handle imports - could be expanded for dependency tracking
                continue;
            }
            
            // Parse class declarations
            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            if (classMatcher.find()) {
                ClassDeclarationNode classNode = parseClass(lines, i, classMatcher);
                program.addChild(classNode);
                continue;
            }
            
            // Parse struct declarations
            Matcher structMatcher = STRUCT_PATTERN.matcher(line);
            if (structMatcher.find()) {
                ClassDeclarationNode structNode = parseStruct(lines, i, structMatcher);
                program.addChild(structNode);
                continue;
            }
            
            // Parse enum declarations
            Matcher enumMatcher = ENUM_PATTERN.matcher(line);
            if (enumMatcher.find()) {
                ClassDeclarationNode enumNode = parseEnum(lines, i, enumMatcher);
                program.addChild(enumNode);
                continue;
            }
            
            // Parse protocol declarations
            Matcher protocolMatcher = PROTOCOL_PATTERN.matcher(line);
            if (protocolMatcher.find()) {
                ClassDeclarationNode protocolNode = parseProtocol(lines, i, protocolMatcher);
                program.addChild(protocolNode);
                continue;
            }
            
            // Parse standalone functions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(line);
            if (functionMatcher.find()) {
                FunctionDeclarationNode functionNode = parseFunction(lines, i, functionMatcher);
                program.addChild(functionNode);
                continue;
            }
        }
    }
    
    private ClassDeclarationNode parseClass(List<String> lines, int startIndex, Matcher classMatcher) {
        String accessibility = classMatcher.group(1);
        String finalKeyword = classMatcher.group(2);
        String className = classMatcher.group(3);
        String superTypes = classMatcher.group(4);
        
        ClassDeclarationNode classNode = new ClassDeclarationNode(className, false, startIndex + 1, 1);
        parseTypeBody(lines, startIndex, classNode);
        
        return classNode;
    }
    
    private ClassDeclarationNode parseStruct(List<String> lines, int startIndex, Matcher structMatcher) {
        String accessibility = structMatcher.group(1);
        String structName = structMatcher.group(2);
        String protocols = structMatcher.group(3);
        
        ClassDeclarationNode structNode = new ClassDeclarationNode(structName, true, startIndex + 1, 1);
        parseTypeBody(lines, startIndex, structNode);
        
        return structNode;
    }
    
    private ClassDeclarationNode parseEnum(List<String> lines, int startIndex, Matcher enumMatcher) {
        String accessibility = enumMatcher.group(1);
        String enumName = enumMatcher.group(2);
        String rawType = enumMatcher.group(3);
        
        ClassDeclarationNode enumNode = new ClassDeclarationNode(enumName, false, startIndex + 1, 1);
        enumNode.setEnum(true);
        parseTypeBody(lines, startIndex, enumNode);
        
        return enumNode;
    }
    
    private ClassDeclarationNode parseProtocol(List<String> lines, int startIndex, Matcher protocolMatcher) {
        String accessibility = protocolMatcher.group(1);
        String protocolName = protocolMatcher.group(2);
        String parentProtocols = protocolMatcher.group(3);
        
        ClassDeclarationNode protocolNode = new ClassDeclarationNode(protocolName, false, startIndex + 1, 1);
        protocolNode.setInterface(true);
        parseTypeBody(lines, startIndex, protocolNode);
        
        return protocolNode;
    }
    
    private void parseTypeBody(List<String> lines, int startIndex, ClassDeclarationNode typeNode) {
        // Find type body
        int braceCount = 0;
        boolean inType = false;
        
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    braceCount++;
                    inType = true;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0 && inType) {
                        return; // End of type
                    }
                }
            }
            
            if (inType && braceCount == 1) {
                // Parse type members
                parseTypeMember(line, typeNode, i + 1);
            }
        }
    }
    
    private void parseTypeMember(String line, ClassDeclarationNode typeNode, int lineNumber) {
        // Parse init methods
        Matcher initMatcher = INIT_PATTERN.matcher(line);
        if (initMatcher.find()) {
            FunctionDeclarationNode init = new FunctionDeclarationNode(
                typeNode.getName(), "void", parseParameters(line), true, true, lineNumber, 1);
            typeNode.addChild(init);
            return;
        }
        
        // Parse functions
        Matcher functionMatcher = FUNCTION_PATTERN.matcher(line);
        if (functionMatcher.find()) {
            String accessibility = functionMatcher.group(1);
            String functionName = functionMatcher.group(2);
            String returnType = functionMatcher.group(3);
            
            if (returnType == null || returnType.trim().isEmpty()) {
                returnType = "Void";
            } else {
                returnType = returnType.trim();
            }
            
            String mappedReturnType = mapSwiftType(returnType);
            List<ParameterNode> parameters = parseParameters(line);
            
            FunctionDeclarationNode function = new FunctionDeclarationNode(
                functionName, mappedReturnType, parameters, true, false, lineNumber, 1);
            
            if (accessibility != null && (accessibility.equals("static") || accessibility.equals("class"))) {
                function.setStatic(true);
            }
            
            typeNode.addChild(function);
            return;
        }
        
        // Parse properties
        Matcher propertyMatcher = PROPERTY_PATTERN.matcher(line);
        if (propertyMatcher.find()) {
            String accessibility = propertyMatcher.group(1);
            String varType = propertyMatcher.group(2); // var or let
            String propertyName = propertyMatcher.group(3);
            String propertyType = propertyMatcher.group(4).trim();
            String defaultValue = propertyMatcher.group(5);
            
            String mappedType = mapSwiftType(propertyType);
            boolean isFinal = "let".equals(varType);
            
            VariableDeclarationNode property = new VariableDeclarationNode(
                propertyName, mappedType, isFinal, defaultValue, lineNumber, 1);
            
            typeNode.addChild(property);
        }
    }
    
    private FunctionDeclarationNode parseFunction(List<String> lines, int startIndex, Matcher functionMatcher) {
        String accessibility = functionMatcher.group(1);
        String functionName = functionMatcher.group(2);
        String returnType = functionMatcher.group(3);
        
        if (returnType == null || returnType.trim().isEmpty()) {
            returnType = "Void";
        } else {
            returnType = returnType.trim();
        }
        
        String mappedReturnType = mapSwiftType(returnType);
        List<ParameterNode> parameters = parseParameters(lines.get(startIndex));
        
        FunctionDeclarationNode function = new FunctionDeclarationNode(
            functionName, mappedReturnType, parameters, false, false, startIndex + 1, 1);
        
        if (accessibility != null && (accessibility.equals("static") || accessibility.equals("class"))) {
            function.setStatic(true);
        }
        
        return function;
    }
    
    private List<ParameterNode> parseParameters(String line) {
        List<ParameterNode> parameters = new ArrayList<>();
        
        // Extract parameter list from parentheses
        int start = line.indexOf('(');
        int end = line.lastIndexOf(')');
        
        if (start >= 0 && end > start) {
            String paramString = line.substring(start + 1, end).trim();
            if (!paramString.isEmpty()) {
                String[] params = paramString.split(",");
                
                for (String param : params) {
                    param = param.trim();
                    
                    Matcher paramMatcher = PARAMETER_PATTERN.matcher(param);
                    if (paramMatcher.find()) {
                        String externalName = paramMatcher.group(1);
                        String paramName = paramMatcher.group(2);
                        String paramType = paramMatcher.group(3).trim();
                        String defaultValue = paramMatcher.group(4);
                        
                        String mappedType = mapSwiftType(paramType);
                        boolean optional = defaultValue != null || paramType.endsWith("?");
                        
                        ParameterNode parameter = new ParameterNode(paramName, mappedType, optional, 0, 0);
                        parameters.add(parameter);
                    } else if (!param.isEmpty()) {
                        // Simple parameter without external name
                        String[] parts = param.split(":");
                        if (parts.length >= 2) {
                            String paramName = parts[0].trim();
                            String paramType = parts[1].trim();
                            
                            String mappedType = mapSwiftType(paramType);
                            boolean optional = paramType.endsWith("?");
                            
                            ParameterNode parameter = new ParameterNode(paramName, mappedType, optional, 0, 0);
                            parameters.add(parameter);
                        }
                    }
                }
            }
        }
        
        return parameters;
    }
    
    private String mapSwiftType(String swiftType) {
        if (swiftType == null || swiftType.trim().isEmpty()) {
            return "Object";
        }
        
        swiftType = swiftType.trim();
        
        // Handle optionals
        boolean isOptional = swiftType.endsWith("?") || swiftType.endsWith("!");
        if (isOptional) {
            swiftType = swiftType.substring(0, swiftType.length() - 1);
        }
        
        switch (swiftType) {
            case "Void": return "void";
            case "Bool": return "Boolean";
            case "Int": case "Int8": case "Int16": case "Int32": return "Integer";
            case "Int64": return "Long";
            case "UInt": case "UInt8": case "UInt16": case "UInt32": return "Integer";
            case "UInt64": return "Long";
            case "Float": return "Float";
            case "Double": return "Double";
            case "Character": return "Character";
            case "String": return "String";
            case "Any": case "AnyObject": return "Object";
            default:
                // Handle generic types
                if (swiftType.startsWith("Array<") || swiftType.startsWith("[")) {
                    return "List";
                }
                if (swiftType.startsWith("Dictionary<") || swiftType.startsWith("[") && swiftType.contains(":")) {
                    return "Map";
                }
                if (swiftType.startsWith("Set<")) {
                    return "Set";
                }
                if (swiftType.startsWith("Optional<")) {
                    String innerType = swiftType.substring(9, swiftType.length() - 1);
                    return "Optional<" + mapSwiftType(innerType) + ">";
                }
                
                return swiftType; // Return as-is for custom types
        }
    }
    
    @Override
    public boolean supportsFile(String fileName) {
        return fileName.toLowerCase().endsWith(".swift");
    }
    
    @Override
    public String getLanguageName() {
        return "Swift";
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".swift"};
    }
}