package com.spichka.lineblock.lang.parser.ast;

import java.util.List;

import com.spichka.lineblock.lang.lexer.Token;

// contains the index of the variable (indexes are just shorter than names)
public class VariableNode extends AstNode {
    public final List<Token> index;

    public VariableNode(List<Token> index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "VariableNode(" + index.size() + ')';
    }
}
