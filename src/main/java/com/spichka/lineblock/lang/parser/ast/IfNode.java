package com.spichka.lineblock.lang.parser.ast;

public class IfNode extends AstNode {
    public final AstNode conditionNode;
    public final AstNode thenBranchNode;
    public final AstNode elseBranchNode;

    public IfNode(AstNode conditionNode, AstNode thenBranchNode, AstNode elseBranchNode) {
        this.conditionNode = conditionNode;
        this.thenBranchNode = thenBranchNode;
        this.elseBranchNode = elseBranchNode;
    }

    @Override
    public String toString() {
        return "If(" + conditionNode + ", " + thenBranchNode + ", " + elseBranchNode + ")";
    }
}
