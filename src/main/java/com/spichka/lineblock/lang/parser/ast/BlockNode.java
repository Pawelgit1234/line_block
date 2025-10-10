package com.spichka.lineblock.lang.parser.ast;

import java.util.ArrayList;
import java.util.List;

public class BlockNode extends AstNode {
    public final List<AstNode> statements;

    public BlockNode() {
        this.statements = new ArrayList<>();
    }

    public void addStatement(AstNode node) {
        this.statements.add(node);
    }

    @Override
    public String toString() {
        String str = "Block(";
        for (AstNode node : statements) {
            str += node + ",\n";
        }
        str += ')';
        return str;
    }
}
