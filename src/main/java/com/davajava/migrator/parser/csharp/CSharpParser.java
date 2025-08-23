package com.davajava.migrator.parser.csharp;

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

public class CSharpParser implements Parser {
    private static final Logger logger = Logger.getLogger(CSharpParser.class.getName());
    
    // C# specific patterns
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile(
        "^\\s*namespace\\s+(\\w+(?:\\.\\w+)*)\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "^\\s*(?:(public|private|protected|internal)\\s+)?(?:(static|sealed|abstract)\\s+)?class\\s+(\\w+)(?:\\s*:\\s*([\\w\\s,<>]+))?\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "^\\s*(?:(public|private|protected|internal)\\s+)?(?:(static|virtual|override|abstract)\\s+)?([\\w\\[\\]<>]+)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:\\{|;)",
        Pattern.MULTILINE
    );
    
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
        "^\\s*(?:(public|private|protected|internal)\\s+)?(?:(static)\\s+)?([\\w\\[\\]<>]+)\\s+(\\w+)\\s*\\{[^}]*\\}",
        Pattern.MULTILINE
    );
    
    private static final Pattern FIELD_PATTERN = Pattern.compile(
        "^\\s*(?:(public|private|protected|internal)\\s+)?(?:(static|readonly|const)\\s+)?([\\w\\[\\]<>]+)\\s+(\\w+)(?:\\s*=\\s*([^;]+))?\\s*;",
        Pattern.MULTILINE
    );
    
    private static final Pattern USING_PATTERN = Pattern.compile(
        "^\\s*using\\s+([\\w\\.]+)\\s*;",
        Pattern.MULTILINE
    );

    @Override
    public ASTNode parse(String sourceCode) throws ParseException {
        try {
            sourceCode = preprocessSource(sourceCode);
            ProgramNode program = new ProgramNode(1, 1);
            
            // Parse namespaces
            Matcher namespaceMatcher = NAMESPACE_PATTERN.matcher(sourceCode);
            while (namespaceMatcher.find()) {
                String namespaceName = namespaceMatcher.group(1);
                String namespaceContent = namespaceMatcher.group(2);
                
                // For simplicity, parse namespace content directly into program
                parseNamespaceContent(program, namespaceContent, namespaceName);
            }
            
            // Also parse any classes outside of namespaces
            Matcher classMatcher = CLASS_PATTERN.matcher(sourceCode);
            while (classMatcher.find()) {
                String visibility = classMatcher.group(1);
                String modifiers = classMatcher.group(2);
                String className = classMatcher.group(3);
                String baseClasses = classMatcher.group(4);
                String classBody = classMatcher.group(5);
                
                ClassDeclarationNode classNode = parseClass(className, visibility, modifiers, baseClasses, classBody, 
                    getLineNumber(sourceCode, classMatcher.start()));
                program.addChild(classNode);
            }
            
            return program;
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse C# source code", e);
        }
    }

    @Override
    public ASTNode parseFile(String filePath) throws IOException, ParseException {
        String content = Files.readString(Paths.get(filePath));
        return parse(content);
    }

    @Override
    public SourceLanguage getSupportedLanguage() {
        return SourceLanguage.CSHARP;
    }

    @Override
    public boolean canHandle(String fileName) {
        return fileName.endsWith(".cs");
    }
    
    private String preprocessSource(String sourceCode) {
        // Remove single-line comments
        sourceCode = sourceCode.replaceAll("//.*", "");
        
        // Remove multi-line comments
        sourceCode = sourceCode.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        
        // Remove regions for simplicity
        sourceCode = sourceCode.replaceAll("^\\s*#region[^\\n]*\\n", "");
        sourceCode = sourceCode.replaceAll("^\\s*#endregion[^\\n]*\\n", "");
        
        // Remove other preprocessor directives
        sourceCode = sourceCode.replaceAll("^\\s*#.*$", "");
        
        return sourceCode;
    }
    
    private void parseNamespaceContent(ProgramNode program, String namespaceContent, String namespaceName) {
        // Parse classes within the namespace
        Matcher classMatcher = CLASS_PATTERN.matcher(namespaceContent);
        while (classMatcher.find()) {
            String visibility = classMatcher.group(1);
            String modifiers = classMatcher.group(2);
            String className = classMatcher.group(3);
            String baseClasses = classMatcher.group(4);
            String classBody = classMatcher.group(5);
            
            ClassDeclarationNode classNode = parseClass(className, visibility, modifiers, baseClasses, classBody, 
                getLineNumber(namespaceContent, classMatcher.start()));
            program.addChild(classNode);
        }
    }
    
    private ClassDeclarationNode parseClass(String className, String visibility, String modifiers, 
                                          String baseClasses, String classBody, int lineNumber) {
        boolean isPublic = "public".equals(visibility) || visibility == null; // Default to public for simplicity
        ClassDeclarationNode classNode = new ClassDeclarationNode(className, isPublic, lineNumber, 1);
        
        // Parse methods, properties, and fields in the class
        parseClassMembers(classNode, classBody);
        
        return classNode;
    }
    
    private void parseClassMembers(ClassDeclarationNode classNode, String classBody) {
        // Parse methods
        Matcher methodMatcher = METHOD_PATTERN.matcher(classBody);
        while (methodMatcher.find()) {
            String visibility = methodMatcher.group(1);
            String modifiers = methodMatcher.group(2);
            String returnType = methodMatcher.group(3);
            String methodName = methodMatcher.group(4);
            String parameters = methodMatcher.group(5);
            
            returnType = mapCSharpTypeToJava(returnType);
            List<ParameterNode> paramList = parseParameters(parameters);
            boolean isPublic = "public".equals(visibility) || visibility == null;
            boolean isStatic = "static".equals(modifiers);
            
            FunctionDeclarationNode methodNode = new FunctionDeclarationNode(
                methodName, returnType, paramList, isPublic, isStatic, 1, 1
            );
            
            classNode.addChild(methodNode);
        }
        
        // Parse properties (treat as getter/setter methods)
        Matcher propertyMatcher = PROPERTY_PATTERN.matcher(classBody);
        while (propertyMatcher.find()) {
            String visibility = propertyMatcher.group(1);
            String modifiers = propertyMatcher.group(2);
            String propertyType = propertyMatcher.group(3);
            String propertyName = propertyMatcher.group(4);
            
            propertyType = mapCSharpTypeToJava(propertyType);
            boolean isPublic = "public".equals(visibility) || visibility == null;
            boolean isStatic = "static".equals(modifiers);
            
            // Create getter
            FunctionDeclarationNode getter = new FunctionDeclarationNode(
                "get" + capitalize(propertyName), propertyType, new ArrayList<>(), isPublic, isStatic, 1, 1
            );
            classNode.addChild(getter);
            
            // Create setter
            List<ParameterNode> setterParams = new ArrayList<>();
            setterParams.add(new ParameterNode("value", propertyType, false, 1, 1));
            FunctionDeclarationNode setter = new FunctionDeclarationNode(
                "set" + capitalize(propertyName), "void", setterParams, isPublic, isStatic, 1, 1
            );
            classNode.addChild(setter);
        }
        
        // Parse fields
        Matcher fieldMatcher = FIELD_PATTERN.matcher(classBody);
        while (fieldMatcher.find()) {
            String visibility = fieldMatcher.group(1);
            String modifiers = fieldMatcher.group(2);
            String fieldType = fieldMatcher.group(3);
            String fieldName = fieldMatcher.group(4);
            
            fieldType = mapCSharpTypeToJava(fieldType);
            
            VariableDeclarationNode fieldNode = new VariableDeclarationNode(
                fieldName, fieldType, true, null, 1, 1
            );
            
            classNode.addChild(fieldNode);
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
            
            // Handle C# specific parameter formats (ref, out, params, default values)
            param = param.replaceAll("^(ref|out|params)\\s+", "");
            
            // Remove default values
            if (param.contains("=")) {
                param = param.substring(0, param.indexOf("=")).trim();
            }
            
            // Split parameter into type and name
            String[] parts = param.trim().split("\\s+");
            if (parts.length >= 2) {
                StringBuilder typeBuilder = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (i > 0) typeBuilder.append(" ");
                    typeBuilder.append(parts[i]);
                }
                String type = mapCSharpTypeToJava(typeBuilder.toString());
                String name = parts[parts.length - 1];
                
                parameters.add(new ParameterNode(name, type, false, 1, 1));
            }
        }
        
        return parameters;
    }
    
    private String mapCSharpTypeToJava(String csharpType) {
        csharpType = csharpType.trim();
        
        // Handle C# built-in types
        switch (csharpType) {
            case "int": return "int";
            case "long": return "long";
            case "short": return "short";
            case "byte": return "byte";
            case "sbyte": return "byte";
            case "uint": return "int";
            case "ulong": return "long";
            case "ushort": return "short";
            case "float": return "float";
            case "double": return "double";
            case "decimal": return "BigDecimal";
            case "bool": return "boolean";
            case "char": return "char";
            case "string": return "String";
            case "object": return "Object";
            case "void": return "void";
            
            // Handle C# collections
            case "List": return "List";
            case "Dictionary": return "Map";
            case "HashSet": return "Set";
            case "Queue": return "Queue";
            case "Stack": return "Stack";
            case "Array": return "Array";
            
            // Handle generic types
            default:
                if (csharpType.contains("<") && csharpType.contains(">")) {
                    // Handle generic types like List<T>, Dictionary<K,V>
                    return csharpType; // Keep generics mostly the same
                }
                
                if (csharpType.endsWith("[]")) {
                    // Handle arrays
                    String baseType = csharpType.substring(0, csharpType.length() - 2);
                    return mapCSharpTypeToJava(baseType) + "[]";
                }
                
                // Capitalize first letter for custom types
                if (csharpType.length() > 0) {
                    return csharpType.substring(0, 1).toUpperCase() + csharpType.substring(1);
                }
                return "Object";
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