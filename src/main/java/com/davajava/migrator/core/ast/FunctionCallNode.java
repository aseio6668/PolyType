package com.davajava.migrator.core.ast;

import java.util.List;
import java.util.ArrayList;

public class FunctionCallNode extends ASTNode {
    private final String functionName;
    private final List<ASTNode> arguments;

    public FunctionCallNode(String functionName, int lineNumber, int columnNumber) {
        super(NodeType.FUNCTION_CALL, lineNumber, columnNumber);
        this.functionName = functionName;
        this.arguments = new ArrayList<>();
    }

    public String getFunctionName() { return functionName; }
    public List<ASTNode> getArguments() { return arguments; }

    public void addArgument(ASTNode argument) {
        arguments.add(argument);
        addChild(argument);
    }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitFunctionCall(this);
    }
}