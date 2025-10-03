package com.spichka.lineblock.lang.parser.ast;

import com.spichka.lineblock.lang.lexer.Token;

public class BinaryOpNode extends AstNode {
    public final Token operator;
    public final AstNode left;
    public final AstNode right;

    public BinaryOpNode(Token operator, AstNode left, AstNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "Binary(" + operator + ", " + left + ", " + right + ')';
    }
}
