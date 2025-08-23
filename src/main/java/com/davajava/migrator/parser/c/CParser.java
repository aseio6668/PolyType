package com.davajava.migrator.parser.c;

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

public class CParser implements Parser {
    private static final Logger logger = Logger.getLogger(CParser.class.getName());
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "^\\s*([\\w\\s*]+)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:\\{|;)",
        Pattern.MULTILINE
    );
    
    private static final Pattern STRUCT_PATTERN = Pattern.compile(
        "^\\s*(?:typedef\\s+)?struct\\s+(\\w+)?\\s*\\{([^}]*)\\}\\s*(\\w+)?\\s*;",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(
        "^\\s*([\\w\\s*]+)\\s+(\\w+)(?:\\s*=\\s*([^;]+))?\\s*;",
        Pattern.MULTILINE
    );

    @Override
    public ASTNode parse(String sourceCode) throws ParseException {
        try {
            sourceCode = preprocessSource(sourceCode);
            ProgramNode program = new ProgramNode(1, 1);
            
            // Parse structs first
            Matcher structMatcher = STRUCT_PATTERN.matcher(sourceCode);
            while (structMatcher.find()) {
                String structName = structMatcher.group(1);
                String structBody = structMatcher.group(2);
                String typedefName = structMatcher.group(3);
                
                // Use typedef name if available, otherwise struct name
                String finalName = (typedefName != null && !typedefName.isEmpty()) ? typedefName : structName;
                
                if (finalName != null && !finalName.isEmpty()) {
                    StructDeclarationNode structNode = parseStruct(finalName, structBody, 
                        getLineNumber(sourceCode, structMatcher.start()));
                    program.addChild(structNode);
                }
            }
            
            // Parse functions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(sourceCode);
            while (functionMatcher.find()) {
                String returnType = functionMatcher.group(1).trim();
                String functionName = functionMatcher.group(2);
                String parameters = functionMatcher.group(3);
                
                // Skip if this looks like a variable declaration
                if (returnType.contains("=") || functionName.contains("=")) {
                    continue;
                }
                
                // Map C types to Java types
                returnType = mapCTypeToJava(returnType);
                
                List<ParameterNode> paramList = parseParameters(parameters);
                
                FunctionDeclarationNode funcNode = new FunctionDeclarationNode(
                    functionName, returnType, paramList, true, false,
                    getLineNumber(sourceCode, functionMatcher.start()), 1
                );
                
                program.addChild(funcNode);
            }
            
            return program;
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse C source code", e);
        }
    }

    @Override
    public ASTNode parseFile(String filePath) throws IOException, ParseException {
        String content = Files.readString(Paths.get(filePath));
        return parse(content);
    }

    @Override
    public SourceLanguage getSupportedLanguage() {
        return SourceLanguage.C;
    }

    @Override
    public boolean canHandle(String fileName) {
        return fileName.endsWith(".c") || fileName.endsWith(".h");
    }
    
    private String preprocessSource(String sourceCode) {
        // Remove comments
        sourceCode = sourceCode.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        sourceCode = sourceCode.replaceAll("//.*", "");
        
        // Remove preprocessor directives (basic)
        sourceCode = sourceCode.replaceAll("^\\s*#.*$", "");
        
        return sourceCode;
    }
    
    private StructDeclarationNode parseStruct(String structName, String structBody, int lineNumber) {
        List<FieldDeclarationNode> fields = new ArrayList<>();
        
        // Parse struct fields
        Matcher fieldMatcher = VARIABLE_PATTERN.matcher(structBody);
        while (fieldMatcher.find()) {
            String fieldType = fieldMatcher.group(1).trim();
            String fieldName = fieldMatcher.group(2);
            
            fieldType = mapCTypeToJava(fieldType);
            
            FieldDeclarationNode fieldNode = new FieldDeclarationNode(
                fieldName, fieldType, true, false,
                getLineNumber(structBody, fieldMatcher.start()), 1
            );
            
            fields.add(fieldNode);
        }
        
        StructDeclarationNode structNode = new StructDeclarationNode(structName, true, fields, lineNumber, 1);
        return structNode;
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
            
            // Split parameter into type and name
            String[] parts = param.trim().split("\\s+");
            if (parts.length >= 2) {
                StringBuilder typeBuilder = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (i > 0) typeBuilder.append(" ");
                    typeBuilder.append(parts[i]);
                }
                String type = mapCTypeToJava(typeBuilder.toString());
                String name = parts[parts.length - 1];
                
                // Handle pointer notation in name
                if (name.startsWith("*")) {
                    name = name.substring(1);
                }
                
                parameters.add(new ParameterNode(name, type, false, 1, 1));
            }
        }
        
        return parameters;
    }
    
    private String mapCTypeToJava(String cType) {
        cType = cType.trim();
        
        // Handle pointer types
        if (cType.contains("*")) {
            String baseType = cType.replaceAll("\\*", "").trim();
            baseType = mapBasicCTypeToJava(baseType);
            
            // Most C pointers become references in Java
            if ("char".equals(baseType)) {
                return "String"; // char* typically used for strings
            }
            return baseType + "[]"; // Array for other pointer types
        }
        
        return mapBasicCTypeToJava(cType);
    }
    
    private String mapBasicCTypeToJava(String cType) {
        switch (cType) {
            case "int": return "int";
            case "long": return "long";
            case "short": return "short";
            case "char": return "char";
            case "float": return "float";
            case "double": return "double";
            case "void": return "void";
            case "unsigned int": return "int";
            case "unsigned long": return "long";
            case "unsigned short": return "short";
            case "unsigned char": return "byte";
            case "size_t": return "long";
            case "FILE": return "FileInputStream";
            default:
                // Capitalize first letter for custom types
                if (cType.length() > 0) {
                    return cType.substring(0, 1).toUpperCase() + cType.substring(1);
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