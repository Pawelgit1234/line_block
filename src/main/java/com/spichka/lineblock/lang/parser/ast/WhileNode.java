package com.spichka.lineblock.lang.parser.ast;

public class WhileNode extends AstNode {
    public final AstNode conditionNode;
    public final AstNode bodyNode;

    public WhileNode(AstNode conditionNode, AstNode bodyNode) {
        this.conditionNode = conditionNode;
        this.bodyNode = bodyNode;
    }

    public String toString() {
        return "While(" + conditionNode + ", " + bodyNode + ")";
    }
}
