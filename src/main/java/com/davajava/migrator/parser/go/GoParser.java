package com.davajava.migrator.parser.go;

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

public class GoParser implements Parser {
    private static final Logger logger = Logger.getLogger(GoParser.class.getName());
    
    // Go specific patterns
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
        "^\\s*package\\s+(\\w+)",
        Pattern.MULTILINE
    );
    
    private static final Pattern STRUCT_PATTERN = Pattern.compile(
        "^\\s*type\\s+(\\w+)\\s+struct\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern INTERFACE_PATTERN = Pattern.compile(
        "^\\s*type\\s+(\\w+)\\s+interface\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "^\\s*func\\s+(\\w+)\\s*\\(([^)]*)\\)(?:\\s*\\(([^)]*)\\)|\\s*([\\w\\[\\]\\*\\s,]+))?\\s*\\{",
        Pattern.MULTILINE
    );
    
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "^\\s*func\\s*\\(([^)]*)\\)\\s+(\\w+)\\s*\\(([^)]*)\\)(?:\\s*\\(([^)]*)\\)|\\s*([\\w\\[\\]\\*\\s,]+))?\\s*\\{",
        Pattern.MULTILINE
    );
    
    private static final Pattern FIELD_PATTERN = Pattern.compile(
        "^\\s*(\\w+)\\s+([\\w\\[\\]\\*\\s,<>]+)(?:\\s*`([^`]*)`)?",
        Pattern.MULTILINE
    );
    
    private static final Pattern CONST_VAR_PATTERN = Pattern.compile(
        "^\\s*(const|var)\\s+(\\w+)(?:\\s+([\\w\\[\\]\\*\\s,<>]+))?(?:\\s*=\\s*([^\\n]+))?",
        Pattern.MULTILINE
    );
    
    private static final Pattern TYPE_ALIAS_PATTERN = Pattern.compile(
        "^\\s*type\\s+(\\w+)\\s+([\\w\\[\\]\\*\\s,<>]+)",
        Pattern.MULTILINE
    );

    @Override
    public ASTNode parse(String sourceCode) throws ParseException {
        try {
            sourceCode = preprocessSource(sourceCode);
            ProgramNode program = new ProgramNode(1, 1);
            
            // Parse interfaces first
            Matcher interfaceMatcher = INTERFACE_PATTERN.matcher(sourceCode);
            while (interfaceMatcher.find()) {
                String interfaceName = interfaceMatcher.group(1);
                String interfaceBody = interfaceMatcher.group(2);
                
                ClassDeclarationNode interfaceNode = parseInterface(interfaceName, interfaceBody,
                    getLineNumber(sourceCode, interfaceMatcher.start()));
                program.addChild(interfaceNode);
            }
            
            // Parse structs
            Matcher structMatcher = STRUCT_PATTERN.matcher(sourceCode);
            while (structMatcher.find()) {
                String structName = structMatcher.group(1);
                String structBody = structMatcher.group(2);
                
                ClassDeclarationNode structClass = parseStruct(structName, structBody,
                    getLineNumber(sourceCode, structMatcher.start()));
                program.addChild(structClass);
            }
            
            // Parse standalone functions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(sourceCode);
            while (functionMatcher.find()) {
                String functionName = functionMatcher.group(1);
                String parameters = functionMatcher.group(2);
                String namedReturns = functionMatcher.group(3);
                String simpleReturn = functionMatcher.group(4);
                
                FunctionDeclarationNode funcNode = parseFunction(functionName, parameters, 
                    namedReturns != null ? namedReturns : simpleReturn,
                    getLineNumber(sourceCode, functionMatcher.start()));
                program.addChild(funcNode);
            }
            
            // Parse type aliases
            Matcher typeAliasMatcher = TYPE_ALIAS_PATTERN.matcher(sourceCode);
            while (typeAliasMatcher.find()) {
                String aliasName = typeAliasMatcher.group(1);
                String baseType = typeAliasMatcher.group(2);
                
                // Create a simple class for type alias
                ClassDeclarationNode aliasClass = new ClassDeclarationNode(aliasName, true,
                    getLineNumber(sourceCode, typeAliasMatcher.start()), 1);
                
                // Add a field to represent the underlying type
                VariableDeclarationNode valueField = new VariableDeclarationNode(
                    "value", mapGoTypeToJava(baseType), false, null, 1, 1);
                aliasClass.addChild(valueField);
                
                program.addChild(aliasClass);
            }
            
            return program;
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse Go source code", e);
        }
    }

    @Override
    public ASTNode parseFile(String filePath) throws IOException, ParseException {
        String content = Files.readString(Paths.get(filePath));
        return parse(content);
    }

    @Override
    public SourceLanguage getSupportedLanguage() {
        return SourceLanguage.GO;
    }

    @Override
    public boolean canHandle(String fileName) {
        return fileName.endsWith(".go");
    }
    
    private String preprocessSource(String sourceCode) {
        // Remove comments
        sourceCode = sourceCode.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        sourceCode = sourceCode.replaceAll("//.*", "");
        
        // Remove import statements for now
        sourceCode = sourceCode.replaceAll("^\\s*import\\s+[^\\n]*\\n", "");
        sourceCode = sourceCode.replaceAll("^\\s*import\\s*\\([^\\)]*\\)", "");
        
        return sourceCode;
    }
    
    private ClassDeclarationNode parseInterface(String interfaceName, String interfaceBody, int lineNumber) {
        ClassDeclarationNode interfaceNode = new ClassDeclarationNode(interfaceName, true, lineNumber, 1);
        
        // Parse interface methods
        String[] methods = interfaceBody.split("\\n");
        for (String method : methods) {
            method = method.trim();
            if (method.isEmpty()) continue;
            
            // Parse method signature: methodName(params) returnType
            Pattern methodSigPattern = Pattern.compile("(\\w+)\\s*\\(([^)]*)\\)(?:\\s*\\(([^)]*)\\)|\\s*([\\w\\[\\]\\*\\s,]+))?");
            Matcher methodMatcher = methodSigPattern.matcher(method);
            
            if (methodMatcher.find()) {
                String methodName = methodMatcher.group(1);
                String parameters = methodMatcher.group(2);
                String namedReturns = methodMatcher.group(3);
                String simpleReturn = methodMatcher.group(4);
                
                String returnType = namedReturns != null ? "Object" : (simpleReturn != null ? mapGoTypeToJava(simpleReturn.trim()) : "void");
                List<ParameterNode> paramList = parseParameters(parameters);
                
                FunctionDeclarationNode interfaceMethod = new FunctionDeclarationNode(
                    methodName, returnType, paramList, true, false, lineNumber, 1
                );
                interfaceNode.addChild(interfaceMethod);
            }
        }
        
        return interfaceNode;
    }
    
    private ClassDeclarationNode parseStruct(String structName, String structBody, int lineNumber) {
        ClassDeclarationNode structClass = new ClassDeclarationNode(structName, true, lineNumber, 1);
        
        // Parse struct fields
        Matcher fieldMatcher = FIELD_PATTERN.matcher(structBody);
        while (fieldMatcher.find()) {
            String fieldName = fieldMatcher.group(1);
            String fieldType = fieldMatcher.group(2);
            String tags = fieldMatcher.group(3); // JSON tags, etc.
            
            String javaType = mapGoTypeToJava(fieldType.trim());
            
            VariableDeclarationNode field = new VariableDeclarationNode(
                fieldName, javaType, true, null, lineNumber, 1
            );
            structClass.addChild(field);
            
            // Generate getter
            FunctionDeclarationNode getter = new FunctionDeclarationNode(
                "get" + capitalize(fieldName), javaType, new ArrayList<>(), true, false, lineNumber, 1
            );
            structClass.addChild(getter);
            
            // Generate setter
            List<ParameterNode> setterParams = new ArrayList<>();
            setterParams.add(new ParameterNode("value", javaType, false, lineNumber, 1));
            FunctionDeclarationNode setter = new FunctionDeclarationNode(
                "set" + capitalize(fieldName), "void", setterParams, true, false, lineNumber, 1
            );
            structClass.addChild(setter);
        }
        
        // Add constructor
        List<ParameterNode> constructorParams = new ArrayList<>();
        // Re-parse fields for constructor
        Matcher constructorFieldMatcher = FIELD_PATTERN.matcher(structBody);
        while (constructorFieldMatcher.find()) {
            String fieldName = constructorFieldMatcher.group(1);
            String fieldType = constructorFieldMatcher.group(2);
            String javaType = mapGoTypeToJava(fieldType.trim());
            
            constructorParams.add(new ParameterNode(fieldName, javaType, false, lineNumber, 1));
        }
        
        FunctionDeclarationNode constructor = new FunctionDeclarationNode(
            structName, "void", constructorParams, true, false, lineNumber, 1
        );
        structClass.addChild(constructor);
        
        return structClass;
    }
    
    private FunctionDeclarationNode parseFunction(String functionName, String parameters, String returnType, int lineNumber) {
        String javaReturnType = "void";
        
        if (returnType != null && !returnType.trim().isEmpty()) {
            if (returnType.contains(",")) {
                // Multiple return values - use a wrapper class or Object
                javaReturnType = "Object"; // or generate a Result class
            } else {
                javaReturnType = mapGoTypeToJava(returnType.trim());
            }
        }
        
        List<ParameterNode> paramList = parseParameters(parameters);
        
        return new FunctionDeclarationNode(
            functionName, javaReturnType, paramList, true, true, lineNumber, 1
        );
    }
    
    private List<ParameterNode> parseParameters(String paramString) {
        List<ParameterNode> parameters = new ArrayList<>();
        
        if (paramString == null || paramString.trim().isEmpty()) {
            return parameters;
        }
        
        // Go parameters can be: name type, name1 name2 type, etc.
        String[] parts = paramString.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            
            // Split by whitespace to get names and type
            String[] tokens = part.trim().split("\\s+");
            if (tokens.length >= 2) {
                String type = tokens[tokens.length - 1]; // Last token is type
                String javaType = mapGoTypeToJava(type);
                
                // All but last tokens are parameter names
                for (int i = 0; i < tokens.length - 1; i++) {
                    String name = tokens[i];
                    parameters.add(new ParameterNode(name, javaType, false, 1, 1));
                }
            } else if (tokens.length == 1) {
                // Only type provided, generate name
                String type = tokens[0];
                String javaType = mapGoTypeToJava(type);
                parameters.add(new ParameterNode("param" + parameters.size(), javaType, false, 1, 1));
            }
        }
        
        return parameters;
    }
    
    private String mapGoTypeToJava(String goType) {
        goType = goType.trim();
        
        // Handle pointers
        if (goType.startsWith("*")) {
            return mapGoTypeToJava(goType.substring(1)); // Remove pointer, Java uses references
        }
        
        // Handle slices and arrays
        if (goType.startsWith("[]")) {
            String elementType = mapGoTypeToJava(goType.substring(2));
            return "List<" + toWrapperType(elementType) + ">";
        }
        
        if (goType.matches("\\[\\d+\\].*")) {
            String elementType = mapGoTypeToJava(goType.replaceFirst("\\[\\d+\\]", ""));
            return elementType + "[]";
        }
        
        // Handle maps
        if (goType.startsWith("map[")) {
            Pattern mapPattern = Pattern.compile("map\\[([^\\]]+)\\](.+)");
            Matcher mapMatcher = mapPattern.matcher(goType);
            if (mapMatcher.find()) {
                String keyType = mapGoTypeToJava(mapMatcher.group(1));
                String valueType = mapGoTypeToJava(mapMatcher.group(2));
                return "Map<" + toWrapperType(keyType) + ", " + toWrapperType(valueType) + ">";
            }
        }
        
        // Handle channels
        if (goType.startsWith("chan") || goType.startsWith("<-chan") || goType.startsWith("chan<-")) {
            return "BlockingQueue<Object>"; // Approximate with blocking queue
        }
        
        // Basic types
        switch (goType) {
            case "bool": return "boolean";
            case "byte": return "byte";
            case "rune": return "char";
            case "int": case "int32": return "int";
            case "int8": return "byte";
            case "int16": return "short";
            case "int64": return "long";
            case "uint": case "uint32": return "int"; // No unsigned in Java
            case "uint8": return "byte";
            case "uint16": return "short";
            case "uint64": return "long";
            case "float32": return "float";
            case "float64": return "double";
            case "string": return "String";
            case "error": return "Exception";
            case "interface{}": case "any": return "Object";
            case "void": return "void";
            
            // Common Go types
            case "time.Time": return "LocalDateTime";
            case "time.Duration": return "Duration";
            case "context.Context": return "Object"; // No direct equivalent
            case "io.Reader": return "InputStream";
            case "io.Writer": return "OutputStream";
            case "http.ResponseWriter": return "HttpServletResponse";
            case "http.Request": return "HttpServletRequest";
            
            default:
                // Handle function types
                if (goType.startsWith("func")) {
                    return "Function<Object, Object>"; // Simplified function type
                }
                
                // Custom types
                return goType;
        }
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