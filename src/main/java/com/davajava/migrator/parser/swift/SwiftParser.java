package com.davajava.migrator.parser.swift;

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

public class SwiftParser implements Parser {
    private static final Logger logger = Logger.getLogger(SwiftParser.class.getName());
    
    // Swift specific patterns
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "^\\s*(?:(open|public|internal|fileprivate|private)\\s+)?(?:(final)\\s+)?class\\s+(\\w+)(?:\\s*:\\s*([\\w\\s,<>]+))?\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern STRUCT_PATTERN = Pattern.compile(
        "^\\s*(?:(public|internal|fileprivate|private)\\s+)?struct\\s+(\\w+)(?:\\s*:\\s*([\\w\\s,<>]+))?\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile(
        "^\\s*(?:(public|internal|fileprivate|private)\\s+)?protocol\\s+(\\w+)(?:\\s*:\\s*([\\w\\s,<>]+))?\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern ENUM_PATTERN = Pattern.compile(
        "^\\s*(?:(public|internal|fileprivate|private)\\s+)?enum\\s+(\\w+)(?:\\s*:\\s*([\\w\\s,<>]+))?\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "^\\s*(?:(open|public|internal|fileprivate|private)\\s+)?(?:(static|class)\\s+)?func\\s+(\\w+)(?:\\s*<([^>]*)>)?\\s*\\(([^)]*)\\)(?:\\s*->\\s*([^\\{\\n]+))?\\s*\\{",
        Pattern.MULTILINE
    );
    
    private static final Pattern INIT_PATTERN = Pattern.compile(
        "^\\s*(?:(public|internal|fileprivate|private)\\s+)?(?:(convenience|required)\\s+)?init(?:\\?|!)?\\s*\\(([^)]*)\\)\\s*\\{",
        Pattern.MULTILINE
    );
    
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
        "^\\s*(?:(open|public|internal|fileprivate|private)\\s+)?(?:(static|class)\\s+)?(?:(var|let)\\s+)(\\w+)\\s*:\\s*([^\\{\\=\\n]+)(?:\\s*=\\s*([^\\{\\n]+))?(?:\\s*\\{([^}]*)\\})?",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern COMPUTED_PROPERTY_PATTERN = Pattern.compile(
        "^\\s*(?:(open|public|internal|fileprivate|private)\\s+)?(?:(static|class)\\s+)?var\\s+(\\w+)\\s*:\\s*([^\\{\\n]+)\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );

    @Override
    public ASTNode parse(String sourceCode) throws ParseException {
        try {
            sourceCode = preprocessSource(sourceCode);
            ProgramNode program = new ProgramNode(1, 1);
            
            // Parse protocols first (similar to interfaces)
            Matcher protocolMatcher = PROTOCOL_PATTERN.matcher(sourceCode);
            while (protocolMatcher.find()) {
                String accessLevel = protocolMatcher.group(1);
                String protocolName = protocolMatcher.group(2);
                String inheritance = protocolMatcher.group(3);
                String protocolBody = protocolMatcher.group(4);
                
                ClassDeclarationNode protocolClass = parseProtocol(protocolName, protocolBody,
                    getLineNumber(sourceCode, protocolMatcher.start()));
                program.addChild(protocolClass);
            }
            
            // Parse enums
            Matcher enumMatcher = ENUM_PATTERN.matcher(sourceCode);
            while (enumMatcher.find()) {
                String accessLevel = enumMatcher.group(1);
                String enumName = enumMatcher.group(2);
                String rawType = enumMatcher.group(3);
                String enumBody = enumMatcher.group(4);
                
                ClassDeclarationNode enumClass = parseEnum(enumName, rawType, enumBody,
                    getLineNumber(sourceCode, enumMatcher.start()));
                program.addChild(enumClass);
            }
            
            // Parse structs
            Matcher structMatcher = STRUCT_PATTERN.matcher(sourceCode);
            while (structMatcher.find()) {
                String accessLevel = structMatcher.group(1);
                String structName = structMatcher.group(2);
                String protocols = structMatcher.group(3);
                String structBody = structMatcher.group(4);
                
                ClassDeclarationNode structClass = parseStruct(structName, structBody,
                    getLineNumber(sourceCode, structMatcher.start()));
                program.addChild(structClass);
            }
            
            // Parse classes
            Matcher classMatcher = CLASS_PATTERN.matcher(sourceCode);
            while (classMatcher.find()) {
                String accessLevel = classMatcher.group(1);
                String finalModifier = classMatcher.group(2);
                String className = classMatcher.group(3);
                String inheritance = classMatcher.group(4);
                String classBody = classMatcher.group(5);
                
                ClassDeclarationNode classNode = parseClass(className, classBody,
                    getLineNumber(sourceCode, classMatcher.start()));
                program.addChild(classNode);
            }
            
            // Parse standalone functions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(sourceCode);
            while (functionMatcher.find()) {
                String accessLevel = functionMatcher.group(1);
                String staticModifier = functionMatcher.group(2);
                String functionName = functionMatcher.group(3);
                String generics = functionMatcher.group(4);
                String parameters = functionMatcher.group(5);
                String returnType = functionMatcher.group(6);
                
                FunctionDeclarationNode funcNode = parseFunction(functionName, parameters, returnType,
                    getLineNumber(sourceCode, functionMatcher.start()));
                program.addChild(funcNode);
            }
            
            return program;
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse Swift source code", e);
        }
    }

    @Override
    public ASTNode parseFile(String filePath) throws IOException, ParseException {
        String content = Files.readString(Paths.get(filePath));
        return parse(content);
    }

    @Override
    public SourceLanguage getSupportedLanguage() {
        return SourceLanguage.SWIFT;
    }

    @Override
    public boolean canHandle(String fileName) {
        return fileName.endsWith(".swift");
    }
    
    private String preprocessSource(String sourceCode) {
        // Remove comments
        sourceCode = sourceCode.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        sourceCode = sourceCode.replaceAll("//.*", "");
        
        // Remove import statements for now
        sourceCode = sourceCode.replaceAll("^\\s*import\\s+[^\\n]+\\n", "");
        
        return sourceCode;
    }
    
    private ClassDeclarationNode parseProtocol(String protocolName, String protocolBody, int lineNumber) {
        ClassDeclarationNode protocolClass = new ClassDeclarationNode(protocolName, true, lineNumber, 1);
        
        // Parse protocol methods and properties
        parseClassBody(protocolClass, protocolBody, true);
        
        return protocolClass;
    }
    
    private ClassDeclarationNode parseEnum(String enumName, String rawType, String enumBody, int lineNumber) {
        ClassDeclarationNode enumClass = new ClassDeclarationNode(enumName, true, lineNumber, 1);
        
        // Parse enum cases and methods
        String[] lines = enumBody.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("case ")) {
                // Parse enum cases
                String caseName = line.substring(5).split("\\s*[=,]")[0].trim();
                
                // Create a static constant for each case
                VariableDeclarationNode caseConstant = new VariableDeclarationNode(
                    caseName.toUpperCase(), enumName, false, null, lineNumber, 1);
                enumClass.addChild(caseConstant);
            }
        }
        
        parseClassBody(enumClass, enumBody, false);
        
        return enumClass;
    }
    
    private ClassDeclarationNode parseStruct(String structName, String structBody, int lineNumber) {
        ClassDeclarationNode structClass = new ClassDeclarationNode(structName, true, lineNumber, 1);
        
        parseClassBody(structClass, structBody, false);
        
        // Add memberwise initializer (Swift feature)
        addMemberwiseInitializer(structClass, structName);
        
        return structClass;
    }
    
    private ClassDeclarationNode parseClass(String className, String classBody, int lineNumber) {
        ClassDeclarationNode classNode = new ClassDeclarationNode(className, true, lineNumber, 1);
        
        parseClassBody(classNode, classBody, false);
        
        return classNode;
    }
    
    private void parseClassBody(ClassDeclarationNode classNode, String body, boolean isProtocol) {
        // Parse properties
        Matcher propertyMatcher = PROPERTY_PATTERN.matcher(body);
        while (propertyMatcher.find()) {
            String accessLevel = propertyMatcher.group(1);
            String staticModifier = propertyMatcher.group(2);
            String varLet = propertyMatcher.group(3);
            String propertyName = propertyMatcher.group(4);
            String propertyType = propertyMatcher.group(5);
            String initializer = propertyMatcher.group(6);
            String propertyBody = propertyMatcher.group(7);
            
            boolean isMutable = "var".equals(varLet);
            String javaType = mapSwiftTypeToJava(propertyType.trim());
            
            VariableDeclarationNode property = new VariableDeclarationNode(
                propertyName, javaType, isMutable, null, 1, 1);
            classNode.addChild(property);
            
            // Generate getter
            FunctionDeclarationNode getter = new FunctionDeclarationNode(
                "get" + capitalize(propertyName), javaType, new ArrayList<>(), true, false, 1, 1);
            classNode.addChild(getter);
            
            // Generate setter for var properties
            if (isMutable) {
                List<ParameterNode> setterParams = new ArrayList<>();
                setterParams.add(new ParameterNode("value", javaType, false, 1, 1));
                FunctionDeclarationNode setter = new FunctionDeclarationNode(
                    "set" + capitalize(propertyName), "void", setterParams, true, false, 1, 1);
                classNode.addChild(setter);
            }
        }
        
        // Parse computed properties
        Matcher computedPropertyMatcher = COMPUTED_PROPERTY_PATTERN.matcher(body);
        while (computedPropertyMatcher.find()) {
            String accessLevel = computedPropertyMatcher.group(1);
            String staticModifier = computedPropertyMatcher.group(2);
            String propertyName = computedPropertyMatcher.group(3);
            String propertyType = computedPropertyMatcher.group(4);
            String propertyBody = computedPropertyMatcher.group(5);
            
            String javaType = mapSwiftTypeToJava(propertyType.trim());
            
            // Computed property becomes a getter method
            FunctionDeclarationNode getter = new FunctionDeclarationNode(
                "get" + capitalize(propertyName), javaType, new ArrayList<>(), true, false, 1, 1);
            classNode.addChild(getter);
        }
        
        // Parse initializers
        Matcher initMatcher = INIT_PATTERN.matcher(body);
        while (initMatcher.find()) {
            String accessLevel = initMatcher.group(1);
            String initModifier = initMatcher.group(2);
            String parameters = initMatcher.group(3);
            
            List<ParameterNode> paramList = parseParameters(parameters);
            FunctionDeclarationNode constructor = new FunctionDeclarationNode(
                classNode.getName(), "void", paramList, true, false, 1, 1);
            classNode.addChild(constructor);
        }
        
        // Parse functions
        Matcher functionMatcher = FUNCTION_PATTERN.matcher(body);
        while (functionMatcher.find()) {
            String accessLevel = functionMatcher.group(1);
            String staticModifier = functionMatcher.group(2);
            String functionName = functionMatcher.group(3);
            String generics = functionMatcher.group(4);
            String parameters = functionMatcher.group(5);
            String returnType = functionMatcher.group(6);
            
            FunctionDeclarationNode method = parseFunction(functionName, parameters, returnType, 1);
            if ("static".equals(staticModifier) || "class".equals(staticModifier)) {
                // Mark as static
                method = new FunctionDeclarationNode(
                    functionName, method.getReturnType(), method.getParameters(), true, true, 1, 1);
            }
            classNode.addChild(method);
        }
    }
    
    private FunctionDeclarationNode parseFunction(String functionName, String parameters, String returnType, int lineNumber) {
        String javaReturnType = "void";
        
        if (returnType != null && !returnType.trim().isEmpty()) {
            javaReturnType = mapSwiftTypeToJava(returnType.trim());
        }
        
        List<ParameterNode> paramList = parseParameters(parameters);
        
        return new FunctionDeclarationNode(
            functionName, javaReturnType, paramList, true, false, lineNumber, 1);
    }
    
    private void addMemberwiseInitializer(ClassDeclarationNode structClass, String structName) {
        List<ParameterNode> memberwiseParams = new ArrayList<>();
        
        // Collect all stored properties for memberwise initializer
        for (ASTNode child : structClass.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode property = (VariableDeclarationNode) child;
                memberwiseParams.add(new ParameterNode(property.getName(), property.getDataType(), false, 1, 1));
            }
        }
        
        if (!memberwiseParams.isEmpty()) {
            FunctionDeclarationNode memberwiseInit = new FunctionDeclarationNode(
                structName, "void", memberwiseParams, true, false, 1, 1);
            structClass.addChild(memberwiseInit);
        }
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
            
            // Swift parameter format: externalName internalName: Type = defaultValue
            // or: _ paramName: Type
            // or: paramName: Type
            
            String name = "param";
            String type = "Object";
            
            if (param.contains(":")) {
                String[] parts = param.split(":");
                String namesPart = parts[0].trim();
                String typePart = parts[1].split("=")[0].trim(); // Remove default value
                
                // Handle external/internal names
                String[] names = namesPart.split("\\s+");
                if (names.length == 2 && !"_".equals(names[0])) {
                    name = names[1]; // Use internal name
                } else if (names.length == 1) {
                    name = names[0];
                } else if (names.length == 2 && "_".equals(names[0])) {
                    name = names[1];
                }
                
                type = mapSwiftTypeToJava(typePart);
            }
            
            parameters.add(new ParameterNode(name, type, false, 1, 1));
        }
        
        return parameters;
    }
    
    private String mapSwiftTypeToJava(String swiftType) {
        swiftType = swiftType.trim();
        
        // Handle optionals
        if (swiftType.endsWith("?")) {
            String baseType = mapSwiftTypeToJava(swiftType.substring(0, swiftType.length() - 1));
            return "Optional<" + toWrapperType(baseType) + ">";
        }
        
        // Handle implicitly unwrapped optionals
        if (swiftType.endsWith("!")) {
            return mapSwiftTypeToJava(swiftType.substring(0, swiftType.length() - 1));
        }
        
        // Handle arrays
        if (swiftType.startsWith("[") && swiftType.endsWith("]")) {
            String elementType = mapSwiftTypeToJava(swiftType.substring(1, swiftType.length() - 1));
            return "List<" + toWrapperType(elementType) + ">";
        }
        
        // Handle dictionaries
        if (swiftType.startsWith("[") && swiftType.contains(":") && swiftType.endsWith("]")) {
            String inner = swiftType.substring(1, swiftType.length() - 1);
            String[] keyValue = inner.split(":", 2);
            if (keyValue.length == 2) {
                String keyType = mapSwiftTypeToJava(keyValue[0].trim());
                String valueType = mapSwiftTypeToJava(keyValue[1].trim());
                return "Map<" + toWrapperType(keyType) + ", " + toWrapperType(valueType) + ">";
            }
        }
        
        // Handle tuples (approximate with Object)
        if (swiftType.startsWith("(") && swiftType.endsWith(")")) {
            return "Object"; // Tuples don't have a direct Java equivalent
        }
        
        // Basic types
        switch (swiftType) {
            case "Int": return "int";
            case "Int8": return "byte";
            case "Int16": return "short";
            case "Int32": return "int";
            case "Int64": return "long";
            case "UInt": case "UInt8": case "UInt16": case "UInt32": return "int"; // No unsigned in Java
            case "UInt64": return "long";
            case "Float": return "float";
            case "Double": return "double";
            case "Bool": return "boolean";
            case "Character": return "char";
            case "String": return "String";
            case "Void": return "void";
            case "Any": case "AnyObject": return "Object";
            case "Never": return "void";
            
            // Swift standard library types
            case "Array": return "List";
            case "Dictionary": return "Map";
            case "Set": return "Set";
            case "Optional": return "Optional";
            case "Result": return "Object"; // No direct equivalent
            case "Range": return "Object";
            case "ClosedRange": return "Object";
            case "Data": return "byte[]";
            case "URL": return "URI";
            case "Date": return "LocalDateTime";
            case "UUID": return "UUID";
            case "Decimal": return "BigDecimal";
            
            // Closure types
            case "() -> Void": return "Runnable";
            case "() -> ()": return "Runnable";
            
            default:
                // Handle generic types
                if (swiftType.contains("<") && swiftType.contains(">")) {
                    return mapGenericSwiftType(swiftType);
                }
                
                // Handle function types
                if (swiftType.contains("->")) {
                    return "Function<Object, Object>"; // Simplified function type
                }
                
                // Custom types
                return swiftType;
        }
    }
    
    private String mapGenericSwiftType(String genericType) {
        // Handle Array<T>, Dictionary<K,V>, etc.
        if (genericType.startsWith("Array<") && genericType.endsWith(">")) {
            String innerType = genericType.substring(6, genericType.length() - 1);
            return "List<" + toWrapperType(mapSwiftTypeToJava(innerType)) + ">";
        }
        
        if (genericType.startsWith("Dictionary<") && genericType.endsWith(">")) {
            String inner = genericType.substring(11, genericType.length() - 1);
            String[] types = inner.split(",", 2);
            if (types.length == 2) {
                String keyType = mapSwiftTypeToJava(types[0].trim());
                String valueType = mapSwiftTypeToJava(types[1].trim());
                return "Map<" + toWrapperType(keyType) + ", " + toWrapperType(valueType) + ">";
            }
        }
        
        if (genericType.startsWith("Set<") && genericType.endsWith(">")) {
            String innerType = genericType.substring(4, genericType.length() - 1);
            return "Set<" + toWrapperType(mapSwiftTypeToJava(innerType)) + ">";
        }
        
        if (genericType.startsWith("Optional<") && genericType.endsWith(">")) {
            String innerType = genericType.substring(9, genericType.length() - 1);
            return "Optional<" + toWrapperType(mapSwiftTypeToJava(innerType)) + ">";
        }
        
        return genericType; // Keep as-is for unrecognized generics
    }
    
    private String toWrapperType(String primitiveType) {
        switch (primitiveType) {
            case "boolean": return "Boolean";
            case "byte": return "Byte";
            case "short": return "Short";
            case "int": return "Integer";
            case "long": return "Long";
            case "float": return "Float";
            case "double": return "Double";
            case "char": return "Character";
            default: return primitiveType;
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