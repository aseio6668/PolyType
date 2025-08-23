package com.davajava.migrator.core.ast;

public class IdentifierNode extends ASTNode {
    private final String name;

    public IdentifierNode(String name, int lineNumber, int columnNumber) {
        super(NodeType.IDENTIFIER, lineNumber, columnNumber);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitIdentifier(this);
    }

    @Override
    public String toString() {
        return String.format("IdentifierNode{name='%s'}", name);
    }
}