package com.spichka.lineblock.lang.parser.ast;

import com.spichka.lineblock.lang.lexer.Token;

public class CommandNode extends AstNode {
    public final Token token;

    public CommandNode(Token token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "Command(" + token + ")";
    }
}
