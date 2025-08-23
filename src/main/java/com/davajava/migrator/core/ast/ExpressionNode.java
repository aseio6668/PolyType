package com.davajava.migrator.core.ast;

public class ExpressionNode extends ASTNode {
    public ExpressionNode(int lineNumber, int columnNumber) {
        super(NodeType.EXPRESSION, lineNumber, columnNumber);
    }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitExpression(this);
    }
}