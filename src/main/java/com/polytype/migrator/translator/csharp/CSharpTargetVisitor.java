package com.polytype.migrator.translator.csharp;

import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.translator.base.TargetVisitor;

public class CSharpTargetVisitor implements TargetVisitor {
    private StringBuilder output;
    private int indentLevel;
    private boolean inClass;
    private String namespaceName;
    
    public CSharpTargetVisitor() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
        this.inClass = false;
        this.namespaceName = "Generated";
    }
    
    public CSharpTargetVisitor(String namespaceName) {
        this();
        this.namespaceName = namespaceName != null ? namespaceName : "Generated";
    }
    
    @Override
    public String getResult() {
        return output.toString();
    }
    
    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            output.append("    ");
        }
    }
    
    private void newLine() {
        output.append("\n");
    }
    
    private String mapToCSharpType(String originalType) {
        if (originalType == null || originalType.isEmpty()) {
            return "object";
        }
        
        switch (originalType) {
            case "void": return "void";
            case "boolean": case "Boolean": return "bool";
            case "byte": return "byte";
            case "short": return "short";
            case "int": case "Integer": return "int";
            case "long": case "Long": return "long";
            case "float": case "Float": return "float";
            case "double": case "Double": return "double";
            case "char": case "Character": return "char";
            case "String": return "string";
            case "Object": return "object";
            case "List": case "ArrayList": return "List<object>";
            case "Map": case "HashMap": return "Dictionary<string, object>";
            case "Set": case "HashSet": return "HashSet<object>";
            default:
                if (originalType.startsWith("List<") || originalType.startsWith("ArrayList<")) {
                    String genericType = extractGenericType(originalType);
                    return "List<" + mapToCSharpType(genericType) + ">";
                }
                if (originalType.startsWith("Map<") || originalType.startsWith("HashMap<")) {
                    String[] types = extractGenericTypes(originalType);
                    if (types.length >= 2) {
                        return "Dictionary<" + mapToCSharpType(types[0]) + ", " + mapToCSharpType(types[1]) + ">";
                    }
                    return "Dictionary<string, object>";
                }
                if (originalType.startsWith("Set<") || originalType.startsWith("HashSet<")) {
                    String genericType = extractGenericType(originalType);
                    return "HashSet<" + mapToCSharpType(genericType) + ">";
                }
                return originalType;
        }
    }
    
    private String extractGenericType(String type) {
        int start = type.indexOf('<');
        int end = type.lastIndexOf('>');
        if (start > 0 && end > start) {
            return type.substring(start + 1, end).trim();
        }
        return "object";
    }
    
    private String[] extractGenericTypes(String type) {
        String genericPart = extractGenericType(type);
        return genericPart.split(",\\s*");
    }
    
    private String formatPropertyName(String name) {
        if (name.startsWith("m_") || name.startsWith("_")) {
            name = name.substring(name.startsWith("m_") ? 2 : 1);
        }
        return capitalizeFirst(name);
    }
    
    private String formatFieldName(String name) {
        if (name.startsWith("m_") || name.startsWith("_")) {
            name = name.substring(name.startsWith("m_") ? 2 : 1);
        }
        return "_" + decapitalizeFirst(name);
    }
    
    private String formatParameterName(String name) {
        if (name.startsWith("m_") || name.startsWith("_")) {
            name = name.substring(name.startsWith("m_") ? 2 : 1);
        }
        return decapitalizeFirst(name);
    }
    
    private String capitalizeFirst(String name) {
        if (name.length() <= 1) return name.toUpperCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    
    private String decapitalizeFirst(String name) {
        if (name.length() <= 1) return name.toLowerCase();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    
    @Override
    public void visit(ProgramNode node) {
        output.append("// Generated C# code from PolyType migrator\n");
        output.append("using System;\n");
        output.append("using System.Collections.Generic;\n");
        output.append("using System.Threading.Tasks;\n");
        output.append("using System.Linq;\n\n");
        
        output.append("namespace ").append(namespaceName).append("\n{\n");
        indentLevel++;
        
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
            newLine();
        }
        
        indentLevel--;
        output.append("}\n");
    }
    
    @Override
    public void visit(ClassDeclarationNode node) {
        boolean wasInClass = inClass;
        inClass = true;
        
        if (node.isDataClass()) {
            generateRecord(node);
        } else {
            generateClass(node);
        }
        
        inClass = wasInClass;
    }
    
    private void generateRecord(ClassDeclarationNode node) {
        indent();
        output.append("public record ").append(node.getName()).append("(");
        
        boolean first = true;
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (!first) output.append(", ");
                output.append(mapToCSharpType(var.getType()))
                      .append(" ")
                      .append(formatPropertyName(var.getName()));
                first = false;
            }
        }
        
        output.append(");");
        newLine();
    }
    
    private void generateClass(ClassDeclarationNode node) {
        indent();
        output.append("public class ").append(node.getName());
        newLine();
        indent();
        output.append("{");
        newLine();
        indentLevel++;
        
        // Generate fields
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                indent();
                output.append("private ")
                      .append(mapToCSharpType(var.getType()))
                      .append(" ")
                      .append(formatFieldName(var.getName()));
                
                if (var.getDefaultValue() != null) {
                    output.append(" = ").append(formatValue(var.getDefaultValue(), var.getType()));
                }
                
                output.append(";");
                newLine();
            }
        }
        
        newLine();
        
        // Generate constructor
        generateConstructor(node);
        
        // Generate properties
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                generateProperty(var);
            }
        }
        
        // Generate methods
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                if (!func.isConstructor() && !func.getName().equals(node.getName())) {
                    func.accept(this);
                }
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private void generateConstructor(ClassDeclarationNode node) {
        indent();
        output.append("public ").append(node.getName()).append("(");
        
        boolean first = true;
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (!first) output.append(", ");
                output.append(mapToCSharpType(var.getType()))
                      .append(" ")
                      .append(formatParameterName(var.getName()));
                first = false;
            }
        }
        
        output.append(")");
        newLine();
        indent();
        output.append("{");
        newLine();
        indentLevel++;
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                indent();
                output.append(formatFieldName(var.getName()))
                      .append(" = ")
                      .append(formatParameterName(var.getName()))
                      .append(";");
                newLine();
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
        newLine();
    }
    
    private void generateProperty(VariableDeclarationNode var) {
        indent();
        output.append("public ")
              .append(mapToCSharpType(var.getType()))
              .append(" ")
              .append(formatPropertyName(var.getName()));
        newLine();
        indent();
        output.append("{");
        newLine();
        indentLevel++;
        
        // Getter
        indent();
        output.append("get { return ").append(formatFieldName(var.getName())).append("; }");
        newLine();
        
        // Setter (if not final)
        if (!var.isFinal()) {
            indent();
            output.append("set { ").append(formatFieldName(var.getName())).append(" = value; }");
            newLine();
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
        newLine();
    }
    
    @Override
    public void visit(FunctionDeclarationNode node) {
        if (node.isConstructor() || node.getName().equals(node.getDeclaringClass())) {
            return; // Constructors handled separately
        }
        
        indent();
        
        if (inClass) {
            output.append("public ");
        }
        
        if (node.isStatic()) {
            output.append("static ");
        }
        
        if (node.isAsync()) {
            String returnType = mapToCSharpType(node.getReturnType());
            if (returnType.equals("void")) {
                output.append("async Task ");
            } else {
                output.append("async Task<").append(returnType).append("> ");
            }
        } else {
            output.append(mapToCSharpType(node.getReturnType())).append(" ");
        }
        
        output.append(capitalizeFirst(node.getName())).append("(");
        
        boolean first = true;
        for (ParameterNode param : node.getParameters()) {
            if (!first) output.append(", ");
            output.append(mapToCSharpType(param.getType()))
                  .append(" ")
                  .append(formatParameterName(param.getName()));
            first = false;
        }
        
        output.append(")");
        newLine();
        indent();
        output.append("{");
        newLine();
        indentLevel++;
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        } else {
            indent();
            if (node.isAsync()) {
                if (!node.getReturnType().equals("void")) {
                    output.append("return await Task.FromResult(")
                          .append(getDefaultValue(node.getReturnType()))
                          .append(");");
                } else {
                    output.append("await Task.CompletedTask;");
                }
            } else {
                if (!node.getReturnType().equals("void")) {
                    output.append("return ").append(getDefaultValue(node.getReturnType())).append(";");
                } else {
                    output.append("// TODO: Implement method body");
                }
            }
            newLine();
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
        newLine();
    }
    
    private String getDefaultValue(String type) {
        String csharpType = mapToCSharpType(type);
        switch (csharpType) {
            case "bool": return "false";
            case "byte": case "short": case "int": case "long": 
            case "float": case "double": return "0";
            case "char": return "'\\0'";
            case "string": return "string.Empty";
            case "void": return "";
            default:
                if (csharpType.startsWith("List<")) return "new " + csharpType + "()";
                if (csharpType.startsWith("Dictionary<")) return "new " + csharpType + "()";
                if (csharpType.startsWith("HashSet<")) return "new " + csharpType + "()";
                return "null";
        }
    }
    
    private String formatValue(Object value, String type) {
        if (value == null) return "null";
        String csharpType = mapToCSharpType(type);
        
        if (csharpType.equals("string")) {
            return "\"" + value.toString().replace("\"", "\\\"") + "\"";
        }
        if (csharpType.equals("char")) {
            return "'" + value.toString().replace("'", "\\'") + "'";
        }
        if (csharpType.equals("bool")) {
            return value.toString().toLowerCase();
        }
        if (csharpType.equals("float")) {
            return value.toString() + "f";
        }
        
        return value.toString();
    }
    
    @Override
    public void visit(VariableDeclarationNode node) {
        if (!inClass) {
            indent();
            output.append(mapToCSharpType(node.getType()))
                  .append(" ")
                  .append(formatParameterName(node.getName()));
            
            if (node.getDefaultValue() != null) {
                output.append(" = ").append(formatValue(node.getDefaultValue(), node.getType()));
            }
            
            output.append(";");
            newLine();
        }
    }
    
    @Override
    public void visit(ParameterNode node) {
        // Parameters are handled in function declarations
    }
    
    @Override
    public void visit(BlockNode node) {
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
        }
    }
    
    @Override
    public void visit(ReturnStatementNode node) {
        indent();
        output.append("return");
        if (node.getExpression() != null) {
            output.append(" ");
            node.getExpression().accept(this);
        }
        output.append(";");
        newLine();
    }
    
    @Override
    public void visit(ExpressionNode node) {
        if (node.getValue() != null) {
            output.append(node.getValue().toString());
        }
    }
    
    @Override
    public void visit(IfStatementNode node) {
        indent();
        output.append("if (");
        if (node.getCondition() != null) {
            node.getCondition().accept(this);
        }
        output.append(")");
        newLine();
        indent();
        output.append("{");
        newLine();
        indentLevel++;
        
        if (node.getThenBlock() != null) {
            node.getThenBlock().accept(this);
        }
        
        indentLevel--;
        indent();
        output.append("}");
        
        if (node.getElseBlock() != null) {
            newLine();
            indent();
            output.append("else");
            newLine();
            indent();
            output.append("{");
            newLine();
            indentLevel++;
            node.getElseBlock().accept(this);
            indentLevel--;
            indent();
            output.append("}");
        }
        
        newLine();
    }
    
    @Override
    public void visit(ForLoopNode node) {
        indent();
        output.append("for (");
        if (node.getInitialization() != null) {
            node.getInitialization().accept(this);
        }
        output.append("; ");
        if (node.getCondition() != null) {
            node.getCondition().accept(this);
        }
        output.append("; ");
        if (node.getIncrement() != null) {
            node.getIncrement().accept(this);
        }
        output.append(")");
        newLine();
        indent();
        output.append("{");
        newLine();
        indentLevel++;
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    @Override
    public void visit(WhileLoopNode node) {
        indent();
        output.append("while (");
        if (node.getCondition() != null) {
            node.getCondition().accept(this);
        }
        output.append(")");
        newLine();
        indent();
        output.append("{");
        newLine();
        indentLevel++;
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
}