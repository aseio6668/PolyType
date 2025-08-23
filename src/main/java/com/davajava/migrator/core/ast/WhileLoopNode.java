package com.davajava.migrator.core.ast;

public class WhileLoopNode extends ASTNode {
    private final ASTNode condition;
    private final ASTNode body;

    public WhileLoopNode(ASTNode condition, ASTNode body, int lineNumber, int columnNumber) {
        super(NodeType.WHILE_LOOP, lineNumber, columnNumber);
        this.condition = condition;
        this.body = body;
        addChild(condition);
        addChild(body);
    }

    public ASTNode getCondition() { return condition; }
    public ASTNode getBody() { return body; }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitWhileLoop(this);
    }
}