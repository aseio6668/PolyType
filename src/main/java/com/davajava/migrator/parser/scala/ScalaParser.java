package com.davajava.migrator.parser.scala;

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

public class ScalaParser implements Parser {
    private static final Logger logger = Logger.getLogger(ScalaParser.class.getName());
    
    // Scala specific patterns
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
        "^\\s*package\\s+([\\w\\.]+)",
        Pattern.MULTILINE
    );
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "^\\s*(?:(abstract|final|sealed)\\s+)?class\\s+(\\w+)(?:\\[([^\\]]+)\\])?(?:\\s*\\(([^)]*)\\))?(?:\\s+extends\\s+([\\w\\s\\[\\],<>]+))?(?:\\s+with\\s+([\\w\\s\\[\\],<>]+))?\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern CASE_CLASS_PATTERN = Pattern.compile(
        "^\\s*case\\s+class\\s+(\\w+)(?:\\[([^\\]]+)\\])?\\s*\\(([^)]*)\\)",
        Pattern.MULTILINE
    );
    
    private static final Pattern OBJECT_PATTERN = Pattern.compile(
        "^\\s*(?:(case)\\s+)?object\\s+(\\w+)(?:\\s+extends\\s+([\\w\\s\\[\\],<>]+))?(?:\\s+with\\s+([\\w\\s\\[\\],<>]+))?\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern TRAIT_PATTERN = Pattern.compile(
        "^\\s*trait\\s+(\\w+)(?:\\[([^\\]]+)\\])?(?:\\s+extends\\s+([\\w\\s\\[\\],<>]+))?\\s*\\{([^}]*)\\}",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "^\\s*(?:(override|final|private|protected)\\s+)?def\\s+(\\w+)(?:\\[([^\\]]+)\\])?\\s*\\(([^)]*)\\)(?:\\s*:\\s*([^\\=\\{]+))?(?:\\s*=|\\s*\\{)",
        Pattern.MULTILINE
    );
    
    private static final Pattern VAL_VAR_PATTERN = Pattern.compile(
        "^\\s*(val|var)\\s+(\\w+)(?:\\s*:\\s*([^\\=\\n]+))?(?:\\s*=\\s*([^\\n]+))?",
        Pattern.MULTILINE
    );

    @Override
    public ASTNode parse(String sourceCode) throws ParseException {
        try {
            sourceCode = preprocessSource(sourceCode);
            ProgramNode program = new ProgramNode(1, 1);
            
            // Parse case classes first (immutable data structures)
            Matcher caseClassMatcher = CASE_CLASS_PATTERN.matcher(sourceCode);
            while (caseClassMatcher.find()) {
                String className = caseClassMatcher.group(1);
                String generics = caseClassMatcher.group(2);
                String parameters = caseClassMatcher.group(3);
                
                ClassDeclarationNode caseClass = parseCaseClass(className, parameters, 
                    getLineNumber(sourceCode, caseClassMatcher.start()));
                program.addChild(caseClass);
            }
            
            // Parse traits (similar to interfaces)
            Matcher traitMatcher = TRAIT_PATTERN.matcher(sourceCode);
            while (traitMatcher.find()) {
                String traitName = traitMatcher.group(1);
                String generics = traitMatcher.group(2);
                String superTypes = traitMatcher.group(3);
                String traitBody = traitMatcher.group(4);
                
                ClassDeclarationNode traitClass = parseTrait(traitName, traitBody, 
                    getLineNumber(sourceCode, traitMatcher.start()));
                program.addChild(traitClass);
            }
            
            // Parse objects (singletons)
            Matcher objectMatcher = OBJECT_PATTERN.matcher(sourceCode);
            while (objectMatcher.find()) {
                String caseObject = objectMatcher.group(1);
                String objectName = objectMatcher.group(2);
                String superTypes = objectMatcher.group(3);
                String mixins = objectMatcher.group(4);
                String objectBody = objectMatcher.group(5);
                
                ClassDeclarationNode objectClass = parseObject(objectName, objectBody, 
                    getLineNumber(sourceCode, objectMatcher.start()));
                program.addChild(objectClass);
            }
            
            // Parse regular classes
            Matcher classMatcher = CLASS_PATTERN.matcher(sourceCode);
            while (classMatcher.find()) {
                String modifiers = classMatcher.group(1);
                String className = classMatcher.group(2);
                String generics = classMatcher.group(3);
                String constructorParams = classMatcher.group(4);
                String superClass = classMatcher.group(5);
                String mixins = classMatcher.group(6);
                String classBody = classMatcher.group(7);
                
                ClassDeclarationNode classNode = parseClass(className, modifiers, constructorParams, 
                    classBody, getLineNumber(sourceCode, classMatcher.start()));
                program.addChild(classNode);
            }
            
            return program;
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse Scala source code", e);
        }
    }

    @Override
    public ASTNode parseFile(String filePath) throws IOException, ParseException {
        String content = Files.readString(Paths.get(filePath));
        return parse(content);
    }

    @Override
    public SourceLanguage getSupportedLanguage() {
        return SourceLanguage.SCALA;
    }

    @Override
    public boolean canHandle(String fileName) {
        return fileName.endsWith(".scala") || fileName.endsWith(".sc");
    }
    
    private String preprocessSource(String sourceCode) {
        // Remove comments
        sourceCode = sourceCode.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        sourceCode = sourceCode.replaceAll("//.*", "");
        
        // Remove imports for now (basic preprocessing)
        sourceCode = sourceCode.replaceAll("^\\s*import\\s+[^\\n]+\\n", "");
        
        return sourceCode;
    }
    
    private ClassDeclarationNode parseCaseClass(String className, String parameters, int lineNumber) {
        ClassDeclarationNode caseClass = new ClassDeclarationNode(className, true, lineNumber, 1);
        
        // Parse parameters as immutable fields
        List<ParameterNode> params = parseParameters(parameters);
        for (ParameterNode param : params) {
            // Case class parameters are immutable fields with getters
            VariableDeclarationNode field = new VariableDeclarationNode(
                param.getName(), param.getDataType(), false, null, lineNumber, 1
            );
            caseClass.addChild(field);
            
            // Generate getter
            FunctionDeclarationNode getter = new FunctionDeclarationNode(
                "get" + capitalize(param.getName()), param.getDataType(), new ArrayList<>(), true, false, lineNumber, 1
            );
            caseClass.addChild(getter);
        }
        
        // Add case class methods (equals, hashCode, toString, copy)
        addCaseClassMethods(caseClass, className);
        
        return caseClass;
    }
    
    private ClassDeclarationNode parseTrait(String traitName, String traitBody, int lineNumber) {
        // Traits become abstract classes or interfaces in Java
        ClassDeclarationNode traitClass = new ClassDeclarationNode(traitName, true, lineNumber, 1);
        
        if (traitBody != null) {
            parseClassBody(traitClass, traitBody, true);
        }
        
        return traitClass;
    }
    
    private ClassDeclarationNode parseObject(String objectName, String objectBody, int lineNumber) {
        // Objects become classes with static members (singleton pattern)
        ClassDeclarationNode objectClass = new ClassDeclarationNode(objectName, true, lineNumber, 1);
        
        // Add singleton pattern
        addSingletonPattern(objectClass, objectName);
        
        if (objectBody != null) {
            parseClassBody(objectClass, objectBody, false);
        }
        
        return objectClass;
    }
    
    private ClassDeclarationNode parseClass(String className, String modifiers, String constructorParams, 
                                          String classBody, int lineNumber) {
        ClassDeclarationNode classNode = new ClassDeclarationNode(className, true, lineNumber, 1);
        
        // Parse primary constructor if exists
        if (constructorParams != null && !constructorParams.trim().isEmpty()) {
            List<ParameterNode> params = parseParameters(constructorParams);
            FunctionDeclarationNode constructor = new FunctionDeclarationNode(
                className, "void", params, true, false, lineNumber, 1
            );
            classNode.addChild(constructor);
        }
        
        // Parse class body
        if (classBody != null) {
            parseClassBody(classNode, classBody, false);
        }
        
        return classNode;
    }
    
    private void parseClassBody(ClassDeclarationNode classNode, String classBody, boolean isTrait) {
        // Parse val/var declarations
        Matcher valVarMatcher = VAL_VAR_PATTERN.matcher(classBody);
        while (valVarMatcher.find()) {
            String keyword = valVarMatcher.group(1);
            String fieldName = valVarMatcher.group(2);
            String fieldType = valVarMatcher.group(3);
            String initializer = valVarMatcher.group(4);
            
            if (fieldType != null) {
                fieldType = mapScalaTypeToJava(fieldType.trim());
            } else {
                fieldType = "Object"; // Type inference
            }
            
            boolean isMutable = "var".equals(keyword);
            
            VariableDeclarationNode field = new VariableDeclarationNode(
                fieldName, fieldType, isMutable, null, 1, 1
            );
            classNode.addChild(field);
            
            // Generate getter
            FunctionDeclarationNode getter = new FunctionDeclarationNode(
                "get" + capitalize(fieldName), fieldType, new ArrayList<>(), true, false, 1, 1
            );
            classNode.addChild(getter);
            
            // Generate setter for var fields
            if (isMutable) {
                List<ParameterNode> setterParams = new ArrayList<>();
                setterParams.add(new ParameterNode("value", fieldType, false, 1, 1));
                FunctionDeclarationNode setter = new FunctionDeclarationNode(
                    "set" + capitalize(fieldName), "void", setterParams, true, false, 1, 1
                );
                classNode.addChild(setter);
            }
        }
        
        // Parse methods
        Matcher methodMatcher = METHOD_PATTERN.matcher(classBody);
        while (methodMatcher.find()) {
            String modifiers = methodMatcher.group(1);
            String methodName = methodMatcher.group(2);
            String generics = methodMatcher.group(3);
            String parameters = methodMatcher.group(4);
            String returnType = methodMatcher.group(5);
            
            returnType = returnType != null ? mapScalaTypeToJava(returnType.trim()) : "Object";
            List<ParameterNode> paramList = parseParameters(parameters);
            
            boolean isAbstract = isTrait && !methodName.equals("apply");
            
            FunctionDeclarationNode method = new FunctionDeclarationNode(
                methodName, returnType, paramList, true, false, 1, 1
            );
            classNode.addChild(method);
        }
    }
    
    private void addCaseClassMethods(ClassDeclarationNode caseClass, String className) {
        // equals method
        List<ParameterNode> equalsParams = new ArrayList<>();
        equalsParams.add(new ParameterNode("other", "Object", false, 1, 1));
        FunctionDeclarationNode equals = new FunctionDeclarationNode(
            "equals", "boolean", equalsParams, true, false, 1, 1
        );
        caseClass.addChild(equals);
        
        // hashCode method
        FunctionDeclarationNode hashCode = new FunctionDeclarationNode(
            "hashCode", "int", new ArrayList<>(), true, false, 1, 1
        );
        caseClass.addChild(hashCode);
        
        // toString method
        FunctionDeclarationNode toString = new FunctionDeclarationNode(
            "toString", "String", new ArrayList<>(), true, false, 1, 1
        );
        caseClass.addChild(toString);
    }
    
    private void addSingletonPattern(ClassDeclarationNode objectClass, String objectName) {
        // Private constructor
        FunctionDeclarationNode privateConstructor = new FunctionDeclarationNode(
            objectName, "void", new ArrayList<>(), false, false, 1, 1
        );
        objectClass.addChild(privateConstructor);
        
        // getInstance method
        FunctionDeclarationNode getInstance = new FunctionDeclarationNode(
            "getInstance", objectName, new ArrayList<>(), true, true, 1, 1
        );
        objectClass.addChild(getInstance);
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
            
            // Handle Scala parameter format: name: Type = default
            String[] parts = param.split(":");
            if (parts.length >= 2) {
                String name = parts[0].trim();
                String type = parts[1].split("=")[0].trim(); // Remove default value
                
                // Remove val/var keywords
                name = name.replaceAll("^(val|var)\\s+", "");
                
                type = mapScalaTypeToJava(type);
                parameters.add(new ParameterNode(name, type, false, 1, 1));
            }
        }
        
        return parameters;
    }
    
    private String mapScalaTypeToJava(String scalaType) {
        scalaType = scalaType.trim();
        
        // Handle Scala basic types
        switch (scalaType) {
            case "Int": return "int";
            case "Long": return "long";
            case "Short": return "short";
            case "Byte": return "byte";
            case "Double": return "double";
            case "Float": return "float";
            case "Boolean": return "boolean";
            case "Char": return "char";
            case "String": return "String";
            case "Any": return "Object";
            case "AnyRef": return "Object";
            case "Unit": return "void";
            case "Nothing": return "void";
            
            // Scala collections
            case "List": return "List";
            case "Vector": return "List";
            case "Array": return "Array";
            case "Set": return "Set";
            case "Map": return "Map";
            case "Option": return "Optional";
            case "Some": return "Optional";
            case "None": return "Optional";
            
            default:
                // Handle generic types
                if (scalaType.contains("[") && scalaType.contains("]")) {
                    return scalaType.replace("[", "<").replace("]", ">");
                }
                
                // Custom types
                return scalaType;
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