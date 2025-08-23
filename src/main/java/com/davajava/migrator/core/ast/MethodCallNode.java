package com.davajava.migrator.core.ast;

import java.util.List;
import java.util.ArrayList;

public class MethodCallNode extends ASTNode {
    private final ASTNode object;
    private final String methodName;
    private final List<ASTNode> arguments;

    public MethodCallNode(ASTNode object, String methodName, int lineNumber, int columnNumber) {
        super(NodeType.METHOD_CALL, lineNumber, columnNumber);
        this.object = object;
        this.methodName = methodName;
        this.arguments = new ArrayList<>();
        if (object != null) addChild(object);
    }

    public ASTNode getObject() { return object; }
    public String getMethodName() { return methodName; }
    public List<ASTNode> getArguments() { return arguments; }

    public void addArgument(ASTNode argument) {
        arguments.add(argument);
        addChild(argument);
    }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitMethodCall(this);
    }
}