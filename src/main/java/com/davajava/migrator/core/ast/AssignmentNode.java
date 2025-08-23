package com.davajava.migrator.core.ast;

public class AssignmentNode extends ASTNode {
    private final ASTNode target;
    private final ASTNode value;
    private final String operator;

    public AssignmentNode(ASTNode target, ASTNode value, String operator, int lineNumber, int columnNumber) {
        super(NodeType.ASSIGNMENT, lineNumber, columnNumber);
        this.target = target;
        this.value = value;
        this.operator = operator;
        addChild(target);
        addChild(value);
    }

    public ASTNode getTarget() { return target; }
    public ASTNode getValue() { return value; }
    public String getOperator() { return operator; }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitAssignment(this);
    }
}