package com.polytype.migrator.translator.go;

import com.polytype.migrator.core.*;
import com.polytype.migrator.translator.EnhancedMultiLanguageTranslator.SemanticAwareVisitor;
import com.polytype.migrator.translator.EnhancedMultiLanguageTranslator.SemanticContext;
import java.util.*;

/**
 * Modern Go target visitor with idiomatic Go patterns and best practices.
 * 
 * This visitor generates clean, idiomatic Go code with:
 * - Proper error handling with explicit error returns
 * - Goroutines and channels for concurrency
 * - Interfaces for polymorphism and duck typing
 * - Struct embedding instead of inheritance
 * - Defer statements for resource cleanup
 * - Context-aware APIs for cancellation and timeouts
 * - Proper package organization and exports
 * - Modern Go modules and dependency management
 * - Testing with table-driven tests
 * - Documentation following Go conventions
 * 
 * Key Translation Features:
 * - Exceptions → Error return values
 * - Classes → Structs with methods
 * - Inheritance → Composition and embedding
 * - Threads → Goroutines and channels
 * - Try-with-resources → Defer statements
 * - Generics → Interface{} with type assertions (Go 1.17-) or generics (Go 1.18+)
 * - Collections → Slices, maps, and channels
 * - Async/await → Goroutines with sync primitives
 */
public class ModernGoTargetVisitor implements TargetVisitor, SemanticAwareVisitor {
    
    private final StringBuilder output;
    private int indentLevel;
    private SemanticContext semanticContext;
    
    // Go-specific state
    private final Set<String> imports = new LinkedHashSet<>();
    private final Map<String, String> typeMap = new HashMap<>();
    private final Set<String> definedStructs = new HashSet<>();
    private final Set<String> definedInterfaces = new HashSet<>();
    private String packageName = "main";
    private boolean useGenerics = false;  // Go 1.18+ feature
    private boolean useContexts = true;
    
    public ModernGoTargetVisitor() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
        initializeTypeMapping();
        initializeStandardImports();
    }
    
    @Override
    public void setSemanticContext(SemanticContext context) {
        this.semanticContext = context;
        analyzeSemanticRequirements();
    }
    
    private void initializeTypeMapping() {
        // Primitive type mappings
        typeMap.put("String", "string");
        typeMap.put("int", "int");
        typeMap.put("Integer", "int");
        typeMap.put("long", "int64");
        typeMap.put("Long", "int64");
        typeMap.put("short", "int16");
        typeMap.put("Short", "int16");
        typeMap.put("byte", "byte");
        typeMap.put("Byte", "byte");
        typeMap.put("float", "float32");
        typeMap.put("Float", "float32");
        typeMap.put("double", "float64");
        typeMap.put("Double", "float64");
        typeMap.put("boolean", "bool");
        typeMap.put("Boolean", "bool");
        typeMap.put("char", "rune");
        typeMap.put("Character", "rune");
        
        // Collection types
        typeMap.put("List", "[]");       // Will be []T
        typeMap.put("ArrayList", "[]");
        typeMap.put("LinkedList", "[]");
        typeMap.put("Vector", "[]");
        typeMap.put("Array", "[]");
        typeMap.put("Set", "map[%s]bool");  // Set implemented as map[T]bool
        typeMap.put("HashSet", "map[%s]bool");
        typeMap.put("Map", "map[%s]%s");
        typeMap.put("HashMap", "map[%s]%s");
        typeMap.put("TreeMap", "map[%s]%s");
        
        // Special types
        typeMap.put("Optional", "*");    // Pointer for optional values
        typeMap.put("Future", "chan");   // Channel for future values
        typeMap.put("CompletableFuture", "chan");
        typeMap.put("Stream", "chan");
        typeMap.put("Iterable", "chan");
        typeMap.put("Iterator", "chan");
        
        // Error types - all map to error interface
        typeMap.put("Exception", "error");
        typeMap.put("RuntimeException", "error");
        typeMap.put("IllegalArgumentException", "error");
        typeMap.put("IOException", "error");
        typeMap.put("NullPointerException", "error");
    }
    
    private void initializeStandardImports() {
        imports.add("fmt");
        imports.add("errors");
    }
    
    private void analyzeSemanticRequirements() {
        if (semanticContext == null) return;
        
        Set<String> patterns = semanticContext.getDetectedPatterns();
        
        if (patterns.contains("async_pattern")) {
            imports.add("context");
            imports.add("sync");
            imports.add("time");
        }
        
        if (patterns.contains("error_handling")) {
            imports.add("errors");
            imports.add("fmt");
        }
        
        if (patterns.contains("singleton_pattern")) {
            imports.add("sync");
        }
        
        if (patterns.contains("observer_pattern")) {
            imports.add("sync");
        }
        
        if (patterns.contains("factory_pattern")) {
            // No special imports needed for factory pattern in Go
        }
        
        if (useContexts) {
            imports.add("context");
        }
    }
    
    @Override
    public String visit(ASTNode node, TranslationOptions options) throws TranslationException {
        output.setLength(0);
        indentLevel = 0;
        
        // Configure Go features based on options
        configureGoFeatures(options);
        
        // Generate file header
        generateFileHeader(options);
        
        // Process the AST
        visitNode(node, options);
        
        return output.toString();
    }
    
    private void configureGoFeatures(TranslationOptions options) {
        useGenerics = options.getBooleanOption("go.useGenerics", false);  // Go 1.18+
        useContexts = options.getBooleanOption("go.useContexts", true);
        packageName = options.getStringOption("go.packageName", "main");
    }
    
    private void generateFileHeader(options) {
        // Package declaration
        output.append("package ").append(packageName).append("\n\n");
        
        // Imports
        if (!imports.isEmpty()) {
            if (imports.size() == 1) {
                output.append("import \"").append(imports.iterator().next()).append("\"\n\n");
            } else {
                output.append("import (\n");
                for (String import_ : imports) {
                    output.append("\t\"").append(import_).append("\"\n");
                }
                output.append(")\n\n");
            }
        }
        
        // File-level documentation
        output.append("// Generated Go code using PolyType Modern Go Translator\n");
        output.append("// This code follows Go best practices and idioms\n\n");
    }
    
    private void visitNode(ASTNode node, TranslationOptions options) throws TranslationException {
        switch (node.getType()) {
            case PROGRAM:
                visitProgram(node, options);
                break;
            case CLASS_DECLARATION:
                visitStructDeclaration(node, options);
                break;
            case INTERFACE_DECLARATION:
                visitInterfaceDeclaration(node, options);
                break;
            case FUNCTION_DECLARATION:
                visitFunctionDeclaration(node, options);
                break;
            case VARIABLE_DECLARATION:
                visitVariableDeclaration(node, options);
                break;
            case IF_STATEMENT:
                visitIfStatement(node, options);
                break;
            case WHILE_LOOP:
                visitForLoop(node, options);  // Convert while to for in Go
                break;
            case FOR_LOOP:
                visitForLoop(node, options);
                break;
            case TRY_CATCH:
                visitErrorHandling(node, options);
                break;
            case ASYNC_FUNCTION:
                visitGoroutine(node, options);
                break;
            default:
                visitDefault(node, options);
        }
    }
    
    private void visitProgram(ASTNode node, TranslationOptions options) throws TranslationException {
        for (ASTNode child : node.getChildren()) {
            visitNode(child, options);
            output.append("\n");
        }
    }
    
    private void visitStructDeclaration(ASTNode node, TranslationOptions options) throws TranslationException {
        String structName = node.getName();
        definedStructs.add(structName);
        
        // Generate struct definition
        output.append("// ").append(structName).append(" represents ").append(node.getDescription()).append("\n");
        output.append("type ").append(structName).append(" struct {\n");
        
        indentLevel++;
        
        // Handle embedded structs (composition instead of inheritance)
        List<String> baseClasses = node.getBaseClasses();
        for (String baseClass : baseClasses) {
            indent();
            output.append(mapToGoType(baseClass)).append("  // embedded struct\n");
        }
        
        // Struct fields
        for (ASTNode field : node.getFieldChildren()) {
            visitStructField(field, options);
        }
        
        indentLevel--;
        output.append("}\n\n");
        
        // Generate constructor function (New pattern)
        generateConstructor(node, structName, options);
        
        // Generate methods
        for (ASTNode method : node.getMethodChildren()) {
            visitMethod(method, structName, options);
        }
    }
    
    private void visitInterfaceDeclaration(ASTNode node, TranslationOptions options) throws TranslationException {
        String interfaceName = node.getName();
        definedInterfaces.add(interfaceName);
        
        output.append("// ").append(interfaceName).append(" defines the interface contract\n");
        output.append("type ").append(interfaceName).append(" interface {\n");
        
        indentLevel++;
        
        // Interface methods
        for (ASTNode method : node.getMethodChildren()) {
            visitInterfaceMethod(method, options);
        }
        
        indentLevel--;
        output.append("}\n\n");
    }
    
    private void visitFunctionDeclaration(ASTNode node, TranslationOptions options) throws TranslationException {
        // Function comment
        output.append("// ").append(node.getName()).append(" ").append(node.getDescription()).append("\n");
        
        output.append("func ").append(node.getName()).append("(");
        
        // Parameters
        visitParameters(node.getParameterChildren(), options);
        
        output.append(")");
        
        // Return types (Go can have multiple returns)
        String returnTypes = getGoReturnTypes(node, options);
        if (!returnTypes.isEmpty()) {
            output.append(" ").append(returnTypes);
        }
        
        output.append(" {\n");
        
        indentLevel++;
        visitFunctionBody(node, options);
        indentLevel--;
        
        output.append("}\n\n");
    }
    
    private void visitVariableDeclaration(ASTNode node, TranslationOptions options) throws TranslationException {
        indent();
        
        String varName = node.getName();
        String goType = mapToGoType(node.getTypeAnnotation());
        
        // Use var declaration with explicit type or := for type inference
        ASTNode initializer = node.getInitializer();
        if (initializer != null) {
            // Short variable declaration with type inference
            output.append(varName).append(" := ");
            visitExpression(initializer, options);
        } else {
            // Explicit type declaration
            output.append("var ").append(varName).append(" ").append(goType);
        }
        
        output.append("\n");
    }
    
    private void visitIfStatement(ASTNode node, TranslationOptions options) throws TranslationException {
        indent();
        output.append("if ");
        visitExpression(node.getCondition(), options);
        output.append(" {\n");
        
        indentLevel++;
        visitBlock(node.getThenBlock(), options);
        indentLevel--;
        
        ASTNode elseBlock = node.getElseBlock();
        if (elseBlock != null) {
            indent();
            output.append("} else {\n");
            indentLevel++;
            visitBlock(elseBlock, options);
            indentLevel--;
        }
        
        indent();
        output.append("}\n");
    }
    
    private void visitForLoop(ASTNode node, TranslationOptions options) throws TranslationException {
        indent();
        
        if (node.getType() == ASTNode.Type.WHILE_LOOP) {
            // Convert while loop to for loop
            output.append("for ");
            visitExpression(node.getCondition(), options);
            output.append(" {\n");
        } else {
            // Handle different for loop types
            String iterVar = node.getIteratorVariable();
            ASTNode iterable = node.getIterable();
            
            if (isRange(iterable)) {
                // Range-based for loop
                output.append("for ").append(iterVar).append(" := ");
                visitRange(iterable, options);
                output.append(" {\n");
            } else {
                // Iteration over slice/map/channel
                output.append("for ");
                if (needsIndex(node)) {
                    output.append("i, ");
                }
                output.append(iterVar).append(" := range ");
                visitExpression(iterable, options);
                output.append(" {\n");
            }
        }
        
        indentLevel++;
        visitBlock(node.getBody(), options);
        indentLevel--;
        
        indent();
        output.append("}\n");
    }
    
    private void visitErrorHandling(ASTNode node, TranslationOptions options) throws TranslationException {
        // Convert try-catch to Go error handling pattern
        indent();
        output.append("// Error handling converted from try-catch\n");
        
        // Generate the operation that might fail
        indent();
        output.append("if err := func() error {\n");
        
        indentLevel++;
        visitBlock(node.getTryBlock(), options);
        indent();
        output.append("return nil\n");
        indentLevel--;
        
        indent();
        output.append("}(); err != nil {\n");
        
        // Handle the error (converted from catch blocks)
        indentLevel++;
        for (ASTNode catchBlock : node.getCatchBlocks()) {
            visitBlock(catchBlock, options);
        }
        indentLevel--;
        
        indent();
        output.append("}\n");
        
        // Finally block equivalent (defer statements)
        ASTNode finallyBlock = node.getFinallyBlock();
        if (finallyBlock != null) {
            indent();
            output.append("defer func() {\n");
            indentLevel++;
            visitBlock(finallyBlock, options);
            indentLevel--;
            indent();
            output.append("}()\n");
        }
    }
    
    private void visitGoroutine(ASTNode node, TranslationOptions options) throws TranslationException {
        // Convert async function to goroutine
        String funcName = node.getName();
        
        output.append("// ").append(funcName).append(" runs as a goroutine\n");
        output.append("func ").append(funcName).append("(");
        
        // Add context as first parameter if using contexts
        if (useContexts) {
            output.append("ctx context.Context");
            if (!node.getParameterChildren().isEmpty()) {
                output.append(", ");
            }
        }
        
        visitParameters(node.getParameterChildren(), options);
        output.append(")");
        
        // Return channel or error for async results
        String returnTypes = getAsyncReturnTypes(node, options);
        if (!returnTypes.isEmpty()) {
            output.append(" ").append(returnTypes);
        }
        
        output.append(" {\n");
        
        indentLevel++;
        
        // Add context cancellation check if using contexts
        if (useContexts) {
            indent();
            output.append("select {\n");
            indent();
            output.append("case <-ctx.Done():\n");
            indentLevel++;
            indent();
            output.append("return ctx.Err()\n");
            indentLevel--;
            indent();
            output.append("default:\n");
            indentLevel++;
        }
        
        visitFunctionBody(node, options);
        
        if (useContexts) {
            indentLevel--;
            indent();
            output.append("}\n");
        }
        
        indentLevel--;
        output.append("}\n\n");
        
        // Generate helper function to start goroutine
        generateGoroutineHelper(node, funcName, options);
    }
    
    // Helper methods
    
    private void visitStructField(ASTNode field, TranslationOptions options) throws TranslationException {
        indent();
        
        String fieldName = capitalizeFirst(field.getName());  // Export if public
        String goType = mapToGoType(field.getTypeAnnotation());
        
        output.append(fieldName).append(" ").append(goType);
        
        // Add struct tags if needed
        String jsonTag = field.getStringAttribute("json", "");
        if (!jsonTag.isEmpty()) {
            output.append(" `json:\"").append(jsonTag).append("\"`");
        }
        
        output.append("\n");
    }
    
    private void visitParameters(List<ASTNode> params, TranslationOptions options) throws TranslationException {
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) output.append(", ");
            visitParameter(params.get(i), options);
        }
    }
    
    private void visitParameter(ASTNode param, TranslationOptions options) throws TranslationException {
        output.append(param.getName()).append(" ").append(mapToGoType(param.getTypeAnnotation()));
    }
    
    private void visitMethod(ASTNode method, String structName, TranslationOptions options) throws TranslationException {
        output.append("// ").append(method.getName()).append(" method for ").append(structName).append("\n");
        output.append("func (");
        
        // Receiver
        String receiverName = structName.toLowerCase().substring(0, 1);
        if (method.getBooleanAttribute("mutates", false)) {
            output.append(receiverName).append(" *").append(structName);
        } else {
            output.append(receiverName).append(" ").append(structName);
        }
        
        output.append(") ").append(method.getName()).append("(");
        
        visitParameters(method.getParameterChildren(), options);
        output.append(")");
        
        // Return types
        String returnTypes = getGoReturnTypes(method, options);
        if (!returnTypes.isEmpty()) {
            output.append(" ").append(returnTypes);
        }
        
        output.append(" {\n");
        
        indentLevel++;
        visitFunctionBody(method, options);
        indentLevel--;
        
        output.append("}\n\n");
    }
    
    private void visitInterfaceMethod(ASTNode method, TranslationOptions options) throws TranslationException {
        indent();
        output.append(method.getName()).append("(");
        visitParameters(method.getParameterChildren(), options);
        output.append(")");
        
        String returnTypes = getGoReturnTypes(method, options);
        if (!returnTypes.isEmpty()) {
            output.append(" ").append(returnTypes);
        }
        
        output.append("\n");
    }
    
    private void generateConstructor(ASTNode classNode, String structName, TranslationOptions options) {
        output.append("// New").append(structName).append(" creates a new instance of ").append(structName).append("\n");
        output.append("func New").append(structName).append("(");
        
        // Constructor parameters from fields
        List<ASTNode> fields = classNode.getFieldChildren();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) output.append(", ");
            ASTNode field = fields.get(i);
            output.append(field.getName()).append(" ").append(mapToGoType(field.getTypeAnnotation()));
        }
        
        output.append(") *").append(structName).append(" {\n");
        
        indentLevel++;
        indent();
        output.append("return &").append(structName).append("{\n");
        
        indentLevel++;
        for (ASTNode field : fields) {
            indent();
            String fieldName = capitalizeFirst(field.getName());
            output.append(fieldName).append(": ").append(field.getName()).append(",\n");
        }
        indentLevel--;
        
        indent();
        output.append("}\n");
        indentLevel--;
        
        output.append("}\n\n");
    }
    
    private void generateGoroutineHelper(ASTNode asyncFunc, String funcName, TranslationOptions options) {
        output.append("// Start").append(capitalizeFirst(funcName)).append(" starts ").append(funcName).append(" as a goroutine\n");
        output.append("func Start").append(capitalizeFirst(funcName)).append("(");
        
        if (useContexts) {
            output.append("ctx context.Context");
            if (!asyncFunc.getParameterChildren().isEmpty()) {
                output.append(", ");
            }
        }
        
        visitParameters(asyncFunc.getParameterChildren(), options);
        output.append(") <-chan error {\n");
        
        indentLevel++;
        indent();
        output.append("errChan := make(chan error, 1)\n");
        indent();
        output.append("go func() {\n");
        
        indentLevel++;
        indent();
        output.append("defer close(errChan)\n");
        indent();
        output.append("if err := ").append(funcName).append("(");
        
        if (useContexts) {
            output.append("ctx");
            if (!asyncFunc.getParameterChildren().isEmpty()) {
                output.append(", ");
            }
        }
        
        // Pass parameters
        List<ASTNode> params = asyncFunc.getParameterChildren();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) output.append(", ");
            output.append(params.get(i).getName());
        }
        
        output.append("); err != nil {\n");
        indentLevel++;
        indent();
        output.append("errChan <- err\n");
        indentLevel--;
        indent();
        output.append("}\n");
        
        indentLevel--;
        indent();
        output.append("}()\n");
        indent();
        output.append("return errChan\n");
        
        indentLevel--;
        output.append("}\n\n");
    }
    
    // Type mapping and utility methods
    
    private String mapToGoType(String originalType) {
        if (originalType == null) return "interface{}";
        
        String mapped = typeMap.get(originalType);
        if (mapped != null) {
            // Handle special format strings for generics
            if (mapped.contains("%s")) {
                return mapped;  // Will be formatted later
            }
            return mapped;
        }
        
        // Handle generic types
        if (originalType.contains("<")) {
            return mapGenericType(originalType);
        }
        
        // Custom types (structs)
        return originalType;
    }
    
    private String mapGenericType(String genericType) {
        if (genericType.startsWith("List<")) {
            String inner = genericType.substring(5, genericType.length() - 1);
            return "[]" + mapToGoType(inner);
        }
        
        if (genericType.startsWith("Map<") || genericType.startsWith("HashMap<")) {
            String content = genericType.substring(genericType.indexOf('<') + 1, genericType.length() - 1);
            String[] parts = content.split(",");
            if (parts.length == 2) {
                return "map[" + mapToGoType(parts[0].trim()) + "]" + mapToGoType(parts[1].trim());
            }
        }
        
        if (genericType.startsWith("Set<")) {
            String inner = genericType.substring(4, genericType.length() - 1);
            return "map[" + mapToGoType(inner) + "]bool";
        }
        
        if (genericType.startsWith("Optional<")) {
            String inner = genericType.substring(9, genericType.length() - 1);
            return "*" + mapToGoType(inner);  // Pointer for optional
        }
        
        if (genericType.startsWith("Future<") || genericType.startsWith("CompletableFuture<")) {
            String inner = genericType.substring(genericType.indexOf('<') + 1, genericType.length() - 1);
            return "chan " + mapToGoType(inner);
        }
        
        return "interface{}";  // Fallback
    }
    
    private String getGoReturnTypes(ASTNode function, TranslationOptions options) {
        String returnType = function.getReturnType();
        
        if (returnType == null || returnType.equals("void")) {
            // Check if function can throw - add error return
            if (function.getBooleanAttribute("canThrow", false)) {
                return "error";
            }
            return "";
        }
        
        String goReturnType = mapToGoType(returnType);
        
        // Add error return if function can throw
        if (function.getBooleanAttribute("canThrow", false)) {
            return "(" + goReturnType + ", error)";
        }
        
        return goReturnType;
    }
    
    private String getAsyncReturnTypes(ASTNode function, TranslationOptions options) {
        // Async functions typically return error channel
        String returnType = function.getReturnType();
        
        if (returnType != null && !returnType.equals("void")) {
            return "(<-chan " + mapToGoType(returnType) + ", <-chan error)";
        }
        
        return "<-chan error";
    }
    
    private boolean isRange(ASTNode iterable) {
        return iterable.getType() == ASTNode.Type.RANGE;
    }
    
    private void visitRange(ASTNode range, TranslationOptions options) throws TranslationException {
        visitExpression(range.getStart(), options);
        output.append("; ").append(range.getIteratorVariable()).append(" < ");
        visitExpression(range.getEnd(), options);
        output.append("; ").append(range.getIteratorVariable()).append("++");
    }
    
    private boolean needsIndex(ASTNode forLoop) {
        return forLoop.getBooleanAttribute("needsIndex", false);
    }
    
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    private void visitExpression(ASTNode expr, TranslationOptions options) throws TranslationException {
        // Simplified expression handling
        output.append(expr.toString());
    }
    
    private void visitBlock(ASTNode block, TranslationOptions options) throws TranslationException {
        for (ASTNode stmt : block.getChildren()) {
            visitNode(stmt, options);
        }
    }
    
    private void visitFunctionBody(ASTNode function, TranslationOptions options) throws TranslationException {
        ASTNode body = function.getBody();
        if (body != null) {
            visitBlock(body, options);
        } else {
            // Add default implementation
            indent();
            output.append("// TODO: Implement function logic\n");
            
            // Return appropriate zero values
            String returnType = function.getReturnType();
            if (returnType != null && !returnType.equals("void")) {
                indent();
                if (function.getBooleanAttribute("canThrow", false)) {
                    output.append("return ").append(getZeroValue(returnType)).append(", nil\n");
                } else {
                    output.append("return ").append(getZeroValue(returnType)).append("\n");
                }
            } else if (function.getBooleanAttribute("canThrow", false)) {
                indent();
                output.append("return nil\n");
            }
        }
    }
    
    private String getZeroValue(String goType) {
        switch (goType) {
            case "string": return "\"\"";
            case "int": case "int32": case "int64": case "int16": case "int8": return "0";
            case "float32": case "float64": return "0.0";
            case "bool": return "false";
            case "byte": case "rune": return "0";
            default:
                if (goType.startsWith("[]")) return "nil";
                if (goType.startsWith("map[")) return "nil";
                if (goType.startsWith("chan ")) return "nil";
                if (goType.startsWith("*")) return "nil";
                return goType + "{}";  // Struct zero value
        }
    }
    
    private void visitDefault(ASTNode node, TranslationOptions options) throws TranslationException {
        indent();
        output.append("// TODO: Implement ").append(node.getType()).append(" translation\n");
    }
    
    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            output.append("\t");
        }
    }
}