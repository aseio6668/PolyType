package com.davajava.migrator.core.ast;

public class BinaryExpressionNode extends ExpressionNode {
    private final String operator;
    private final ASTNode left;
    private final ASTNode right;

    public BinaryExpressionNode(String operator, ASTNode left, ASTNode right, int lineNumber, int columnNumber) {
        super(lineNumber, columnNumber);
        this.operator = operator;
        this.left = left;
        this.right = right;
        addChild(left);
        addChild(right);
    }

    public String getOperator() { return operator; }
    public ASTNode getLeft() { return left; }
    public ASTNode getRight() { return right; }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitBinaryExpression(this);
    }
}