package com.spichka.lineblock.lang.parser.ast;

public class ForNode extends AstNode {
    public final AstNode initializerNode;
    public final AstNode conditionNode;
    public final AstNode incrementNode;
    public final AstNode bodyNode;

    public ForNode(AstNode initializerNode, AstNode conditionNode, AstNode incrementNode, AstNode bodyNode) {
        this.initializerNode = initializerNode;
        this.conditionNode = conditionNode;
        this.incrementNode = incrementNode;
        this.bodyNode = bodyNode;
    }

    public String toString() {
        return "For(" + initializerNode + ", " + conditionNode + ", " + incrementNode + ", " + bodyNode + ")";
    }
}
