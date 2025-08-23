package com.davajava.migrator.core.ast;

public class FieldAccessNode extends ASTNode {
    private final ASTNode object;
    private final String fieldName;

    public FieldAccessNode(ASTNode object, String fieldName, int lineNumber, int columnNumber) {
        super(NodeType.FIELD_ACCESS, lineNumber, columnNumber);
        this.object = object;
        this.fieldName = fieldName;
        if (object != null) addChild(object);
    }

    public ASTNode getObject() { return object; }
    public String getFieldName() { return fieldName; }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitFieldAccess(this);
    }
}