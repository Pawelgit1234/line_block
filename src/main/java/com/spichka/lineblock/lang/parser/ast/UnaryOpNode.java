package com.spichka.lineblock.lang.parser.ast;

import com.spichka.lineblock.lang.lexer.Token;

public class UnaryOpNode extends AstNode {
    public final Token operator;
    public final AstNode operand;

    public UnaryOpNode(Token operator, AstNode operand) {
        this.operator = operator;
        this.operand = operand;
    }

    @Override
    public String toString() {
        return "Unary(" + operator + ", " + operand + ")";
    }
}
