package com.davajava.migrator.core.ast;

import java.util.List;
import java.util.ArrayList;

public class BlockStatementNode extends ASTNode {
    private final List<ASTNode> statements;

    public BlockStatementNode(int lineNumber, int columnNumber) {
        super(NodeType.BLOCK_STATEMENT, lineNumber, columnNumber);
        this.statements = new ArrayList<>();
    }

    public List<ASTNode> getStatements() { return statements; }
    
    public void addStatement(ASTNode statement) {
        statements.add(statement);
        addChild(statement);
    }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitBlockStatement(this);
    }
}