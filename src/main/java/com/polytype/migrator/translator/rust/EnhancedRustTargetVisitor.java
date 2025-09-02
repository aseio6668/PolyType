package com.polytype.migrator.translator.rust;

import com.polytype.migrator.core.*;
import com.polytype.migrator.translator.EnhancedMultiLanguageTranslator.SemanticAwareVisitor;
import com.polytype.migrator.translator.EnhancedMultiLanguageTranslator.SemanticContext;
import java.util.*;

/**
 * Enhanced Rust target visitor with advanced memory safety and performance optimizations.
 * 
 * This visitor specializes in translating code to idiomatic, safe, and high-performance Rust:
 * - Zero-cost abstractions and compile-time optimizations
 * - Ownership system with borrowing and lifetimes
 * - Result types for error handling instead of exceptions
 * - Traits and pattern matching for polymorphism
 * - Async/await for concurrency
 * - RAII and automatic memory management
 * - Cargo ecosystem integration
 * 
 * Key Translation Features:
 * - Exception → Result<T, E> transformation
 * - GC references → Ownership/borrowing
 * - Dynamic typing → Strong static typing with enums
 * - Inheritance → Composition with traits
 * - Null references → Option<T>
 * - Concurrency → async/await + Send/Sync bounds
 */
public class EnhancedRustTargetVisitor implements TargetVisitor, SemanticAwareVisitor {
    
    private final StringBuilder output;
    private int indentLevel;
    private SemanticContext semanticContext;
    
    // Rust-specific state
    private final Set<String> imports = new LinkedHashSet<>();
    private final Map<String, String> typeMap = new HashMap<>();
    private final Set<String> lifetimeParams = new HashSet<>();
    private boolean inAsyncContext = false;
    private boolean needsErrorHandling = false;
    
    public EnhancedRustTargetVisitor() {
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
        // Common type mappings to Rust
        typeMap.put("String", "String");
        typeMap.put("int", "i32");
        typeMap.put("long", "i64");
        typeMap.put("float", "f32");
        typeMap.put("double", "f64");
        typeMap.put("boolean", "bool");
        typeMap.put("List", "Vec");
        typeMap.put("Map", "HashMap");
        typeMap.put("Set", "HashSet");
        typeMap.put("Optional", "Option");
        typeMap.put("CompletableFuture", "Future");
        
        // Language-specific mappings
        typeMap.put("Array", "Vec");
        typeMap.put("ArrayList", "Vec");
        typeMap.put("HashMap", "HashMap");
        typeMap.put("TreeMap", "BTreeMap");
        typeMap.put("LinkedList", "VecDeque");
        typeMap.put("StringBuilder", "String");
        typeMap.put("StringBuffer", "String");
        typeMap.put("Exception", "Box<dyn std::error::Error>");
    }
    
    private void initializeStandardImports() {
        imports.add("use std::collections::{HashMap, HashSet};");
        imports.add("use std::error::Error;");
        imports.add("use std::result::Result;");
    }
    
    private void analyzeSemanticRequirements() {
        if (semanticContext == null) return;
        
        Set<String> patterns = semanticContext.getDetectedPatterns();
        
        if (patterns.contains("async_pattern")) {
            imports.add("use tokio;");
            imports.add("use std::future::Future;");
            inAsyncContext = true;
        }
        
        if (patterns.contains("error_handling")) {
            imports.add("use thiserror::Error;");
            imports.add("use anyhow::Result;");
            needsErrorHandling = true;
        }
        
        if (patterns.contains("singleton_pattern")) {
            imports.add("use std::sync::{Arc, Mutex, Once};");
            imports.add("use lazy_static::lazy_static;");
        }
        
        if (patterns.contains("observer_pattern")) {
            imports.add("use std::sync::mpsc;");
            imports.add("use std::thread;");
        }
    }
    
    @Override
    public String visit(ASTNode node, TranslationOptions options) throws TranslationException {
        output.setLength(0);
        indentLevel = 0;
        
        // Generate file header
        generateFileHeader();
        
        // Process the AST
        visitNode(node, options);
        
        return output.toString();
    }
    
    private void generateFileHeader() {
        // Add standard imports
        for (String import_ : imports) {
            output.append(import_).append("\n");
        }
        
        if (!imports.isEmpty()) {
            output.append("\n");
        }
        
        // Add common Rust attributes for optimization
        output.append("#![warn(clippy::all)]\n");
        output.append("#![warn(unused_imports)]\n");
        output.append("#![allow(dead_code)]\n\n");
    }
    
    private void visitNode(ASTNode node, TranslationOptions options) throws TranslationException {
        switch (node.getType()) {
            case PROGRAM:
                visitProgram(node, options);
                break;
            case CLASS_DECLARATION:
                visitClassDeclaration(node, options);
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
                visitWhileLoop(node, options);
                break;
            case FOR_LOOP:
                visitForLoop(node, options);
                break;
            case TRY_CATCH:
                visitTryCatch(node, options);
                break;
            case ASYNC_FUNCTION:
                visitAsyncFunction(node, options);
                break;
            default:
                visitDefault(node, options);
        }
    }
    
    private void visitProgram(ASTNode node, TranslationOptions options) throws TranslationException {
        for (ASTNode child : node.getChildren()) {
            visitNode(child, options);
            if (child.getType() != ASTNode.Type.IMPORT) {
                output.append("\n");
            }
        }
    }
    
    private void visitClassDeclaration(ASTNode node, TranslationOptions options) throws TranslationException {
        String className = node.getName();
        
        // Convert class to struct with impl blocks in Rust
        indent();
        output.append("pub struct ").append(className);
        
        // Add generic parameters if needed
        addGenericParameters(node);
        
        output.append(" {\n");
        indentLevel++;
        
        // Add fields
        for (ASTNode field : node.getFieldChildren()) {
            visitStructField(field, options);
        }
        
        indentLevel--;
        indent();
        output.append("}\n\n");
        
        // Add implementation block
        indent();
        output.append("impl");
        addGenericParameters(node);
        output.append(" ").append(className);
        addGenericParameters(node);
        output.append(" {\n");
        indentLevel++;
        
        // Add constructor (new function)
        generateConstructor(node, className, options);
        
        // Add methods
        for (ASTNode method : node.getMethodChildren()) {
            visitMethod(method, options);
        }
        
        indentLevel--;
        indent();
        output.append("}\n");
    }
    
    private void visitFunctionDeclaration(ASTNode node, TranslationOptions options) throws TranslationException {
        indent();
        
        if (inAsyncContext && isAsyncFunction(node)) {
            output.append("pub async fn ");
        } else {
            output.append("pub fn ");
        }
        
        output.append(node.getName()).append("(");
        
        // Parameters
        List<ASTNode> params = node.getParameterChildren();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) output.append(", ");
            visitParameter(params.get(i), options);
        }
        
        output.append(")");
        
        // Return type
        String returnType = getRustReturnType(node, options);
        if (!returnType.equals("()")) {
            output.append(" -> ").append(returnType);
        }
        
        output.append(" {\n");
        indentLevel++;
        
        // Function body
        visitFunctionBody(node, options);
        
        indentLevel--;
        indent();
        output.append("}\n");
    }
    
    private void visitVariableDeclaration(ASTNode node, TranslationOptions options) throws TranslationException {
        indent();
        
        // Mutability
        boolean isMutable = node.getBooleanAttribute("mutable", false);
        output.append("let ");
        if (isMutable) {
            output.append("mut ");
        }
        
        output.append(node.getName());
        
        // Type annotation (optional in Rust due to inference)
        String rustType = mapToRustType(node.getTypeAnnotation());
        if (rustType != null && !rustType.equals("_")) {
            output.append(": ").append(rustType);
        }
        
        // Initialization
        ASTNode initializer = node.getInitializer();
        if (initializer != null) {
            output.append(" = ");
            visitExpression(initializer, options);
        }
        
        output.append(";\n");
    }
    
    private void visitIfStatement(ASTNode node, TranslationOptions options) throws TranslationException {
        indent();
        output.append("if ");
        visitExpression(node.getCondition(), options);
        output.append(" {\n");
        
        indentLevel++;
        visitBlock(node.getThenBlock(), options);
        indentLevel--;
        
        indent();
        
        ASTNode elseBlock = node.getElseBlock();
        if (elseBlock != null) {
            output.append("} else {\n");
            indentLevel++;
            visitBlock(elseBlock, options);
            indentLevel--;
            indent();
        }
        
        output.append("}\n");
    }
    
    private void visitWhileLoop(ASTNode node, TranslationOptions options) throws TranslationException {
        indent();
        output.append("while ");
        visitExpression(node.getCondition(), options);
        output.append(" {\n");
        
        indentLevel++;
        visitBlock(node.getBody(), options);
        indentLevel--;
        
        indent();
        output.append("}\n");
    }
    
    private void visitForLoop(ASTNode node, TranslationOptions options) throws TranslationException {
        indent();
        
        // Convert to idiomatic Rust iterator syntax
        String iterVar = node.getIteratorVariable();
        ASTNode iterable = node.getIterable();
        
        output.append("for ").append(iterVar).append(" in ");
        
        // Handle different iterable types
        if (isRange(iterable)) {
            visitRange(iterable, options);
        } else {
            visitExpression(iterable, options);
            // Add .iter() if needed for borrowing
            if (needsIterator(iterable)) {
                output.append(".iter()");
            }
        }
        
        output.append(" {\n");
        
        indentLevel++;
        visitBlock(node.getBody(), options);
        indentLevel--;
        
        indent();
        output.append("}\n");
    }
    
    private void visitTryCatch(ASTNode node, TranslationOptions options) throws TranslationException {
        // Convert try-catch to Result-based error handling
        indent();
        output.append("match (|| -> Result<(), Box<dyn Error>> {\n");
        
        indentLevel++;
        visitBlock(node.getTryBlock(), options);
        indent();
        output.append("Ok(())\n");
        indentLevel--;
        
        indent();
        output.append("})() {\n");
        
        indentLevel++;
        indent();
        output.append("Ok(_) => {}\n");
        
        // Handle catch blocks
        for (ASTNode catchBlock : node.getCatchBlocks()) {
            indent();
            output.append("Err(e) => {\n");
            indentLevel++;
            visitBlock(catchBlock, options);
            indentLevel--;
            indent();
            output.append("}\n");
        }
        
        indentLevel--;
        indent();
        output.append("}\n");
    }
    
    private void visitAsyncFunction(ASTNode node, TranslationOptions options) throws TranslationException {
        boolean wasAsync = inAsyncContext;
        inAsyncContext = true;
        
        visitFunctionDeclaration(node, options);
        
        inAsyncContext = wasAsync;
    }
    
    // Helper methods
    
    private void visitStructField(ASTNode field, TranslationOptions options) throws TranslationException {
        indent();
        
        // Visibility
        if (field.getBooleanAttribute("public", false)) {
            output.append("pub ");
        }
        
        output.append(field.getName()).append(": ");
        output.append(mapToRustType(field.getTypeAnnotation()));
        output.append(",\n");
    }
    
    private void visitMethod(ASTNode method, TranslationOptions options) throws TranslationException {
        indent();
        
        if (inAsyncContext && isAsyncFunction(method)) {
            output.append("pub async fn ");
        } else {
            output.append("pub fn ");
        }
        
        output.append(method.getName()).append("(");
        
        // Self parameter
        boolean isStatic = method.getBooleanAttribute("static", false);
        if (!isStatic) {
            if (method.getBooleanAttribute("mutable", false)) {
                output.append("&mut self");
            } else {
                output.append("&self");
            }
            
            List<ASTNode> params = method.getParameterChildren();
            if (!params.isEmpty()) {
                output.append(", ");
            }
        }
        
        // Other parameters
        visitParameters(method.getParameterChildren(), options);
        
        output.append(")");
        
        // Return type
        String returnType = getRustReturnType(method, options);
        if (!returnType.equals("()")) {
            output.append(" -> ").append(returnType);
        }
        
        output.append(" {\n");
        
        indentLevel++;
        visitFunctionBody(method, options);
        indentLevel--;
        
        indent();
        output.append("}\n\n");
    }
    
    private void visitParameters(List<ASTNode> params, TranslationOptions options) throws TranslationException {
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) output.append(", ");
            visitParameter(params.get(i), options);
        }
    }
    
    private void visitParameter(ASTNode param, TranslationOptions options) throws TranslationException {
        output.append(param.getName()).append(": ");
        
        String rustType = mapToRustType(param.getTypeAnnotation());
        
        // Add borrowing for complex types
        if (needsBorrowing(rustType)) {
            output.append("&");
            if (param.getBooleanAttribute("mutable", false)) {
                output.append("mut ");
            }
        }
        
        output.append(rustType);
    }
    
    private void visitFunctionBody(ASTNode function, TranslationOptions options) throws TranslationException {
        ASTNode body = function.getBody();
        if (body != null) {
            visitBlock(body, options);
        }
        
        // Add default return if needed
        String returnType = getRustReturnType(function, options);
        if (!returnType.equals("()") && !hasExplicitReturn(body)) {
            indent();
            output.append("// TODO: Implement function logic\n");
            indent();
            output.append("unimplemented!()\n");
        }
    }
    
    private void visitBlock(ASTNode block, TranslationOptions options) throws TranslationException {
        for (ASTNode stmt : block.getChildren()) {
            visitNode(stmt, options);
        }
    }
    
    private void visitExpression(ASTNode expr, TranslationOptions options) throws TranslationException {
        // Handle different expression types with Rust-specific adaptations
        switch (expr.getType()) {
            case BINARY_OPERATION:
                visitBinaryOperation(expr, options);
                break;
            case METHOD_CALL:
                visitMethodCall(expr, options);
                break;
            case LITERAL:
                visitLiteral(expr, options);
                break;
            default:
                output.append(expr.toString());
        }
    }
    
    private void visitBinaryOperation(ASTNode expr, TranslationOptions options) throws TranslationException {
        visitExpression(expr.getLeft(), options);
        output.append(" ").append(expr.getOperator()).append(" ");
        visitExpression(expr.getRight(), options);
    }
    
    private void visitMethodCall(ASTNode call, TranslationOptions options) throws TranslationException {
        if (call.getReceiver() != null) {
            visitExpression(call.getReceiver(), options);
            output.append(".");
        }
        
        output.append(call.getMethodName()).append("(");
        
        List<ASTNode> args = call.getArguments();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) output.append(", ");
            visitExpression(args.get(i), options);
        }
        
        output.append(")");
    }
    
    private void visitLiteral(ASTNode literal, TranslationOptions options) {
        String value = literal.getValue();
        String type = literal.getDataType();
        
        switch (type) {
            case "string":
                output.append("\"").append(escapeString(value)).append("\"");
                break;
            case "char":
                output.append("'").append(value).append("'");
                break;
            case "null":
                output.append("None");
                break;
            default:
                output.append(value);
        }
    }
    
    private void generateConstructor(ASTNode classNode, String className, TranslationOptions options) {
        indent();
        output.append("pub fn new(");
        
        // Constructor parameters based on fields
        List<ASTNode> fields = classNode.getFieldChildren();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) output.append(", ");
            ASTNode field = fields.get(i);
            output.append(field.getName()).append(": ");
            output.append(mapToRustType(field.getTypeAnnotation()));
        }
        
        output.append(") -> Self {\n");
        
        indentLevel++;
        indent();
        output.append("Self {\n");
        
        indentLevel++;
        for (ASTNode field : fields) {
            indent();
            output.append(field.getName()).append(",\n");
        }
        indentLevel--;
        
        indent();
        output.append("}\n");
        indentLevel--;
        
        indent();
        output.append("}\n\n");
    }
    
    // Type mapping and utility methods
    
    private String mapToRustType(String originalType) {
        if (originalType == null) return "()";
        
        String mapped = typeMap.get(originalType);
        if (mapped != null) return mapped;
        
        // Handle generic types
        if (originalType.contains("<")) {
            return mapGenericType(originalType);
        }
        
        // Default to the original type (assume it's a custom type)
        return originalType;
    }
    
    private String mapGenericType(String genericType) {
        // Handle common generic patterns
        if (genericType.startsWith("List<")) {
            String inner = genericType.substring(5, genericType.length() - 1);
            return "Vec<" + mapToRustType(inner) + ">";
        }
        
        if (genericType.startsWith("Map<")) {
            // Extract key and value types
            String content = genericType.substring(4, genericType.length() - 1);
            String[] parts = content.split(",");
            if (parts.length == 2) {
                return "HashMap<" + mapToRustType(parts[0].trim()) + ", " + 
                       mapToRustType(parts[1].trim()) + ">";
            }
        }
        
        if (genericType.startsWith("Optional<")) {
            String inner = genericType.substring(9, genericType.length() - 1);
            return "Option<" + mapToRustType(inner) + ">";
        }
        
        return genericType; // Fallback
    }
    
    private String getRustReturnType(ASTNode function, TranslationOptions options) {
        String returnType = function.getReturnType();
        
        if (returnType == null || returnType.equals("void")) {
            return "()";
        }
        
        String rustType = mapToRustType(returnType);
        
        // Wrap in Result if function can throw exceptions
        if (needsErrorHandling && function.getBooleanAttribute("canThrow", false)) {
            return "Result<" + rustType + ", Box<dyn Error>>";
        }
        
        return rustType;
    }
    
    private boolean needsBorrowing(String rustType) {
        // Types that should be borrowed by default
        return rustType.equals("String") || rustType.startsWith("Vec<") || 
               rustType.startsWith("HashMap<") || rustType.startsWith("HashSet<");
    }
    
    private boolean isAsyncFunction(ASTNode function) {
        return function.getBooleanAttribute("async", false) || 
               function.getReturnType() != null && 
               function.getReturnType().contains("Future");
    }
    
    private boolean isRange(ASTNode iterable) {
        return iterable.getType() == ASTNode.Type.RANGE;
    }
    
    private void visitRange(ASTNode range, TranslationOptions options) throws TranslationException {
        visitExpression(range.getStart(), options);
        output.append("..");
        if (range.getBooleanAttribute("inclusive", false)) {
            output.append("=");
        }
        visitExpression(range.getEnd(), options);
    }
    
    private boolean needsIterator(ASTNode iterable) {
        // Determine if we need to call .iter() on the iterable
        return !iterable.getDataType().equals("range");
    }
    
    private boolean hasExplicitReturn(ASTNode body) {
        // Check if function body has explicit return statement
        return body != null && hasReturnStatement(body);
    }
    
    private boolean hasReturnStatement(ASTNode node) {
        if (node.getType() == ASTNode.Type.RETURN_STATEMENT) {
            return true;
        }
        
        for (ASTNode child : node.getChildren()) {
            if (hasReturnStatement(child)) {
                return true;
            }
        }
        
        return false;
    }
    
    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\t", "\\t");
    }
    
    private void addGenericParameters(ASTNode classNode) {
        List<String> generics = classNode.getGenericParameters();
        if (!generics.isEmpty()) {
            output.append("<");
            for (int i = 0; i < generics.size(); i++) {
                if (i > 0) output.append(", ");
                output.append(generics.get(i));
            }
            output.append(">");
        }
    }
    
    private void visitDefault(ASTNode node, TranslationOptions options) throws TranslationException {
        // Default handling for unimplemented node types
        indent();
        output.append("// TODO: Implement ").append(node.getType()).append(" translation\n");
    }
    
    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            output.append("    ");
        }
    }
}