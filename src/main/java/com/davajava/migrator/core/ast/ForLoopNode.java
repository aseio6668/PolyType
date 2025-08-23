package com.davajava.migrator.core.ast;

public class ForLoopNode extends ASTNode {
    private final ASTNode initialization;
    private final ASTNode condition;
    private final ASTNode increment;
    private final ASTNode body;

    public ForLoopNode(ASTNode initialization, ASTNode condition, ASTNode increment, ASTNode body, int lineNumber, int columnNumber) {
        super(NodeType.FOR_LOOP, lineNumber, columnNumber);
        this.initialization = initialization;
        this.condition = condition;
        this.increment = increment;
        this.body = body;
        if (initialization != null) addChild(initialization);
        if (condition != null) addChild(condition);
        if (increment != null) addChild(increment);
        addChild(body);
    }

    public ASTNode getInitialization() { return initialization; }
    public ASTNode getCondition() { return condition; }
    public ASTNode getIncrement() { return increment; }
    public ASTNode getBody() { return body; }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitForLoop(this);
    }
}