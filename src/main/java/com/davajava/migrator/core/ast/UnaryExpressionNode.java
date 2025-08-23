package com.davajava.migrator.core.ast;

public class UnaryExpressionNode extends ExpressionNode {
    private final String operator;
    private final ASTNode operand;

    public UnaryExpressionNode(String operator, ASTNode operand, int lineNumber, int columnNumber) {
        super(lineNumber, columnNumber);
        this.operator = operator;
        this.operand = operand;
        addChild(operand);
    }

    public String getOperator() { return operator; }
    public ASTNode getOperand() { return operand; }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitUnaryExpression(this);
    }
}