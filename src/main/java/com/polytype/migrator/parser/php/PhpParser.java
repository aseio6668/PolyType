package com.polytype.migrator.parser.php;

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
 * Parser for PHP source code that converts it to AST representation.
 * Supports PHP 7.4+ features including classes, traits, namespaces, type declarations, and modern PHP syntax.
 */
public class PhpParser implements Parser {
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "(?:(abstract|final)\\s+)?class\\s+(\\w+)(?:\\s+extends\\s+(\\w+))?(?:\\s+implements\\s+([^{]+))?\\s*\\{"
    );
    
    private static final Pattern INTERFACE_PATTERN = Pattern.compile(
        "interface\\s+(\\w+)(?:\\s+extends\\s+([^{]+))?\\s*\\{"
    );
    
    private static final Pattern TRAIT_PATTERN = Pattern.compile(
        "trait\\s+(\\w+)\\s*\\{"
    );
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "(?:(public|private|protected|static|abstract|final)\\s+)*function\\s+(\\w+)\\s*\\([^)]*\\)(?:\\s*:\\s*([^{]+))?\\s*\\{"
    );
    
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
        "(?:(public|private|protected|static|readonly)\\s+)*(?:(\\?)?([\\w\\|\\\\]+)\\s+)?\\$(\\w+)(?:\\s*=\\s*([^;]+))?\\s*;"
    );
    
    private static final Pattern CONSTANT_PATTERN = Pattern.compile(
        "(?:(public|private|protected)\\s+)?const\\s+(\\w+)\\s*=\\s*([^;]+)\\s*;"
    );
    
    private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile(
        "(?:(public|private|protected)\\s+)?function\\s+__construct\\s*\\([^)]*\\)\\s*\\{"
    );
    
    private static final Pattern MAGIC_METHOD_PATTERN = Pattern.compile(
        "(?:(public|private|protected|static)\\s+)?function\\s+(__(\\w+))\\s*\\([^)]*\\)(?:\\s*:\\s*([^{]+))?\\s*\\{"
    );
    
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile(
        "namespace\\s+([\\w\\\\]+)\\s*;"
    );
    
    private static final Pattern USE_PATTERN = Pattern.compile(
        "use\\s+([\\w\\\\]+)(?:\\s+as\\s+(\\w+))?\\s*;"
    );
    
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(
        "(?:(\\?)?([\\w\\|\\\\]+)\\s+)?\\$(\\w+)(?:\\s*=\\s*([^,)]+))?"
    );
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(
        "\\$(\\w+)(?:\\s*=\\s*([^;]+))?\\s*;"
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
        boolean inPhpTag = false;
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("#") ||
                (line.startsWith("/*") && line.endsWith("*/"))) {
                continue;
            }
            
            // Handle PHP opening/closing tags
            if (line.contains("<?php") || line.contains("<?")) {
                inPhpTag = true;
                continue;
            }
            if (line.contains("?>")) {
                inPhpTag = false;
                continue;
            }
            
            if (!inPhpTag && !line.startsWith("<?php")) {
                continue; // Skip HTML/text outside PHP tags
            }
            
            // Parse namespace declarations
            Matcher namespaceMatcher = NAMESPACE_PATTERN.matcher(line);
            if (namespaceMatcher.find()) {
                // Handle namespaces - could be expanded for dependency tracking
                continue;
            }
            
            // Parse use statements
            Matcher useMatcher = USE_PATTERN.matcher(line);
            if (useMatcher.find()) {
                // Handle use statements - could be expanded for dependency tracking
                continue;
            }
            
            // Parse class declarations
            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            if (classMatcher.find()) {
                ClassDeclarationNode classNode = parseClass(lines, i, classMatcher);
                program.addChild(classNode);
                continue;
            }
            
            // Parse interface declarations
            Matcher interfaceMatcher = INTERFACE_PATTERN.matcher(line);
            if (interfaceMatcher.find()) {
                ClassDeclarationNode interfaceNode = parseInterface(lines, i, interfaceMatcher);
                program.addChild(interfaceNode);
                continue;
            }
            
            // Parse trait declarations
            Matcher traitMatcher = TRAIT_PATTERN.matcher(line);
            if (traitMatcher.find()) {
                ClassDeclarationNode traitNode = parseTrait(lines, i, traitMatcher);
                program.addChild(traitNode);
                continue;
            }
            
            // Parse standalone functions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(line);
            if (functionMatcher.find()) {
                FunctionDeclarationNode functionNode = parseFunction(lines, i, functionMatcher);
                program.addChild(functionNode);
                continue;
            }
            
            // Parse global variables
            Matcher variableMatcher = VARIABLE_PATTERN.matcher(line);
            if (variableMatcher.find()) {
                VariableDeclarationNode varNode = parseGlobalVariable(variableMatcher, i + 1);
                if (varNode != null) {
                    program.addChild(varNode);
                }
                continue;
            }
        }
    }
    
    private ClassDeclarationNode parseClass(List<String> lines, int startIndex, Matcher classMatcher) {
        String modifier = classMatcher.group(1); // abstract, final
        String className = classMatcher.group(2);
        String superClass = classMatcher.group(3);
        String interfaces = classMatcher.group(4);
        
        ClassDeclarationNode classNode = new ClassDeclarationNode(className, false, startIndex + 1, 1);
        
        // Parse class body
        parseClassBody(lines, startIndex, classNode);
        
        return classNode;
    }
    
    private ClassDeclarationNode parseInterface(List<String> lines, int startIndex, Matcher interfaceMatcher) {
        String interfaceName = interfaceMatcher.group(1);
        String parentInterfaces = interfaceMatcher.group(2);
        
        ClassDeclarationNode interfaceNode = new ClassDeclarationNode(interfaceName, false, startIndex + 1, 1);
        interfaceNode.setInterface(true);
        
        parseClassBody(lines, startIndex, interfaceNode);
        
        return interfaceNode;
    }
    
    private ClassDeclarationNode parseTrait(List<String> lines, int startIndex, Matcher traitMatcher) {
        String traitName = traitMatcher.group(1);
        
        ClassDeclarationNode traitNode = new ClassDeclarationNode(traitName, false, startIndex + 1, 1);
        // Could add metadata about being a trait
        
        parseClassBody(lines, startIndex, traitNode);
        
        return traitNode;
    }
    
    private void parseClassBody(List<String> lines, int startIndex, ClassDeclarationNode classNode) {
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
                        return; // End of class
                    }
                }
            }
            
            if (inClass && braceCount >= 1) {
                parseClassMember(line, classNode, i + 1);
            }
        }
    }
    
    private void parseClassMember(String line, ClassDeclarationNode classNode, int lineNumber) {
        // Parse constructor
        Matcher constructorMatcher = CONSTRUCTOR_PATTERN.matcher(line);
        if (constructorMatcher.find()) {
            FunctionDeclarationNode constructor = new FunctionDeclarationNode(
                "__construct", "void", parseParameters(line), true, true, lineNumber, 1);
            classNode.addChild(constructor);
            return;
        }
        
        // Parse magic methods
        Matcher magicMethodMatcher = MAGIC_METHOD_PATTERN.matcher(line);
        if (magicMethodMatcher.find()) {
            String visibility = magicMethodMatcher.group(1);
            String methodName = magicMethodMatcher.group(2);
            String shortName = magicMethodMatcher.group(3);
            String returnType = magicMethodMatcher.group(4);
            
            String mappedReturnType = returnType != null ? mapPhpType(returnType.trim()) : "Object";
            
            FunctionDeclarationNode magicMethod = new FunctionDeclarationNode(
                methodName, mappedReturnType, parseParameters(line), true, false, lineNumber, 1);
            
            classNode.addChild(magicMethod);
            return;
        }
        
        // Parse regular methods
        Matcher methodMatcher = FUNCTION_PATTERN.matcher(line);
        if (methodMatcher.find()) {
            String modifiers = methodMatcher.group(1);
            String methodName = methodMatcher.group(2);
            String returnType = methodMatcher.group(3);
            
            String mappedReturnType = returnType != null ? mapPhpType(returnType.trim()) : "Object";
            List<ParameterNode> parameters = parseParameters(line);
            
            FunctionDeclarationNode method = new FunctionDeclarationNode(
                methodName, mappedReturnType, parameters, true, false, lineNumber, 1);
            
            if (modifiers != null && modifiers.contains("static")) {
                method.setStatic(true);
            }
            
            classNode.addChild(method);
            return;
        }
        
        // Parse properties
        Matcher propertyMatcher = PROPERTY_PATTERN.matcher(line);
        if (propertyMatcher.find()) {
            String modifiers = propertyMatcher.group(1);
            String nullable = propertyMatcher.group(2);
            String propertyType = propertyMatcher.group(3);
            String propertyName = propertyMatcher.group(4);
            String defaultValue = propertyMatcher.group(5);
            
            String mappedType = propertyType != null ? mapPhpType(propertyType) : "Object";
            boolean isReadonly = modifiers != null && modifiers.contains("readonly");
            
            VariableDeclarationNode property = new VariableDeclarationNode(
                propertyName, mappedType, isReadonly, defaultValue, lineNumber, 1);
            
            classNode.addChild(property);
            return;
        }
        
        // Parse constants
        Matcher constantMatcher = CONSTANT_PATTERN.matcher(line);
        if (constantMatcher.find()) {
            String visibility = constantMatcher.group(1);
            String constantName = constantMatcher.group(2);
            String constantValue = constantMatcher.group(3);
            
            VariableDeclarationNode constant = new VariableDeclarationNode(
                constantName, "String", true, constantValue, lineNumber, 1);
            
            classNode.addChild(constant);
        }
    }
    
    private FunctionDeclarationNode parseFunction(List<String> lines, int startIndex, Matcher functionMatcher) {
        String modifiers = functionMatcher.group(1);
        String functionName = functionMatcher.group(2);
        String returnType = functionMatcher.group(3);
        
        String mappedReturnType = returnType != null ? mapPhpType(returnType.trim()) : "Object";
        List<ParameterNode> parameters = parseParameters(lines.get(startIndex));
        
        FunctionDeclarationNode function = new FunctionDeclarationNode(
            functionName, mappedReturnType, parameters, false, false, startIndex + 1, 1);
        
        if (modifiers != null && modifiers.contains("static")) {
            function.setStatic(true);
        }
        
        return function;
    }
    
    private VariableDeclarationNode parseGlobalVariable(Matcher variableMatcher, int lineNumber) {
        String varName = variableMatcher.group(1);
        String defaultValue = variableMatcher.group(2);
        
        String type = inferTypeFromValue(defaultValue);
        
        VariableDeclarationNode variable = new VariableDeclarationNode(
            varName, type, false, defaultValue, lineNumber, 1);
        
        return variable;
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
                        String nullable = paramMatcher.group(1);
                        String paramType = paramMatcher.group(2);
                        String paramName = paramMatcher.group(3);
                        String defaultValue = paramMatcher.group(4);
                        
                        String mappedType = paramType != null ? mapPhpType(paramType) : "Object";
                        boolean optional = defaultValue != null || nullable != null;
                        
                        ParameterNode parameter = new ParameterNode(paramName, mappedType, optional, 0, 0);
                        parameters.add(parameter);
                    } else if (param.startsWith("$")) {
                        // Simple parameter without type
                        String paramName = param.substring(1);
                        if (paramName.contains("=")) {
                            paramName = paramName.substring(0, paramName.indexOf("=")).trim();
                        }
                        
                        ParameterNode parameter = new ParameterNode(paramName, "Object", false, 0, 0);
                        parameters.add(parameter);
                    }
                }
            }
        }
        
        return parameters;
    }
    
    private String mapPhpType(String phpType) {
        if (phpType == null || phpType.trim().isEmpty()) {
            return "Object";
        }
        
        phpType = phpType.trim();
        
        // Handle nullable types
        if (phpType.startsWith("?")) {
            phpType = phpType.substring(1);
        }
        
        // Handle union types (PHP 8.0+)
        if (phpType.contains("|")) {
            String[] types = phpType.split("\\|");
            // For simplicity, return the first non-null type
            for (String type : types) {
                type = type.trim();
                if (!type.equals("null")) {
                    return mapPhpType(type);
                }
            }
            return "Object";
        }
        
        switch (phpType) {
            case "void": return "void";
            case "bool": case "boolean": return "Boolean";
            case "int": case "integer": return "Integer";
            case "float": case "double": case "real": return "Double";
            case "string": return "String";
            case "array": return "List";
            case "object": return "Object";
            case "resource": return "Object";
            case "mixed": return "Object";
            case "callable": return "Object";
            case "iterable": return "List";
            case "self": case "static": case "parent": return "Object";
            default:
                // Handle class names and fully qualified names
                if (phpType.startsWith("\\")) {
                    phpType = phpType.substring(1);
                }
                if (phpType.contains("\\")) {
                    phpType = phpType.substring(phpType.lastIndexOf("\\") + 1);
                }
                
                // Common PHP classes
                if (phpType.equals("DateTime") || phpType.equals("DateTimeInterface")) {
                    return "Object";
                }
                if (phpType.equals("Exception") || phpType.equals("Throwable")) {
                    return "Object";
                }
                
                return phpType;
        }
    }
    
    private String inferTypeFromValue(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("null")) {
            return "Object";
        }
        
        value = value.trim();
        
        // String literals
        if ((value.startsWith("\"") && value.endsWith("\"")) || 
            (value.startsWith("'") && value.endsWith("'"))) {
            return "String";
        }
        
        // Numbers
        if (value.matches("-?\\d+")) {
            return "Integer";
        }
        if (value.matches("-?\\d+\\.\\d+")) {
            return "Double";
        }
        
        // Booleans
        if (value.equals("true") || value.equals("false")) {
            return "Boolean";
        }
        
        // Arrays
        if (value.startsWith("array(") || value.startsWith("[")) {
            return "List";
        }
        
        // Objects
        if (value.startsWith("new ")) {
            return "Object";
        }
        
        return "Object";
    }
    
    @Override
    public boolean supportsFile(String fileName) {
        return fileName.toLowerCase().endsWith(".php") ||
               fileName.toLowerCase().endsWith(".phtml") ||
               fileName.toLowerCase().endsWith(".php3") ||
               fileName.toLowerCase().endsWith(".php4") ||
               fileName.toLowerCase().endsWith(".php5") ||
               fileName.toLowerCase().endsWith(".phps");
    }
    
    @Override
    public String getLanguageName() {
        return "PHP";
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".php", ".phtml", ".php3", ".php4", ".php5", ".phps"};
    }
}