package com.davajava.migrator.core.ast;

public class ReturnStatementNode extends ASTNode {
    private final ASTNode expression;

    public ReturnStatementNode(ASTNode expression, int lineNumber, int columnNumber) {
        super(NodeType.RETURN_STATEMENT, lineNumber, columnNumber);
        this.expression = expression;
        if (expression != null) addChild(expression);
    }

    public ASTNode getExpression() { return expression; }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitReturnStatement(this);
    }
}