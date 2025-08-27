package com.polytype.migrator.parser.cpp;

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
 * Parser for C++ source code that converts it to AST representation.
 * Supports modern C++ features including classes, templates, namespaces, STL, and C++11/14/17/20 syntax.
 */
public class CppParser implements Parser {
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "(?:(template\\s*<[^>]*>)\\s*)?class\\s+(\\w+)(?:\\s*:\\s*(?:(public|private|protected)\\s+)?(\\w+))?\\s*\\{"
    );
    
    private static final Pattern STRUCT_PATTERN = Pattern.compile(
        "(?:(template\\s*<[^>]*>)\\s*)?struct\\s+(\\w+)(?:\\s*:\\s*(?:(public|private|protected)\\s+)?(\\w+))?\\s*\\{"
    );
    
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile(
        "namespace\\s+(\\w+)\\s*\\{"
    );
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "(?:(template\\s*<[^>]*>)\\s*)?(?:(virtual|static|inline|constexpr|explicit)\\s+)*([\\w:<>*&]+(?:\\s*\\*\\s*|\\s*&\\s*)?)\\s+((?:\\w+::)*(?:~?\\w+|operator[^(]+))\\s*\\([^)]*\\)(?:\\s*const)?(?:\\s*noexcept)?(?:\\s*override)?(?:\\s*final)?\\s*(?:\\{|;|=)"
    );
    
    private static final Pattern MEMBER_VARIABLE_PATTERN = Pattern.compile(
        "(?:(public|private|protected)\\s*:.*?)?((?:static\\s+|const\\s+|mutable\\s+|constexpr\\s+)*)([\\w:<>*&]+(?:\\s*\\*\\s*|\\s*&\\s*)?)\\s+(\\w+)(?:\\s*=\\s*([^;]+))?\\s*;"
    );
    
    private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile(
        "((?:explicit\\s+)?)(\\w+)\\s*\\([^)]*\\)(?:\\s*:\\s*[^{]*)?\\s*\\{"
    );
    
    private static final Pattern DESTRUCTOR_PATTERN = Pattern.compile(
        "(?:(virtual\\s+)?)~(\\w+)\\s*\\(\\)\\s*(?:noexcept)?\\s*(?:override)?\\s*(?:final)?\\s*\\{"
    );
    
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile(
        "template\\s*<([^>]+)>"
    );
    
    private static final Pattern USING_PATTERN = Pattern.compile(
        "using\\s+(?:namespace\\s+)?(\\w+(?:::\\w+)*)"
    );
    
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(
        "#include\\s*[<\"]([^>\"]+)[>\"]"
    );
    
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(
        "(?:(const|volatile)\\s+)?([\\w:<>*&]+(?:\\s*\\*\\s*|\\s*&\\s*)?)\\s*(\\w+)?(?:\\s*=\\s*([^,)]+))?"
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
            
            // Handle multiline constructs
            if (trimmed.contains("{") || inMultilineConstruct) {
                inMultilineConstruct = true;
                currentConstruct.append(" ").append(trimmed);
                
                // Count braces
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
            
            // Parse using declarations
            Matcher usingMatcher = USING_PATTERN.matcher(line);
            if (usingMatcher.find()) {
                // Handle using declarations - could be expanded for dependency tracking
                continue;
            }
            
            // Parse namespace declarations
            Matcher namespaceMatcher = NAMESPACE_PATTERN.matcher(line);
            if (namespaceMatcher.find()) {
                // For now, continue parsing inside namespace
                continue;
            }
            
            // Parse class declarations
            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            if (classMatcher.find()) {
                ClassDeclarationNode classNode = parseClass(line, classMatcher, i + 1);
                if (classNode != null) {
                    program.addChild(classNode);
                }
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
            
            // Parse function declarations/definitions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(line);
            if (functionMatcher.find()) {
                FunctionDeclarationNode functionNode = parseFunction(line, functionMatcher, i + 1);
                if (functionNode != null) {
                    program.addChild(functionNode);
                }
                continue;
            }
        }
    }
    
    private void parsePreprocessorDirective(String line, ProgramNode program) {
        Matcher includeMatcher = INCLUDE_PATTERN.matcher(line);
        if (includeMatcher.find()) {
            // Could track includes for dependency analysis
            return;
        }
    }
    
    private ClassDeclarationNode parseClass(String line, Matcher classMatcher, int lineNumber) {
        String templateDecl = classMatcher.group(1);
        String className = classMatcher.group(2);
        String accessSpecifier = classMatcher.group(3);
        String baseClass = classMatcher.group(4);
        
        ClassDeclarationNode classNode = new ClassDeclarationNode(className, false, lineNumber, 1);
        
        // Parse class body
        parseClassBody(line, classNode);
        
        return classNode;
    }
    
    private ClassDeclarationNode parseStruct(String line, Matcher structMatcher, int lineNumber) {
        String templateDecl = structMatcher.group(1);
        String structName = structMatcher.group(2);
        String accessSpecifier = structMatcher.group(3);
        String baseStruct = structMatcher.group(4);
        
        ClassDeclarationNode structNode = new ClassDeclarationNode(structName, true, lineNumber, 1);
        
        // Parse struct body
        parseClassBody(line, structNode);
        
        return structNode;
    }
    
    private void parseClassBody(String line, ClassDeclarationNode classNode) {
        // Extract content between braces
        int start = line.indexOf('{');
        int end = line.lastIndexOf('}');
        
        if (start >= 0 && end > start) {
            String bodyContent = line.substring(start + 1, end).trim();
            
            // Parse member variables and methods
            parseClassMembers(bodyContent, classNode);
        }
    }
    
    private void parseClassMembers(String bodyContent, ClassDeclarationNode classNode) {
        // Split by access specifiers and semicolons
        String[] members = bodyContent.split("(?:public:|private:|protected:|;)");
        
        for (String member : members) {
            member = member.trim();
            if (member.isEmpty()) continue;
            
            // Parse constructor
            Matcher constructorMatcher = CONSTRUCTOR_PATTERN.matcher(member);
            if (constructorMatcher.find()) {
                parseConstructor(member, classNode);
                continue;
            }
            
            // Parse destructor
            Matcher destructorMatcher = DESTRUCTOR_PATTERN.matcher(member);
            if (destructorMatcher.find()) {
                parseDestructor(member, classNode);
                continue;
            }
            
            // Parse member functions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(member);
            if (functionMatcher.find()) {
                FunctionDeclarationNode method = parseClassFunction(member, functionMatcher, classNode.getName());
                if (method != null) {
                    classNode.addChild(method);
                }
                continue;
            }
            
            // Parse member variables
            Matcher memberVarMatcher = MEMBER_VARIABLE_PATTERN.matcher(member + ";");
            if (memberVarMatcher.find()) {
                VariableDeclarationNode memberVar = parseMemberVariable(memberVarMatcher);
                if (memberVar != null) {
                    classNode.addChild(memberVar);
                }
                continue;
            }
        }
    }
    
    private void parseConstructor(String member, ClassDeclarationNode classNode) {
        List<ParameterNode> parameters = parseParameters(member);
        FunctionDeclarationNode constructor = new FunctionDeclarationNode(
            classNode.getName(), "void", parameters, true, true, 0, 0);
        classNode.addChild(constructor);
    }
    
    private void parseDestructor(String member, ClassDeclarationNode classNode) {
        FunctionDeclarationNode destructor = new FunctionDeclarationNode(
            "~" + classNode.getName(), "void", new ArrayList<>(), true, false, 0, 0);
        classNode.addChild(destructor);
    }
    
    private FunctionDeclarationNode parseClassFunction(String member, Matcher functionMatcher, String className) {
        String templateDecl = functionMatcher.group(1);
        String modifiers = functionMatcher.group(2);
        String returnType = functionMatcher.group(3);
        String functionName = functionMatcher.group(4);
        
        // Extract just the function name from qualified names
        if (functionName.contains("::")) {
            functionName = functionName.substring(functionName.lastIndexOf("::") + 2);
        }
        
        String mappedReturnType = mapCppType(returnType);
        List<ParameterNode> parameters = parseParameters(member);
        
        FunctionDeclarationNode method = new FunctionDeclarationNode(
            functionName, mappedReturnType, parameters, true, false, 0, 0);
        
        if (modifiers != null) {
            if (modifiers.contains("static")) {
                method.setStatic(true);
            }
            if (modifiers.contains("virtual")) {
                // Could add virtual flag
            }
        }
        
        return method;
    }
    
    private VariableDeclarationNode parseMemberVariable(Matcher memberVarMatcher) {
        String accessSpecifier = memberVarMatcher.group(1);
        String modifiers = memberVarMatcher.group(2);
        String varType = memberVarMatcher.group(3);
        String varName = memberVarMatcher.group(4);
        String initializer = memberVarMatcher.group(5);
        
        if (varType == null || varName == null) {
            return null;
        }
        
        String mappedType = mapCppType(varType.trim());
        boolean isFinal = modifiers != null && modifiers.contains("const");
        
        VariableDeclarationNode memberVar = new VariableDeclarationNode(
            varName, mappedType, isFinal, initializer, 0, 0);
        
        return memberVar;
    }
    
    private FunctionDeclarationNode parseFunction(String line, Matcher functionMatcher, int lineNumber) {
        String templateDecl = functionMatcher.group(1);
        String modifiers = functionMatcher.group(2);
        String returnType = functionMatcher.group(3);
        String functionName = functionMatcher.group(4);
        
        // Extract function name from signature
        if (functionName.contains("(")) {
            functionName = functionName.substring(0, functionName.indexOf("("));
        }
        
        // Handle qualified names
        if (functionName.contains("::")) {
            functionName = functionName.substring(functionName.lastIndexOf("::") + 2);
        }
        
        String mappedReturnType = mapCppType(returnType);
        List<ParameterNode> parameters = parseParameters(line);
        
        FunctionDeclarationNode function = new FunctionDeclarationNode(
            functionName, mappedReturnType, parameters, false, false, lineNumber, 1);
        
        if (modifiers != null) {
            if (modifiers.contains("static")) {
                function.setStatic(true);
            }
            if (modifiers.contains("inline")) {
                // Could add inline flag
            }
        }
        
        return function;
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
                        String paramType = paramMatcher.group(2);
                        String paramName = paramMatcher.group(3);
                        String defaultValue = paramMatcher.group(4);
                        
                        // Handle unnamed parameters
                        if (paramName == null) {
                            paramName = "param" + (parameters.size() + 1);
                        }
                        
                        String mappedType = mapCppType(paramType.trim());
                        boolean optional = defaultValue != null;
                        
                        ParameterNode parameter = new ParameterNode(paramName, mappedType, optional, 0, 0);
                        parameters.add(parameter);
                    }
                }
            }
        }
        
        return parameters;
    }
    
    private String mapCppType(String cppType) {
        if (cppType == null || cppType.trim().isEmpty()) {
            return "Object";
        }
        
        cppType = cppType.trim();
        
        // Remove cv-qualifiers and storage specifiers
        cppType = cppType.replaceAll("\\b(const|volatile|mutable|static|extern|register|thread_local|constexpr|inline)\\b", "").trim();
        cppType = cppType.replaceAll("\\s+", " ").trim();
        
        // Handle references and pointers
        boolean isReference = cppType.endsWith("&");
        boolean isPointer = cppType.contains("*");
        
        if (isReference) {
            cppType = cppType.substring(0, cppType.length() - 1).trim();
        }
        if (isPointer) {
            cppType = cppType.replaceAll("\\*", "").trim();
        }
        
        // Handle templates and nested types
        if (cppType.contains("<")) {
            return mapTemplateType(cppType);
        }
        
        // Handle scope resolution
        if (cppType.contains("::")) {
            cppType = cppType.substring(cppType.lastIndexOf("::") + 2);
        }
        
        switch (cppType) {
            case "void": return "void";
            case "bool": return "Boolean";
            case "char": case "signed char": case "unsigned char": return "Character";
            case "wchar_t": case "char16_t": case "char32_t": return "Character";
            case "short": case "unsigned short": return "Short";
            case "int": case "unsigned int": case "unsigned": return "Integer";
            case "long": case "unsigned long": return "Long";
            case "long long": case "unsigned long long": return "Long";
            case "float": return "Float";
            case "double": case "long double": return "Double";
            case "size_t": case "ptrdiff_t": return "Long";
            case "string": case "std::string": return "String";
            case "wstring": case "std::wstring": return "String";
            default:
                // Handle standard library types
                if (cppType.equals("string") || cppType.equals("std::string")) return "String";
                if (cppType.startsWith("vector") || cppType.startsWith("std::vector")) return "List";
                if (cppType.startsWith("map") || cppType.startsWith("std::map")) return "Map";
                if (cppType.startsWith("set") || cppType.startsWith("std::set")) return "Set";
                if (cppType.startsWith("unique_ptr") || cppType.startsWith("std::unique_ptr")) return "Object";
                if (cppType.startsWith("shared_ptr") || cppType.startsWith("std::shared_ptr")) return "Object";
                
                return cppType;
        }
    }
    
    private String mapTemplateType(String templateType) {
        if (templateType.startsWith("std::vector") || templateType.startsWith("vector")) {
            return "List";
        }
        if (templateType.startsWith("std::map") || templateType.startsWith("map")) {
            return "Map";
        }
        if (templateType.startsWith("std::set") || templateType.startsWith("set")) {
            return "Set";
        }
        if (templateType.startsWith("std::unique_ptr") || templateType.startsWith("unique_ptr")) {
            return "Object";
        }
        if (templateType.startsWith("std::shared_ptr") || templateType.startsWith("shared_ptr")) {
            return "Object";
        }
        if (templateType.startsWith("std::optional") || templateType.startsWith("optional")) {
            return "Optional";
        }
        
        return templateType;
    }
    
    @Override
    public boolean supportsFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".cpp") || lowerName.endsWith(".cc") || 
               lowerName.endsWith(".cxx") || lowerName.endsWith(".c++") ||
               lowerName.endsWith(".hpp") || lowerName.endsWith(".hh") ||
               lowerName.endsWith(".hxx") || lowerName.endsWith(".h++");
    }
    
    @Override
    public String getLanguageName() {
        return "C++";
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".cpp", ".cc", ".cxx", ".c++", ".hpp", ".hh", ".hxx", ".h++"};
    }
}