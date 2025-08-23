package com.davajava.migrator.core.ast;

public class TypeAnnotationNode extends ASTNode {
    private final String typeName;

    public TypeAnnotationNode(String typeName, int lineNumber, int columnNumber) {
        super(NodeType.TYPE_ANNOTATION, lineNumber, columnNumber);
        this.typeName = typeName;
    }

    public String getTypeName() { return typeName; }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitTypeAnnotation(this);
    }
}