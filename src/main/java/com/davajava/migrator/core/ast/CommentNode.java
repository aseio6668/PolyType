package com.davajava.migrator.core.ast;

public class CommentNode extends ASTNode {
    private final String content;
    private final boolean isBlockComment;

    public CommentNode(String content, boolean isBlockComment, int lineNumber, int columnNumber) {
        super(NodeType.COMMENT, lineNumber, columnNumber);
        this.content = content;
        this.isBlockComment = isBlockComment;
    }

    public String getContent() { return content; }
    public boolean isBlockComment() { return isBlockComment; }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitComment(this);
    }
}