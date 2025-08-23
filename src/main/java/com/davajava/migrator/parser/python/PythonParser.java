package com.davajava.migrator.parser.python;

import com.davajava.migrator.core.ParseException;
import com.davajava.migrator.core.Parser;
import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.ast.ASTNode;
import com.davajava.migrator.core.ast.ProgramNode;
import com.davajava.migrator.core.ast.FunctionDeclarationNode;
import com.davajava.migrator.core.ast.ParameterNode;
import com.davajava.migrator.core.ast.ClassDeclarationNode;
import java.util.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PythonParser implements Parser {
    private static final Logger logger = Logger.getLogger(PythonParser.class.getName());
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "^def\\s+(\\w+)\\s*\\(([^)]*)\\)(?:\\s*->\\s*([^:]+))?\\s*:",
        Pattern.MULTILINE
    );
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "^class\\s+(\\w+)(?:\\([^)]*\\))?\\s*:",
        Pattern.MULTILINE
    );

    @Override
    public ASTNode parse(String sourceCode) throws ParseException {
        try {
            ProgramNode program = new ProgramNode(1, 1);
            
            // Parse classes
            Matcher classMatcher = CLASS_PATTERN.matcher(sourceCode);
            while (classMatcher.find()) {
                String className = classMatcher.group(1);
                
                ClassDeclarationNode classNode = new ClassDeclarationNode(
                    className, true, // Assume public for now
                    getLineNumber(sourceCode, classMatcher.start()), 1
                );
                
                program.addChild(classNode);
            }
            
            // Parse functions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(sourceCode);
            while (functionMatcher.find()) {
                String functionName = functionMatcher.group(1);
                String parameters = functionMatcher.group(2);
                String returnType = functionMatcher.group(3);
                
                if (returnType == null || returnType.trim().isEmpty()) {
                    returnType = "void";
                } else {
                    returnType = mapPythonTypeToJava(returnType.trim());
                }
                
                List<ParameterNode> paramList = parseParameters(parameters);
                
                // Skip __init__ and other special methods for now
                if (functionName.startsWith("__") && functionName.endsWith("__")) {
                    continue;
                }
                
                FunctionDeclarationNode funcNode = new FunctionDeclarationNode(
                    functionName, returnType, paramList, true, false, // Assume public for now
                    getLineNumber(sourceCode, functionMatcher.start()), 1
                );
                
                program.addChild(funcNode);
            }
            
            return program;
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse Python source code", e);
        }
    }

    @Override
    public ASTNode parseFile(String filePath) throws IOException, ParseException {
        String content = Files.readString(Paths.get(filePath));
        return parse(content);
    }

    @Override
    public SourceLanguage getSupportedLanguage() {
        return SourceLanguage.PYTHON;
    }

    @Override
    public boolean canHandle(String fileName) {
        return fileName.endsWith(".py");
    }

    private List<ParameterNode> parseParameters(String paramString) {
        List<ParameterNode> parameters = new ArrayList<>();
        
        if (paramString == null || paramString.trim().isEmpty()) {
            return parameters;
        }
        
        String[] params = paramString.split(",");
        for (String param : params) {
            param = param.trim();
            if (param.isEmpty() || "self".equals(param)) continue;
            
            String name;
            String type = "Object"; // Default type
            
            // Handle type annotations (param: type)
            if (param.contains(":")) {
                String[] parts = param.split(":");
                name = parts[0].trim();
                if (parts.length > 1) {
                    type = mapPythonTypeToJava(parts[1].trim());
                }
            } else {
                name = param;
            }
            
            parameters.add(new ParameterNode(name, type, false, 1, 1));
        }
        
        return parameters;
    }

    private String mapPythonTypeToJava(String pythonType) {
        pythonType = pythonType.trim();
        
        // Basic type mappings
        switch (pythonType) {
            case "int": return "int";
            case "float": return "double";
            case "bool": return "boolean";
            case "str": return "String";
            case "list": return "List";
            case "dict": return "Map";
            case "None": return "void";
            default:
                // Handle generic types like list[int], dict[str, int]
                if (pythonType.startsWith("list[") && pythonType.endsWith("]")) {
                    String innerType = pythonType.substring(5, pythonType.length() - 1);
                    return "List<" + mapPythonTypeToJava(innerType) + ">";
                }
                
                if (pythonType.startsWith("dict[") && pythonType.endsWith("]")) {
                    String innerTypes = pythonType.substring(5, pythonType.length() - 1);
                    String[] types = innerTypes.split(",");
                    if (types.length >= 2) {
                        String keyType = mapPythonTypeToJava(types[0].trim());
                        String valueType = mapPythonTypeToJava(types[1].trim());
                        return "Map<" + keyType + ", " + valueType + ">";
                    }
                    return "Map";
                }
                
                // Capitalize first letter for class names
                return pythonType.substring(0, 1).toUpperCase() + pythonType.substring(1);
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