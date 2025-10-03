package com.spichka.lineblock.lang.parser.ast;

import java.util.List;

import com.spichka.lineblock.lang.lexer.Token;

public class LiteralNode extends AstNode {
    Token type; // str, int, float or bool
    List<Token> bits;

    public LiteralNode(Token type, List<Token> bits) {
        this.type = type;
        this.bits = bits;
    }

    @Override
    public String toString() {
        return "Literal(" + type + ", " + bits + ")";
    }
}
