package com.davajava.migrator.core.ast;

public class IfStatementNode extends ASTNode {
    private final ASTNode condition;
    private final ASTNode thenStatement;
    private final ASTNode elseStatement;

    public IfStatementNode(ASTNode condition, ASTNode thenStatement, ASTNode elseStatement, int lineNumber, int columnNumber) {
        super(NodeType.IF_STATEMENT, lineNumber, columnNumber);
        this.condition = condition;
        this.thenStatement = thenStatement;
        this.elseStatement = elseStatement;
        addChild(condition);
        addChild(thenStatement);
        if (elseStatement != null) addChild(elseStatement);
    }

    public ASTNode getCondition() { return condition; }
    public ASTNode getThenStatement() { return thenStatement; }
    public ASTNode getElseStatement() { return elseStatement; }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitIfStatement(this);
    }
}