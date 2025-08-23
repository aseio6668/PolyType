package com.davajava.migrator.parser.rust;

import com.davajava.migrator.core.ParseException;
import com.davajava.migrator.core.Parser;
import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.ast.ASTNode;
import com.davajava.migrator.core.ast.ProgramNode;
import com.davajava.migrator.core.ast.FunctionDeclarationNode;
import com.davajava.migrator.core.ast.ParameterNode;
import com.davajava.migrator.core.ast.StructDeclarationNode;
import com.davajava.migrator.core.ast.FieldDeclarationNode;
import java.util.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RustParser implements Parser {
    private static final Logger logger = Logger.getLogger(RustParser.class.getName());
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "(?:pub\\s+)?fn\\s+(\\w+)\\s*\\(([^)]*)\\)(?:\\s*->\\s*([^{]+))?\\s*\\{([^}]*)\\}", 
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern STRUCT_PATTERN = Pattern.compile(
        "(?:pub\\s+)?struct\\s+(\\w+)(?:<[^>]*>)?\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );

    @Override
    public ASTNode parse(String sourceCode) throws ParseException {
        try {
            ProgramNode program = new ProgramNode(1, 1);
            
            // Parse structs
            Matcher structMatcher = STRUCT_PATTERN.matcher(sourceCode);
            while (structMatcher.find()) {
                String visibility = sourceCode.substring(
                    Math.max(0, structMatcher.start() - 10), structMatcher.start()
                ).trim();
                boolean isPublic = visibility.contains("pub");
                
                String structName = structMatcher.group(1);
                String fieldsBlock = structMatcher.group(2);
                
                List<FieldDeclarationNode> fields = parseStructFields(fieldsBlock);
                
                StructDeclarationNode structNode = new StructDeclarationNode(
                    structName, isPublic, fields,
                    getLineNumber(sourceCode, structMatcher.start()), 1
                );
                
                program.addChild(structNode);
            }
            
            // Parse functions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(sourceCode);
            while (functionMatcher.find()) {
                String visibility = sourceCode.substring(
                    Math.max(0, functionMatcher.start() - 10), functionMatcher.start()
                ).trim();
                boolean isPublic = visibility.contains("pub");
                
                String functionName = functionMatcher.group(1);
                String parameters = functionMatcher.group(2);
                String returnType = functionMatcher.group(3);
                String functionBody = functionMatcher.group(4); // NEW: capture function body
                
                if (returnType == null || returnType.trim().isEmpty()) {
                    returnType = "void";
                } else {
                    returnType = mapRustTypeToJava(returnType.trim());
                }
                
                List<ParameterNode> paramList = parseParameters(parameters);
                
                FunctionDeclarationNode funcNode = new FunctionDeclarationNode(
                    functionName, returnType, paramList, isPublic, false, 
                    getLineNumber(sourceCode, functionMatcher.start()), 1
                );
                
                // NEW: Store the raw function body for translation
                if (functionBody != null && !functionBody.trim().isEmpty()) {
                    funcNode.setRawBody(functionBody.trim()); // We'll add this method
                }
                
                program.addChild(funcNode);
            }
            
            return program;
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse Rust source code", e);
        }
    }

    @Override
    public ASTNode parseFile(String filePath) throws IOException, ParseException {
        String content = Files.readString(Paths.get(filePath));
        return parse(content);
    }

    @Override
    public SourceLanguage getSupportedLanguage() {
        return SourceLanguage.RUST;
    }

    @Override
    public boolean canHandle(String fileName) {
        return fileName.endsWith(".rs");
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
            
            boolean isMutable = param.startsWith("mut ");
            if (isMutable) {
                param = param.substring(4);
            }
            
            String[] parts = param.split(":");
            if (parts.length >= 2) {
                String name = parts[0].trim();
                String type = mapRustTypeToJava(parts[1].trim());
                
                parameters.add(new ParameterNode(name, type, isMutable, 1, 1));
            }
        }
        
        return parameters;
    }

    private List<FieldDeclarationNode> parseStructFields(String fieldsBlock) {
        List<FieldDeclarationNode> fields = new ArrayList<>();
        
        if (fieldsBlock == null || fieldsBlock.trim().isEmpty()) {
            return fields;
        }
        
        // Split by comma, but handle nested types properly
        String[] fieldLines = fieldsBlock.split(",");
        for (String fieldLine : fieldLines) {
            fieldLine = fieldLine.trim();
            if (fieldLine.isEmpty()) continue;
            
            // Check for pub modifier
            boolean isPublic = fieldLine.startsWith("pub ");
            if (isPublic) {
                fieldLine = fieldLine.substring(4).trim();
            }
            
            String[] parts = fieldLine.split(":");
            if (parts.length >= 2) {
                String fieldName = parts[0].trim();
                String fieldType = mapRustTypeToJava(parts[1].trim());
                
                fields.add(new FieldDeclarationNode(fieldName, fieldType, isPublic, false, 1, 1));
            }
        }
        
        return fields;
    }

    private String mapRustTypeToJava(String rustType) {
        rustType = rustType.trim();
        
        // Basic type mappings
        switch (rustType) {
            case "i32": case "i64": return "int";
            case "u32": case "u64": return "int";
            case "f32": case "f64": return "double";
            case "bool": return "boolean";
            case "String": case "&str": return "String";
            case "()": return "void";
            default:
                // Handle Option<T>
                if (rustType.startsWith("Option<") && rustType.endsWith(">")) {
                    String innerType = rustType.substring(7, rustType.length() - 1);
                    return mapRustTypeToJava(innerType);
                }
                
                // Handle Vec<T>
                if (rustType.startsWith("Vec<") && rustType.endsWith(">")) {
                    String innerType = rustType.substring(4, rustType.length() - 1);
                    return mapRustTypeToJava(innerType) + "[]";
                }
                
                // Handle references
                if (rustType.startsWith("&")) {
                    return mapRustTypeToJava(rustType.substring(1));
                }
                
                return rustType; // Return as-is for unknown types
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