package com.spichka.lineblock.lang.parser;

import java.util.ArrayList;
import java.util.List;

import com.spichka.lineblock.lang.lexer.Token;
import com.spichka.lineblock.lang.lexer.TokenType;
import com.spichka.lineblock.lang.parser.ast.AstNode;
import com.spichka.lineblock.lang.parser.ast.BinaryOpNode;
import com.spichka.lineblock.lang.parser.ast.BlockNode;
import com.spichka.lineblock.lang.parser.ast.CommandNode;
import com.spichka.lineblock.lang.parser.ast.ConstantNode;
import com.spichka.lineblock.lang.parser.ast.LiteralNode;
import com.spichka.lineblock.lang.parser.ast.UnaryOpNode;
import com.spichka.lineblock.lang.parser.ast.VariableNode;

public class Parser {
    private final List<Token> tokens;
    private Token currentToken;
    private int position;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }

    public AstNode parse() {
        BlockNode root = new BlockNode();

        while (position < tokens.size()) {
            AstNode codeLineNode = parseLine();
            root.addStatement(codeLineNode);
        }

        return root;
    }

    private Token match(List<TokenType> types) {
        if (position < tokens.size()) {
            Token currentToken = tokens.get(position);

            if (types.contains(currentToken.type)) {
                position++;
                this.currentToken = currentToken;
                return currentToken;
            }
        }
        return null;
    }

    private Token require(List<TokenType> types) {
        Token token = match(types);
        if (token != null)
            // TODO: Error
            return null;
        return token;
    }

    private AstNode parseLine() {
        if (match(List.of(
            TokenType.INT_ASSIGN, TokenType.STRING_ASSIGN,
            TokenType.FLOAT_ASSIGN, TokenType.BOOL_ASSIGN
        )) != null) {
           return parseVariable();
        } else if (match(List.of(TokenType.COMMAND, TokenType.STOP)) != null) {
            return new CommandNode(currentToken);
        } else if (match(List.of(TokenType.PRINT, TokenType.GOTO)) != null) {
        } else if (match(List.of(TokenType.PLACEBLOCK)) != null) {
        
        } else if (match(List.of(TokenType.IF)) != null) {
        
        }

        // just skip
        // TODO: Error
        position++;
        return null;
    }

    private AstNode parseVariable() {
        Token assign = currentToken;

        VariableNode variableNode;
        if (match(List.of(TokenType.VAR_INDEX)) != null) {
            List<Token> varIndexTokens = new ArrayList<Token>();
            varIndexTokens.add(currentToken);
            varIndexTokens.addAll(collectTokens(List.of(TokenType.VAR_INDEX)));
            variableNode = new VariableNode(varIndexTokens);
        } else {
            
        }

        BinaryOpNode binaryOpNode = new BinaryOpNode(assign, variableNode, null);
        return binaryOpNode;
    }

    private AstNode parseTerm() {
    }

    private AstNode parseFactor() {
        if (match(List.of(
            TokenType.PLUS, TokenType.MINUS,
            TokenType.NOT, TokenType.BIT_NOT,
            TokenType.SIN, TokenType.COS, TokenType.TAN,
            TokenType.ASIN, TokenType.ACOS, TokenType.ATAN,
            TokenType.ABS, TokenType.CEIL, TokenType.FLOOR
        )) != null) {
            Token operator = currentToken;
            return new UnaryOpNode(operator, parseFactor());
        } else if (match(List.of(TokenType.LPAR)) != null) {
            AstNode node = parseExpression();
            require(List.of(TokenType.RPAR));
            return node;
        } else if (match(List.of(
            TokenType.INT_ASSIGN, TokenType.FLOAT_ASSIGN,
            TokenType.STRING_ASSIGN, TokenType.BOOL_ASSIGN
        )) != null) {
            Token type = currentToken;
            return new LiteralNode(type, collectTokens(List.of(TokenType.ZERO, TokenType.ONE)));
        } else if (match(List.of(TokenType.PI, TokenType.E)) != null) {
            return new ConstantNode(currentToken);
        }
        // TODO: error
        return null;
    }

    private AstNode parseExpression() {
        AstNode node = parseTerm();

        while (match(List.of(
            TokenType.PLUS, TokenType.MINUS,
            TokenType.AND, TokenType.OR, TokenType.XOR,
            TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE, TokenType.EQ, TokenType.NE
        )) != null) {
            Token token = currentToken;
            node = new BinaryOpNode(token, node, parseTerm());
        }

        return node;
    }

    private List<Token> collectTokens(List<TokenType> validTypes) {
        List<Token> collected = new ArrayList<>();

        while (position < tokens.size()) {
            if (match(validTypes) != null)
                collected.add(currentToken);
            else
                break;
        }

        return collected;
    }

}
