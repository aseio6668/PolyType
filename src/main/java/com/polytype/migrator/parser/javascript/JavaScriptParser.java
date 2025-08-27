package com.polytype.migrator.parser.javascript;

import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.parser.base.Parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for JavaScript source code that converts it to AST representation.
 * Supports ES6+ features including classes, arrow functions, async/await, and modules.
 */
public class JavaScriptParser implements Parser {
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "(?:export\\s+)?(?:default\\s+)?class\\s+(\\w+)(?:\\s+extends\\s+(\\w+))?\\s*\\{"
    );
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "(?:(async)\\s+)?(?:(static)\\s+)?(?:function\\s+)?(\\w+)\\s*\\([^)]*\\)\\s*(?:\\{|=>)"
    );
    
    private static final Pattern ARROW_FUNCTION_PATTERN = Pattern.compile(
        "(?:const|let|var)\\s+(\\w+)\\s*=\\s*(?:(async)\\s+)?\\([^)]*\\)\\s*=>\\s*"
    );
    
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "(?:(async)\\s+)?(?:(static)\\s+)?(?:(get|set)\\s+)?(\\w+)\\s*\\([^)]*\\)\\s*\\{"
    );
    
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
        "(?:this\\.)?(\\w+)\\s*=\\s*([^;]+);"
    );
    
    private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile(
        "constructor\\s*\\([^)]*\\)\\s*\\{"
    );
    
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(
        "(\\w+)(?:\\s*=\\s*([^,)]+))?"
    );
    
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
        "import\\s+(?:\\{([^}]+)\\}|([^\\s]+))\\s+from\\s+['\"]([^'\"]+)['\"]"
    );
    
    private static final Pattern EXPORT_PATTERN = Pattern.compile(
        "export\\s+(?:default\\s+)?(?:class|function|const|let|var)\\s+(\\w+)"
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
            
            // Parse standalone functions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(line);
            if (functionMatcher.find()) {
                FunctionDeclarationNode functionNode = parseFunction(lines, i, functionMatcher);
                program.addChild(functionNode);
                continue;
            }
            
            // Parse arrow functions
            Matcher arrowMatcher = ARROW_FUNCTION_PATTERN.matcher(line);
            if (arrowMatcher.find()) {
                FunctionDeclarationNode functionNode = parseArrowFunction(lines, i, arrowMatcher);
                program.addChild(functionNode);
                continue;
            }
        }
    }
    
    private ClassDeclarationNode parseClass(List<String> lines, int startIndex, Matcher classMatcher) {
        String className = classMatcher.group(1);
        String superClass = classMatcher.group(2);
        
        ClassDeclarationNode classNode = new ClassDeclarationNode(className, false, startIndex + 1, 1);
        
        // Find class body
        int braceCount = 0;
        boolean inClass = false;
        
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    braceCount++;
                    inClass = true;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0 && inClass) {
                        return classNode; // End of class
                    }
                }
            }
            
            if (inClass && braceCount == 1) {
                // Parse class members
                parseClassMember(line, classNode, i + 1);
            }
        }
        
        return classNode;
    }
    
    private void parseClassMember(String line, ClassDeclarationNode classNode, int lineNumber) {
        // Parse constructor
        Matcher constructorMatcher = CONSTRUCTOR_PATTERN.matcher(line);
        if (constructorMatcher.find()) {
            FunctionDeclarationNode constructor = new FunctionDeclarationNode(
                classNode.getName(), "void", parseParameters(line), true, true, lineNumber, 1);
            classNode.addChild(constructor);
            return;
        }
        
        // Parse methods
        Matcher methodMatcher = METHOD_PATTERN.matcher(line);
        if (methodMatcher.find()) {
            String asyncKeyword = methodMatcher.group(1);
            String staticKeyword = methodMatcher.group(2);
            String accessorType = methodMatcher.group(3); // get/set
            String methodName = methodMatcher.group(4);
            
            boolean isAsync = asyncKeyword != null;
            boolean isStatic = staticKeyword != null;
            String returnType = inferReturnType(methodName, accessorType);
            
            FunctionDeclarationNode method = new FunctionDeclarationNode(
                methodName, returnType, parseParameters(line), true, false, lineNumber, 1);
            method.setAsync(isAsync);
            method.setStatic(isStatic);
            
            classNode.addChild(method);
            return;
        }
        
        // Parse properties (this.property = value)
        Matcher propertyMatcher = PROPERTY_PATTERN.matcher(line);
        if (propertyMatcher.find()) {
            String propertyName = propertyMatcher.group(1);
            String defaultValue = propertyMatcher.group(2);
            
            String type = inferTypeFromValue(defaultValue);
            VariableDeclarationNode property = new VariableDeclarationNode(
                propertyName, type, false, defaultValue, lineNumber, 1);
            
            classNode.addChild(property);
        }
    }
    
    private FunctionDeclarationNode parseFunction(List<String> lines, int startIndex, Matcher functionMatcher) {
        String asyncKeyword = functionMatcher.group(1);
        String staticKeyword = functionMatcher.group(2);
        String functionName = functionMatcher.group(3);
        
        boolean isAsync = asyncKeyword != null;
        boolean isStatic = staticKeyword != null;
        String returnType = inferReturnType(functionName, null);
        
        List<ParameterNode> parameters = parseParameters(lines.get(startIndex));
        
        FunctionDeclarationNode function = new FunctionDeclarationNode(
            functionName, returnType, parameters, false, false, startIndex + 1, 1);
        function.setAsync(isAsync);
        function.setStatic(isStatic);
        
        return function;
    }
    
    private FunctionDeclarationNode parseArrowFunction(List<String> lines, int startIndex, Matcher arrowMatcher) {
        String functionName = arrowMatcher.group(1);
        String asyncKeyword = arrowMatcher.group(2);
        
        boolean isAsync = asyncKeyword != null;
        String returnType = isAsync ? "Promise" : "Object";
        
        List<ParameterNode> parameters = parseParameters(lines.get(startIndex));
        
        FunctionDeclarationNode function = new FunctionDeclarationNode(
            functionName, returnType, parameters, false, false, startIndex + 1, 1);
        function.setAsync(isAsync);
        
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
                    
                    // Handle destructuring and default parameters
                    Matcher paramMatcher = PARAMETER_PATTERN.matcher(param);
                    if (paramMatcher.find()) {
                        String paramName = paramMatcher.group(1);
                        String defaultValue = paramMatcher.group(2);
                        
                        String type = inferTypeFromValue(defaultValue != null ? defaultValue : "undefined");
                        boolean optional = defaultValue != null;
                        
                        ParameterNode parameter = new ParameterNode(paramName, type, optional, 0, 0);
                        parameters.add(parameter);
                    } else if (!param.isEmpty()) {
                        // Simple parameter
                        ParameterNode parameter = new ParameterNode(param, "Object", false, 0, 0);
                        parameters.add(parameter);
                    }
                }
            }
        }
        
        return parameters;
    }
    
    private String inferTypeFromValue(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("undefined") || value.equals("null")) {
            return "Object";
        }
        
        value = value.trim();
        
        // String literals
        if ((value.startsWith("\"") && value.endsWith("\"")) || 
            (value.startsWith("'") && value.endsWith("'"))) {
            return "String";
        }
        
        // Template literals
        if (value.startsWith("`") && value.endsWith("`")) {
            return "String";
        }
        
        // Numbers
        if (value.matches("-?\\d+(\\.\\d+)?")) {
            return value.contains(".") ? "Double" : "Integer";
        }
        
        // Booleans
        if (value.equals("true") || value.equals("false")) {
            return "Boolean";
        }
        
        // Arrays
        if (value.startsWith("[") && value.endsWith("]")) {
            return "List";
        }
        
        // Objects
        if (value.startsWith("{") && value.endsWith("}")) {
            return "Object";
        }
        
        // Functions
        if (value.contains("=>") || value.startsWith("function")) {
            return "Function";
        }
        
        // Regular expressions
        if (value.startsWith("/") && value.matches("/.*/[gimuy]*")) {
            return "RegExp";
        }
        
        return "Object";
    }
    
    private String inferReturnType(String functionName, String accessorType) {
        if (accessorType != null) {
            switch (accessorType) {
                case "get":
                    return "Object";
                case "set":
                    return "void";
            }
        }
        
        // Common naming conventions
        if (functionName.startsWith("is") || functionName.startsWith("has") || 
            functionName.startsWith("can") || functionName.startsWith("should")) {
            return "Boolean";
        }
        
        if (functionName.startsWith("get")) {
            return "Object";
        }
        
        if (functionName.startsWith("set") || functionName.startsWith("add") || 
            functionName.startsWith("remove") || functionName.startsWith("delete") ||
            functionName.startsWith("update") || functionName.startsWith("create")) {
            return "void";
        }
        
        if (functionName.startsWith("find") || functionName.startsWith("search") ||
            functionName.startsWith("load") || functionName.startsWith("fetch")) {
            return "Object";
        }
        
        if (functionName.startsWith("list") || functionName.contains("Array") ||
            functionName.endsWith("s") && !functionName.endsWith("ss")) {
            return "List";
        }
        
        return "Object";
    }
    
    @Override
    public boolean supportsFile(String fileName) {
        return fileName.toLowerCase().endsWith(".js") || 
               fileName.toLowerCase().endsWith(".mjs") ||
               fileName.toLowerCase().endsWith(".jsx");
    }
    
    @Override
    public String getLanguageName() {
        return "JavaScript";
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".js", ".mjs", ".jsx"};
    }
}