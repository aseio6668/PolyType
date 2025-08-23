package com.davajava.migrator.core.ast;

public class ParameterNode extends ASTNode {
    private final String name;
    private final String type;
    private final boolean isMutable;

    public ParameterNode(String name, String type, boolean isMutable, int lineNumber, int columnNumber) {
        super(NodeType.PARAMETER, lineNumber, columnNumber);
        this.name = name;
        this.type = type;
        this.isMutable = isMutable;
    }

    public String getName() {
        return name;
    }

    public String getDataType() {
        return type;
    }

    public boolean isMutable() {
        return isMutable;
    }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitParameter(this);
    }
}