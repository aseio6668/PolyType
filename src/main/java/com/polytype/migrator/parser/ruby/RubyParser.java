package com.polytype.migrator.parser.ruby;

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
 * Parser for Ruby source code that converts it to AST representation.
 * Supports Ruby's classes, modules, blocks, metaprogramming, and dynamic language features.
 */
public class RubyParser implements Parser {
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "class\\s+(\\w+)(?:\\s*<\\s*(\\w+))?(?:\\s*$|\\s*#)"
    );
    
    private static final Pattern MODULE_PATTERN = Pattern.compile(
        "module\\s+(\\w+)(?:\\s*$|\\s*#)"
    );
    
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "def\\s+(self\\.)?(\\w+[?!]?)(?:\\s*\\(([^)]*)\\))?(?:\\s*$|\\s*#)"
    );
    
    private static final Pattern ATTR_PATTERN = Pattern.compile(
        "(attr_reader|attr_writer|attr_accessor)\\s+:([\\w\\s,:]+)"
    );
    
    private static final Pattern INSTANCE_VAR_PATTERN = Pattern.compile(
        "@(\\w+)\\s*=\\s*([^#\\n]+)"
    );
    
    private static final Pattern CLASS_VAR_PATTERN = Pattern.compile(
        "@@(\\w+)\\s*=\\s*([^#\\n]+)"
    );
    
    private static final Pattern CONSTANT_PATTERN = Pattern.compile(
        "([A-Z][A-Z0-9_]*)\\s*=\\s*([^#\\n]+)"
    );
    
    private static final Pattern REQUIRE_PATTERN = Pattern.compile(
        "require(?:_relative)?\\s+['\"]([^'\"]+)['\"]"
    );
    
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(
        "include\\s+(\\w+)"
    );
    
    private static final Pattern EXTEND_PATTERN = Pattern.compile(
        "extend\\s+(\\w+)"
    );
    
    private static final Pattern BLOCK_PATTERN = Pattern.compile(
        "\\{\\s*\\|([^|]*)\\|"
    );
    
    private static final Pattern SYMBOL_PATTERN = Pattern.compile(
        ":(\\w+)"
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
            
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // Parse require statements
            Matcher requireMatcher = REQUIRE_PATTERN.matcher(line);
            if (requireMatcher.find()) {
                // Handle requires - could be expanded for dependency tracking
                continue;
            }
            
            // Parse include/extend statements
            Matcher includeMatcher = INCLUDE_PATTERN.matcher(line);
            if (includeMatcher.find()) {
                // Handle includes - could be expanded
                continue;
            }
            
            Matcher extendMatcher = EXTEND_PATTERN.matcher(line);
            if (extendMatcher.find()) {
                // Handle extends - could be expanded
                continue;
            }
            
            // Parse class declarations
            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            if (classMatcher.find()) {
                ClassDeclarationNode classNode = parseClass(lines, i, classMatcher);
                program.addChild(classNode);
                continue;
            }
            
            // Parse module declarations
            Matcher moduleMatcher = MODULE_PATTERN.matcher(line);
            if (moduleMatcher.find()) {
                ClassDeclarationNode moduleNode = parseModule(lines, i, moduleMatcher);
                program.addChild(moduleNode);
                continue;
            }
            
            // Parse standalone methods
            Matcher methodMatcher = METHOD_PATTERN.matcher(line);
            if (methodMatcher.find()) {
                FunctionDeclarationNode methodNode = parseMethod(lines, i, methodMatcher);
                program.addChild(methodNode);
                continue;
            }
            
            // Parse constants
            Matcher constantMatcher = CONSTANT_PATTERN.matcher(line);
            if (constantMatcher.find()) {
                VariableDeclarationNode constantNode = parseConstant(constantMatcher, i + 1);
                if (constantNode != null) {
                    program.addChild(constantNode);
                }
                continue;
            }
        }
    }
    
    private ClassDeclarationNode parseClass(List<String> lines, int startIndex, Matcher classMatcher) {
        String className = classMatcher.group(1);
        String superClass = classMatcher.group(2);
        
        ClassDeclarationNode classNode = new ClassDeclarationNode(className, false, startIndex + 1, 1);
        
        // Parse class body
        parseClassBody(lines, startIndex, classNode);
        
        return classNode;
    }
    
    private ClassDeclarationNode parseModule(List<String> lines, int startIndex, Matcher moduleMatcher) {
        String moduleName = moduleMatcher.group(1);
        
        ClassDeclarationNode moduleNode = new ClassDeclarationNode(moduleName, false, startIndex + 1, 1);
        // Could add metadata about being a module
        
        parseClassBody(lines, startIndex, moduleNode);
        
        return moduleNode;
    }
    
    private void parseClassBody(List<String> lines, int startIndex, ClassDeclarationNode classNode) {
        boolean inClass = false;
        int indentLevel = getIndentLevel(lines.get(startIndex));
        
        for (int i = startIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmedLine = line.trim();
            
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }
            
            // Check if we've left the class (by indentation or 'end' keyword)
            int currentIndent = getIndentLevel(line);
            if (trimmedLine.equals("end") && currentIndent <= indentLevel) {
                break;
            }
            
            // Only parse if we're inside the class
            if (currentIndent > indentLevel || !inClass) {
                inClass = true;
                parseClassMember(trimmedLine, classNode, i + 1);
            }
        }
    }
    
    private void parseClassMember(String line, ClassDeclarationNode classNode, int lineNumber) {
        // Parse attr_* declarations
        Matcher attrMatcher = ATTR_PATTERN.matcher(line);
        if (attrMatcher.find()) {
            parseAttrDeclaration(attrMatcher, classNode, lineNumber);
            return;
        }
        
        // Parse method definitions
        Matcher methodMatcher = METHOD_PATTERN.matcher(line);
        if (methodMatcher.find()) {
            FunctionDeclarationNode method = parseClassMethod(methodMatcher, lineNumber);
            classNode.addChild(method);
            return;
        }
        
        // Parse instance variables
        Matcher instanceVarMatcher = INSTANCE_VAR_PATTERN.matcher(line);
        if (instanceVarMatcher.find()) {
            VariableDeclarationNode instanceVar = parseInstanceVariable(instanceVarMatcher, lineNumber);
            if (instanceVar != null) {
                classNode.addChild(instanceVar);
            }
            return;
        }
        
        // Parse class variables
        Matcher classVarMatcher = CLASS_VAR_PATTERN.matcher(line);
        if (classVarMatcher.find()) {
            VariableDeclarationNode classVar = parseClassVariable(classVarMatcher, lineNumber);
            if (classVar != null) {
                classNode.addChild(classVar);
            }
            return;
        }
        
        // Parse constants
        Matcher constantMatcher = CONSTANT_PATTERN.matcher(line);
        if (constantMatcher.find()) {
            VariableDeclarationNode constant = parseConstant(constantMatcher, lineNumber);
            if (constant != null) {
                classNode.addChild(constant);
            }
            return;
        }
    }
    
    private void parseAttrDeclaration(Matcher attrMatcher, ClassDeclarationNode classNode, int lineNumber) {
        String attrType = attrMatcher.group(1); // attr_reader, attr_writer, attr_accessor
        String attributes = attrMatcher.group(2);
        
        String[] attrs = attributes.split(",");
        for (String attr : attrs) {
            attr = attr.trim();
            if (attr.startsWith(":")) {
                attr = attr.substring(1);
            }
            
            // Create a property for each attribute
            VariableDeclarationNode property = new VariableDeclarationNode(
                attr, "Object", false, null, lineNumber, 1);
            classNode.addChild(property);
            
            // Generate getter method
            if (attrType.equals("attr_reader") || attrType.equals("attr_accessor")) {
                FunctionDeclarationNode getter = new FunctionDeclarationNode(
                    attr, "Object", new ArrayList<>(), true, false, lineNumber, 1);
                classNode.addChild(getter);
            }
            
            // Generate setter method
            if (attrType.equals("attr_writer") || attrType.equals("attr_accessor")) {
                List<ParameterNode> setterParams = new ArrayList<>();
                setterParams.add(new ParameterNode("value", "Object", false, lineNumber, 1));
                
                FunctionDeclarationNode setter = new FunctionDeclarationNode(
                    attr + "=", "Object", setterParams, true, false, lineNumber, 1);
                classNode.addChild(setter);
            }
        }
    }
    
    private FunctionDeclarationNode parseClassMethod(Matcher methodMatcher, int lineNumber) {
        String selfKeyword = methodMatcher.group(1); // "self." for class methods
        String methodName = methodMatcher.group(2);
        String parameters = methodMatcher.group(3);
        
        boolean isStatic = selfKeyword != null;
        String returnType = inferReturnTypeFromName(methodName);
        
        List<ParameterNode> paramList = parseParameters(parameters);
        
        FunctionDeclarationNode method = new FunctionDeclarationNode(
            methodName, returnType, paramList, true, false, lineNumber, 1);
        method.setStatic(isStatic);
        
        return method;
    }
    
    private FunctionDeclarationNode parseMethod(List<String> lines, int startIndex, Matcher methodMatcher) {
        String selfKeyword = methodMatcher.group(1);
        String methodName = methodMatcher.group(2);
        String parameters = methodMatcher.group(3);
        
        boolean isStatic = selfKeyword != null;
        String returnType = inferReturnTypeFromName(methodName);
        
        List<ParameterNode> paramList = parseParameters(parameters);
        
        FunctionDeclarationNode method = new FunctionDeclarationNode(
            methodName, returnType, paramList, false, false, startIndex + 1, 1);
        method.setStatic(isStatic);
        
        return method;
    }
    
    private VariableDeclarationNode parseInstanceVariable(Matcher instanceVarMatcher, int lineNumber) {
        String varName = instanceVarMatcher.group(1);
        String value = instanceVarMatcher.group(2);
        
        String type = inferTypeFromValue(value);
        
        VariableDeclarationNode instanceVar = new VariableDeclarationNode(
            varName, type, false, value, lineNumber, 1);
        
        return instanceVar;
    }
    
    private VariableDeclarationNode parseClassVariable(Matcher classVarMatcher, int lineNumber) {
        String varName = classVarMatcher.group(1);
        String value = classVarMatcher.group(2);
        
        String type = inferTypeFromValue(value);
        
        VariableDeclarationNode classVar = new VariableDeclarationNode(
            varName, type, false, value, lineNumber, 1);
        
        return classVar;
    }
    
    private VariableDeclarationNode parseConstant(Matcher constantMatcher, int lineNumber) {
        String constantName = constantMatcher.group(1);
        String value = constantMatcher.group(2);
        
        String type = inferTypeFromValue(value);
        
        VariableDeclarationNode constant = new VariableDeclarationNode(
            constantName, type, true, value, lineNumber, 1);
        
        return constant;
    }
    
    private List<ParameterNode> parseParameters(String parameters) {
        List<ParameterNode> paramList = new ArrayList<>();
        
        if (parameters == null || parameters.trim().isEmpty()) {
            return paramList;
        }
        
        String[] params = parameters.split(",");
        for (String param : params) {
            param = param.trim();
            
            String paramName;
            String defaultValue = null;
            boolean optional = false;
            
            // Handle default parameters
            if (param.contains("=")) {
                String[] parts = param.split("=", 2);
                paramName = parts[0].trim();
                defaultValue = parts[1].trim();
                optional = true;
            } else {
                paramName = param;
            }
            
            // Handle splat operators
            if (paramName.startsWith("*")) {
                paramName = paramName.substring(1);
                // Could mark as variadic
            }
            
            // Handle keyword parameters
            if (paramName.endsWith(":")) {
                paramName = paramName.substring(0, paramName.length() - 1);
                // Could mark as keyword parameter
            }
            
            String type = inferTypeFromValue(defaultValue);
            ParameterNode parameter = new ParameterNode(paramName, type, optional, 0, 0);
            paramList.add(parameter);
        }
        
        return paramList;
    }
    
    private String inferReturnTypeFromName(String methodName) {
        // Ruby naming conventions
        if (methodName.endsWith("?")) {
            return "Boolean"; // Predicate methods
        }
        if (methodName.endsWith("!")) {
            return "Object"; // Mutating methods
        }
        if (methodName.equals("initialize")) {
            return "void"; // Constructor
        }
        if (methodName.startsWith("get_") || methodName.startsWith("find_")) {
            return "Object";
        }
        if (methodName.startsWith("set_") || methodName.startsWith("create_") || 
            methodName.startsWith("update_") || methodName.startsWith("delete_")) {
            return "Object";
        }
        if (methodName.startsWith("is_") || methodName.startsWith("has_") || 
            methodName.startsWith("can_") || methodName.startsWith("should_")) {
            return "Boolean";
        }
        if (methodName.contains("count") || methodName.contains("size") || 
            methodName.contains("length")) {
            return "Integer";
        }
        if (methodName.contains("list") || methodName.contains("all") ||
            methodName.endsWith("s")) {
            return "List";
        }
        
        return "Object";
    }
    
    private String inferTypeFromValue(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("nil")) {
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
        if (value.startsWith("[") && value.endsWith("]")) {
            return "List";
        }
        
        // Hashes
        if (value.startsWith("{") && value.endsWith("}")) {
            return "Map";
        }
        
        // Symbols
        if (value.startsWith(":")) {
            return "String"; // Treating symbols as strings
        }
        
        // Regular expressions
        if (value.startsWith("/") && value.endsWith("/")) {
            return "Object"; // Regex object
        }
        
        // Class instantiation
        if (value.contains(".new")) {
            return "Object";
        }
        
        return "Object";
    }
    
    private int getIndentLevel(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                count++;
            } else if (c == '\t') {
                count += 4; // Treat tab as 4 spaces
            } else {
                break;
            }
        }
        return count;
    }
    
    @Override
    public boolean supportsFile(String fileName) {
        return fileName.toLowerCase().endsWith(".rb") ||
               fileName.toLowerCase().endsWith(".ruby") ||
               fileName.toLowerCase().endsWith(".rake") ||
               fileName.toLowerCase().equals("rakefile") ||
               fileName.toLowerCase().equals("gemfile");
    }
    
    @Override
    public String getLanguageName() {
        return "Ruby";
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".rb", ".ruby", ".rake"};
    }
}