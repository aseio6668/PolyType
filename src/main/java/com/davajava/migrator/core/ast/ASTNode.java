package com.davajava.migrator.core.ast;

import java.util.ArrayList;
import java.util.List;

public abstract class ASTNode {
    protected final NodeType type;
    protected final List<ASTNode> children;
    protected ASTNode parent;
    protected final int lineNumber;
    protected final int columnNumber;

    public ASTNode(NodeType type, int lineNumber, int columnNumber) {
        this.type = type;
        this.children = new ArrayList<>();
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public NodeType getType() {
        return type;
    }

    public List<ASTNode> getChildren() {
        return new ArrayList<>(children);
    }

    public void addChild(ASTNode child) {
        children.add(child);
        child.parent = this;
    }

    public ASTNode getParent() {
        return parent;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public abstract String accept(ASTVisitor visitor);
}