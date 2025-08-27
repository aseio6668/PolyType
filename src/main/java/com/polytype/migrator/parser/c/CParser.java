package com.polytype.migrator.parser.c;

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
 * Parser for C source code that converts it to AST representation.
 * Supports C89/C99/C11/C18 standards including structs, unions, functions, and preprocessor directives.
 */
public class CParser implements Parser {
    
    private static final Pattern STRUCT_PATTERN = Pattern.compile(
        "(?:typedef\\s+)?struct\\s+(?:(\\w+)\\s*)?\\{[^}]*\\}(?:\\s*(\\w+))?\\s*;"
    );
    
    private static final Pattern UNION_PATTERN = Pattern.compile(
        "(?:typedef\\s+)?union\\s+(?:(\\w+)\\s*)?\\{[^}]*\\}(?:\\s*(\\w+))?\\s*;"
    );
    
    private static final Pattern ENUM_PATTERN = Pattern.compile(
        "(?:typedef\\s+)?enum\\s+(?:(\\w+)\\s*)?\\{[^}]*\\}(?:\\s*(\\w+))?\\s*;"
    );
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "(?:(static|extern|inline)\\s+)?([\\w*]+(?:\\s*\\*\\s*)?)\\s+(\\w+)\\s*\\([^)]*\\)\\s*(?:\\{|;)"
    );
    
    private static final Pattern FIELD_PATTERN = Pattern.compile(
        "([\\w*]+(?:\\s*\\*\\s*)?)\\s+(\\w+)(?:\\[[^\\]]*\\])?\\s*;"
    );
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(
        "(?:(static|extern|register|auto)\\s+)?(?:(const|volatile)\\s+)?([\\w*]+(?:\\s*\\*\\s*)?)\\s+(\\w+)(?:\\s*=\\s*([^;]+))?\\s*;"
    );
    
    private static final Pattern TYPEDEF_PATTERN = Pattern.compile(
        "typedef\\s+([^;]+)\\s+(\\w+)\\s*;"
    );
    
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(
        "#include\\s*[<\"]([^>\"]+)[>\"]"
    );
    
    private static final Pattern DEFINE_PATTERN = Pattern.compile(
        "#define\\s+(\\w+)(?:\\([^)]*\\))?\\s*(.*)"
    );
    
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(
        "(?:(const|volatile)\\s+)?([\\w*]+(?:\\s*\\*\\s*)?)\\s*(\\w+)?"
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
            
            // Preprocess to handle multiline constructs
            List<String> preprocessedLines = preprocessLines(lines);
            parseLines(preprocessedLines, program);
        }
        
        return program;
    }
    
    private List<String> preprocessLines(List<String> lines) {
        List<String> processed = new ArrayList<>();
        StringBuilder currentConstruct = new StringBuilder();
        boolean inMultilineConstruct = false;
        int braceCount = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Skip comments and empty lines
            if (trimmed.isEmpty() || trimmed.startsWith("//") || 
                (trimmed.startsWith("/*") && trimmed.endsWith("*/"))) {
                continue;
            }
            
            // Handle multiline constructs (structs, functions, etc.)
            if (trimmed.contains("{") || inMultilineConstruct) {
                inMultilineConstruct = true;
                currentConstruct.append(" ").append(trimmed);
                
                // Count braces to determine end of construct
                for (char c : trimmed.toCharArray()) {
                    if (c == '{') braceCount++;
                    else if (c == '}') braceCount--;
                }
                
                if (braceCount == 0) {
                    processed.add(currentConstruct.toString().trim());
                    currentConstruct.setLength(0);
                    inMultilineConstruct = false;
                }
            } else {
                processed.add(trimmed);
            }
        }
        
        return processed;
    }
    
    private void parseLines(List<String> lines, ProgramNode program) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            if (line.isEmpty()) {
                continue;
            }
            
            // Parse preprocessor directives
            if (line.startsWith("#")) {
                parsePreprocessorDirective(line, program);
                continue;
            }
            
            // Parse typedef declarations
            Matcher typedefMatcher = TYPEDEF_PATTERN.matcher(line);
            if (typedefMatcher.find()) {
                parseTypedef(typedefMatcher, program, i + 1);
                continue;
            }
            
            // Parse struct declarations
            Matcher structMatcher = STRUCT_PATTERN.matcher(line);
            if (structMatcher.find()) {
                ClassDeclarationNode structNode = parseStruct(line, structMatcher, i + 1);
                if (structNode != null) {
                    program.addChild(structNode);
                }
                continue;
            }
            
            // Parse union declarations
            Matcher unionMatcher = UNION_PATTERN.matcher(line);
            if (unionMatcher.find()) {
                ClassDeclarationNode unionNode = parseUnion(line, unionMatcher, i + 1);
                if (unionNode != null) {
                    program.addChild(unionNode);
                }
                continue;
            }
            
            // Parse enum declarations
            Matcher enumMatcher = ENUM_PATTERN.matcher(line);
            if (enumMatcher.find()) {
                ClassDeclarationNode enumNode = parseEnum(line, enumMatcher, i + 1);
                if (enumNode != null) {
                    program.addChild(enumNode);
                }
                continue;
            }
            
            // Parse function declarations/definitions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(line);
            if (functionMatcher.find()) {
                FunctionDeclarationNode functionNode = parseFunction(line, functionMatcher, i + 1);
                if (functionNode != null) {
                    program.addChild(functionNode);
                }
                continue;
            }
            
            // Parse global variable declarations
            Matcher variableMatcher = VARIABLE_PATTERN.matcher(line);
            if (variableMatcher.find()) {
                VariableDeclarationNode varNode = parseVariable(variableMatcher, i + 1);
                if (varNode != null) {
                    program.addChild(varNode);
                }
                continue;
            }
        }
    }
    
    private void parsePreprocessorDirective(String line, ProgramNode program) {
        // Handle includes and defines - could be expanded for dependency tracking
        Matcher includeMatcher = INCLUDE_PATTERN.matcher(line);
        if (includeMatcher.find()) {
            // Could track includes for dependency analysis
            return;
        }
        
        Matcher defineMatcher = DEFINE_PATTERN.matcher(line);
        if (defineMatcher.find()) {
            // Could handle #define as constants
            return;
        }
    }
    
    private void parseTypedef(Matcher typedefMatcher, ProgramNode program, int lineNumber) {
        String originalType = typedefMatcher.group(1).trim();
        String newTypeName = typedefMatcher.group(2);
        
        // Create a type alias - for simplicity, treating as a class
        ClassDeclarationNode typedefNode = new ClassDeclarationNode(newTypeName, false, lineNumber, 1);
        // Could add metadata about being a typedef
        program.addChild(typedefNode);
    }
    
    private ClassDeclarationNode parseStruct(String line, Matcher structMatcher, int lineNumber) {
        String structName = structMatcher.group(1);
        String typedefName = structMatcher.group(2);
        
        // Use typedef name if available, otherwise struct name
        String finalName = typedefName != null ? typedefName : structName;
        
        if (finalName == null) {
            return null; // Anonymous struct
        }
        
        ClassDeclarationNode structNode = new ClassDeclarationNode(finalName, true, lineNumber, 1);
        
        // Parse struct fields
        parseStructFields(line, structNode);
        
        return structNode;
    }
    
    private ClassDeclarationNode parseUnion(String line, Matcher unionMatcher, int lineNumber) {
        String unionName = unionMatcher.group(1);
        String typedefName = unionMatcher.group(2);
        
        String finalName = typedefName != null ? typedefName : unionName;
        
        if (finalName == null) {
            return null;
        }
        
        ClassDeclarationNode unionNode = new ClassDeclarationNode(finalName, true, lineNumber, 1);
        // Could add metadata about being a union
        
        parseStructFields(line, unionNode);
        
        return unionNode;
    }
    
    private ClassDeclarationNode parseEnum(String line, Matcher enumMatcher, int lineNumber) {
        String enumName = enumMatcher.group(1);
        String typedefName = enumMatcher.group(2);
        
        String finalName = typedefName != null ? typedefName : enumName;
        
        if (finalName == null) {
            return null;
        }
        
        ClassDeclarationNode enumNode = new ClassDeclarationNode(finalName, false, lineNumber, 1);
        enumNode.setEnum(true);
        
        // Parse enum values - simplified for now
        return enumNode;
    }
    
    private void parseStructFields(String line, ClassDeclarationNode structNode) {
        // Extract content between braces
        int start = line.indexOf('{');
        int end = line.lastIndexOf('}');
        
        if (start >= 0 && end > start) {
            String fieldsContent = line.substring(start + 1, end).trim();
            
            // Split by semicolons to get individual field declarations
            String[] fieldDeclarations = fieldsContent.split(";");
            
            for (String fieldDecl : fieldDeclarations) {
                fieldDecl = fieldDecl.trim();
                if (!fieldDecl.isEmpty()) {
                    parseStructField(fieldDecl, structNode);
                }
            }
        }
    }
    
    private void parseStructField(String fieldDecl, ClassDeclarationNode structNode) {
        Matcher fieldMatcher = FIELD_PATTERN.matcher(fieldDecl + ";");
        if (fieldMatcher.find()) {
            String fieldType = fieldMatcher.group(1).trim();
            String fieldName = fieldMatcher.group(2);
            
            String mappedType = mapCType(fieldType);
            VariableDeclarationNode field = new VariableDeclarationNode(
                fieldName, mappedType, false, null, 0, 0);
            
            structNode.addChild(field);
        }
    }
    
    private FunctionDeclarationNode parseFunction(String line, Matcher functionMatcher, int lineNumber) {
        String storageClass = functionMatcher.group(1); // static, extern, inline
        String returnType = functionMatcher.group(2).trim();
        String functionName = functionMatcher.group(3);
        
        String mappedReturnType = mapCType(returnType);
        List<ParameterNode> parameters = parseParameters(line);
        
        FunctionDeclarationNode function = new FunctionDeclarationNode(
            functionName, mappedReturnType, parameters, false, false, lineNumber, 1);
        
        if ("static".equals(storageClass)) {
            function.setStatic(true);
        }
        
        return function;
    }
    
    private VariableDeclarationNode parseVariable(Matcher variableMatcher, int lineNumber) {
        String storageClass = variableMatcher.group(1); // static, extern, register, auto
        String qualifier = variableMatcher.group(2);    // const, volatile
        String varType = variableMatcher.group(3).trim();
        String varName = variableMatcher.group(4);
        String initializer = variableMatcher.group(5);
        
        String mappedType = mapCType(varType);
        boolean isFinal = "const".equals(qualifier);
        
        VariableDeclarationNode variable = new VariableDeclarationNode(
            varName, mappedType, isFinal, initializer, lineNumber, 1);
        
        return variable;
    }
    
    private List<ParameterNode> parseParameters(String line) {
        List<ParameterNode> parameters = new ArrayList<>();
        
        // Extract parameter list from parentheses
        int start = line.indexOf('(');
        int end = line.lastIndexOf(')');
        
        if (start >= 0 && end > start) {
            String paramString = line.substring(start + 1, end).trim();
            if (!paramString.isEmpty() && !paramString.equals("void")) {
                String[] params = paramString.split(",");
                
                for (String param : params) {
                    param = param.trim();
                    
                    Matcher paramMatcher = PARAMETER_PATTERN.matcher(param);
                    if (paramMatcher.find()) {
                        String qualifier = paramMatcher.group(1);
                        String paramType = paramMatcher.group(2).trim();
                        String paramName = paramMatcher.group(3);
                        
                        // Handle unnamed parameters
                        if (paramName == null) {
                            paramName = "param" + (parameters.size() + 1);
                        }
                        
                        String mappedType = mapCType(paramType);
                        boolean isFinal = "const".equals(qualifier);
                        
                        ParameterNode parameter = new ParameterNode(paramName, mappedType, false, 0, 0);
                        parameters.add(parameter);
                    }
                }
            }
        }
        
        return parameters;
    }
    
    private String mapCType(String cType) {
        if (cType == null || cType.trim().isEmpty()) {
            return "Object";
        }
        
        cType = cType.trim();
        
        // Remove storage qualifiers and specifiers
        cType = cType.replaceAll("\\b(static|extern|register|auto|const|volatile|restrict|inline)\\b", "").trim();
        cType = cType.replaceAll("\\s+", " ").trim();
        
        // Handle pointers
        int pointerCount = 0;
        while (cType.endsWith("*")) {
            pointerCount++;
            cType = cType.substring(0, cType.length() - 1).trim();
        }
        
        String baseType;
        switch (cType) {
            case "void": baseType = "void"; break;
            case "char": baseType = "Character"; break;
            case "signed char": case "unsigned char": baseType = "Byte"; break;
            case "short": case "short int": case "signed short": case "signed short int": baseType = "Short"; break;
            case "unsigned short": case "unsigned short int": baseType = "Short"; break;
            case "int": case "signed": case "signed int": baseType = "Integer"; break;
            case "unsigned": case "unsigned int": baseType = "Integer"; break;
            case "long": case "long int": case "signed long": case "signed long int": baseType = "Long"; break;
            case "unsigned long": case "unsigned long int": baseType = "Long"; break;
            case "long long": case "long long int": case "signed long long": case "signed long long int": baseType = "Long"; break;
            case "unsigned long long": case "unsigned long long int": baseType = "Long"; break;
            case "float": baseType = "Float"; break;
            case "double": baseType = "Double"; break;
            case "long double": baseType = "Double"; break;
            case "_Bool": case "bool": baseType = "Boolean"; break;
            case "size_t": case "ssize_t": baseType = "Long"; break;
            case "ptrdiff_t": baseType = "Long"; break;
            case "FILE": baseType = "Object"; break;
            default:
                // Handle arrays
                if (cType.contains("[")) {
                    return "List";
                }
                // Custom types (structs, typedefs, etc.)
                baseType = cType;
        }
        
        // Handle pointer types
        if (pointerCount > 0) {
            if (baseType.equals("Character")) {
                return "String"; // char* is typically a string
            }
            return "Object"; // Other pointers become generic objects
        }
        
        return baseType;
    }
    
    @Override
    public boolean supportsFile(String fileName) {
        return fileName.toLowerCase().endsWith(".c") || 
               fileName.toLowerCase().endsWith(".h");
    }
    
    @Override
    public String getLanguageName() {
        return "C";
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".c", ".h"};
    }
}