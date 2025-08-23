package com.davajava.migrator.parser.cpp;

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

public class CppParser implements Parser {
    private static final Logger logger = Logger.getLogger(CppParser.class.getName());
    
    // C++ specific patterns
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "^\\s*class\\s+(\\w+)(?:\\s*:\\s*(?:public|private|protected)\\s+(\\w+))?\\s*\\{([^}]*)\\}\\s*;",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile(
        "^\\s*namespace\\s+(\\w+)\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "^\\s*(?:(public|private|protected)\\s*:)?\\s*([\\w\\s*<>:]+)\\s+(\\w+)\\s*\\(([^)]*)\\)(?:\\s*const)?\\s*(?:\\{|;)",
        Pattern.MULTILINE
    );
    
    private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile(
        "^\\s*(?:(public|private|protected)\\s*:)?\\s*(\\w+)\\s*\\(([^)]*)\\)(?:\\s*:\\s*[^{]*)?\\s*(?:\\{|;)",
        Pattern.MULTILINE
    );
    
    private static final Pattern FIELD_PATTERN = Pattern.compile(
        "^\\s*(?:(public|private|protected)\\s*:)?\\s*([\\w\\s*<>:]+)\\s+(\\w+)(?:\\s*=\\s*([^;]+))?\\s*;",
        Pattern.MULTILINE
    );
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "^\\s*([\\w\\s*<>:]+)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:\\{|;)",
        Pattern.MULTILINE
    );

    @Override
    public ASTNode parse(String sourceCode) throws ParseException {
        try {
            sourceCode = preprocessSource(sourceCode);
            ProgramNode program = new ProgramNode(1, 1);
            
            // Parse namespaces first
            Matcher namespaceMatcher = NAMESPACE_PATTERN.matcher(sourceCode);
            while (namespaceMatcher.find()) {
                // For simplicity, we'll treat namespace content as part of the main program
                // In a full implementation, you'd create namespace nodes
                String namespaceContent = namespaceMatcher.group(2);
                parseNamespaceContent(program, namespaceContent);
            }
            
            // Parse classes
            Matcher classMatcher = CLASS_PATTERN.matcher(sourceCode);
            while (classMatcher.find()) {
                String className = classMatcher.group(1);
                String baseClass = classMatcher.group(2);
                String classBody = classMatcher.group(3);
                
                ClassDeclarationNode classNode = parseClass(className, baseClass, classBody, 
                    getLineNumber(sourceCode, classMatcher.start()));
                program.addChild(classNode);
            }
            
            // Parse standalone functions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(sourceCode);
            while (functionMatcher.find()) {
                String returnType = functionMatcher.group(1).trim();
                String functionName = functionMatcher.group(2);
                String parameters = functionMatcher.group(3);
                
                // Skip if this looks like a constructor or is inside a class
                if (returnType.equals(functionName)) {
                    continue;
                }
                
                returnType = mapCppTypeToJava(returnType);
                List<ParameterNode> paramList = parseParameters(parameters);
                
                FunctionDeclarationNode funcNode = new FunctionDeclarationNode(
                    functionName, returnType, paramList, true, false,
                    getLineNumber(sourceCode, functionMatcher.start()), 1
                );
                
                program.addChild(funcNode);
            }
            
            return program;
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse C++ source code", e);
        }
    }

    @Override
    public ASTNode parseFile(String filePath) throws IOException, ParseException {
        String content = Files.readString(Paths.get(filePath));
        return parse(content);
    }

    @Override
    public SourceLanguage getSupportedLanguage() {
        return SourceLanguage.CPP;
    }

    @Override
    public boolean canHandle(String fileName) {
        return fileName.endsWith(".cpp") || fileName.endsWith(".cc") || fileName.endsWith(".cxx") || fileName.endsWith(".hpp");
    }
    
    private String preprocessSource(String sourceCode) {
        // Remove comments
        sourceCode = sourceCode.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        sourceCode = sourceCode.replaceAll("//.*", "");
        
        // Remove preprocessor directives (basic)
        sourceCode = sourceCode.replaceAll("^\\s*#.*$", "");
        
        // Remove template declarations for simplicity
        sourceCode = sourceCode.replaceAll("template\\s*<[^>]*>\\s*", "");
        
        return sourceCode;
    }
    
    private void parseNamespaceContent(ProgramNode program, String namespaceContent) {
        // For now, just parse the content as if it were global scope
        // In a full implementation, you'd maintain namespace context
    }
    
    private ClassDeclarationNode parseClass(String className, String baseClass, String classBody, int lineNumber) {
        ClassDeclarationNode classNode = new ClassDeclarationNode(className, true, lineNumber, 1);
        
        String currentVisibility = "private"; // C++ default
        
        // Parse methods and fields in the class
        String[] lines = classBody.split("\n");
        StringBuilder currentSection = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            
            // Check for visibility modifiers
            if (line.matches("(public|private|protected)\\s*:")) {
                currentVisibility = line.replaceAll("\\s*:", "").trim();
                continue;
            }
            
            // Parse methods
            Matcher methodMatcher = METHOD_PATTERN.matcher(line);
            if (methodMatcher.find()) {
                String returnType = methodMatcher.group(2).trim();
                String methodName = methodMatcher.group(3);
                String parameters = methodMatcher.group(4);
                
                returnType = mapCppTypeToJava(returnType);
                List<ParameterNode> paramList = parseParameters(parameters);
                boolean isPublic = "public".equals(currentVisibility);
                
                FunctionDeclarationNode methodNode = new FunctionDeclarationNode(
                    methodName, returnType, paramList, isPublic, false, lineNumber, 1
                );
                
                classNode.addChild(methodNode);
                continue;
            }
            
            // Parse constructors
            Matcher constructorMatcher = CONSTRUCTOR_PATTERN.matcher(line);
            if (constructorMatcher.find() && constructorMatcher.group(2).equals(className)) {
                String parameters = constructorMatcher.group(3);
                List<ParameterNode> paramList = parseParameters(parameters);
                boolean isPublic = "public".equals(currentVisibility);
                
                FunctionDeclarationNode constructorNode = new FunctionDeclarationNode(
                    className, "void", paramList, isPublic, false, lineNumber, 1
                );
                
                classNode.addChild(constructorNode);
                continue;
            }
            
            // Parse fields
            Matcher fieldMatcher = FIELD_PATTERN.matcher(line);
            if (fieldMatcher.find()) {
                String fieldType = fieldMatcher.group(2).trim();
                String fieldName = fieldMatcher.group(3);
                
                fieldType = mapCppTypeToJava(fieldType);
                boolean isPublic = "public".equals(currentVisibility);
                
                VariableDeclarationNode fieldNode = new VariableDeclarationNode(
                    fieldName, fieldType, true, null, lineNumber, 1
                );
                
                classNode.addChild(fieldNode);
            }
        }
        
        return classNode;
    }
    
    private List<ParameterNode> parseParameters(String paramString) {
        List<ParameterNode> parameters = new ArrayList<>();
        
        if (paramString == null || paramString.trim().isEmpty() || "void".equals(paramString.trim())) {
            return parameters;
        }
        
        String[] params = paramString.split(",");
        for (String param : params) {
            param = param.trim();
            if (param.isEmpty()) continue;
            
            // Handle C++ specific parameter formats
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
                String type = mapCppTypeToJava(typeBuilder.toString());
                String name = parts[parts.length - 1];
                
                // Handle reference and pointer notation in name
                if (name.startsWith("*") || name.startsWith("&")) {
                    name = name.substring(1);
                }
                
                parameters.add(new ParameterNode(name, type, false, 1, 1));
            }
        }
        
        return parameters;
    }
    
    private String mapCppTypeToJava(String cppType) {
        cppType = cppType.trim();
        
        // Handle C++ references and pointers
        if (cppType.contains("&") && !cppType.contains("*")) {
            // Reference - just remove & for Java
            cppType = cppType.replaceAll("&", "").trim();
        }
        
        if (cppType.contains("*")) {
            String baseType = cppType.replaceAll("\\*", "").trim();
            baseType = mapBasicCppTypeToJava(baseType);
            
            if ("char".equals(baseType)) {
                return "String";
            }
            return baseType + "[]";
        }
        
        // Handle C++ standard library types
        if (cppType.startsWith("std::")) {
            return mapStdTypeToJava(cppType);
        }
        
        return mapBasicCppTypeToJava(cppType);
    }
    
    private String mapBasicCppTypeToJava(String cppType) {
        switch (cppType) {
            case "int": return "int";
            case "long": case "long long": return "long";
            case "short": return "short";
            case "char": return "char";
            case "float": return "float";
            case "double": return "double";
            case "bool": return "boolean";
            case "void": return "void";
            case "unsigned int": return "int";
            case "unsigned long": return "long";
            case "unsigned short": return "short";
            case "unsigned char": return "byte";
            case "size_t": return "long";
            case "string": case "std::string": return "String";
            default:
                // Capitalize first letter for custom types
                if (cppType.length() > 0) {
                    return cppType.substring(0, 1).toUpperCase() + cppType.substring(1);
                }
                return "Object";
        }
    }
    
    private String mapStdTypeToJava(String stdType) {
        switch (stdType) {
            case "std::string": return "String";
            case "std::vector": return "List";
            case "std::map": return "Map";
            case "std::set": return "Set";
            case "std::list": return "LinkedList";
            case "std::queue": return "Queue";
            case "std::stack": return "Stack";
            case "std::pair": return "Pair";
            case "std::shared_ptr": case "std::unique_ptr": return ""; // Remove smart pointers
            default:
                if (stdType.startsWith("std::")) {
                    String type = stdType.substring(5);
                    return type.substring(0, 1).toUpperCase() + type.substring(1);
                }
                return "Object";
        }
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