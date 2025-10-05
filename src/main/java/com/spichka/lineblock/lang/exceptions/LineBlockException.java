package com.spichka.lineblock.lang.exceptions;

import com.spichka.lineblock.lang.lexer.Token;

public class LineBlockException extends RuntimeException {
    public LineBlockException(String msg, Token token) {
        super("Error at " + token.pos + ": " + msg);
    }
}
