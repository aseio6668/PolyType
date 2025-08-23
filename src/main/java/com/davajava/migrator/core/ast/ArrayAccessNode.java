package com.davajava.migrator.core.ast;

public class ArrayAccessNode extends ASTNode {
    private final ASTNode array;
    private final ASTNode index;

    public ArrayAccessNode(ASTNode array, ASTNode index, int lineNumber, int columnNumber) {
        super(NodeType.ARRAY_ACCESS, lineNumber, columnNumber);
        this.array = array;
        this.index = index;
        addChild(array);
        addChild(index);
    }

    public ASTNode getArray() { return array; }
    public ASTNode getIndex() { return index; }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitArrayAccess(this);
    }
}