package com.spichka.lineblock.lang.parser.ast;

import com.spichka.lineblock.lang.lexer.Token;

// for PI, E
public class ConstantNode extends AstNode {
    public final Token constant;

    public ConstantNode(Token constant) {
        this.constant = constant;
    }

    @Override
    public String toString() {
        return "Constant(" + constant + ")";
    }
}
