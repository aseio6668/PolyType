package com.davajava.migrator.core.ast;

public class LiteralNode extends ASTNode {
    private final Object value;
    private final LiteralType literalType;

    public enum LiteralType {
        INTEGER, FLOAT, STRING, BOOLEAN, CHARACTER, NULL
    }

    public LiteralNode(Object value, LiteralType literalType, int lineNumber, int columnNumber) {
        super(NodeType.LITERAL, lineNumber, columnNumber);
        this.value = value;
        this.literalType = literalType;
    }

    public Object getValue() {
        return value;
    }

    public LiteralType getLiteralType() {
        return literalType;
    }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitLiteral(this);
    }

    @Override
    public String toString() {
        return String.format("LiteralNode{value=%s, type=%s}", value, literalType);
    }
}