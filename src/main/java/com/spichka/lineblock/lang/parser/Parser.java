package com.spichka.lineblock.lang.parser;

import java.util.ArrayList;
import java.util.List;

import com.spichka.lineblock.lang.exceptions.LineBlockException;
import com.spichka.lineblock.lang.lexer.Token;
import com.spichka.lineblock.lang.lexer.TokenType;
import com.spichka.lineblock.lang.parser.ast.AstNode;
import com.spichka.lineblock.lang.parser.ast.BinaryOpNode;
import com.spichka.lineblock.lang.parser.ast.BlockNode;
import com.spichka.lineblock.lang.parser.ast.CommandNode;
import com.spichka.lineblock.lang.parser.ast.ConstantNode;
import com.spichka.lineblock.lang.parser.ast.IfNode;
import com.spichka.lineblock.lang.parser.ast.LiteralNode;
import com.spichka.lineblock.lang.parser.ast.PlaceBlockNode;
import com.spichka.lineblock.lang.parser.ast.UnaryOpNode;
import com.spichka.lineblock.lang.parser.ast.VariableNode;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

public class Parser {
    private final List<Token> tokens;
    private final World world;
    private Token currentToken;
    private int position;

    public Parser(World world, List<Token> tokens) {
        this.tokens = tokens;
        this.world = world;
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
        if (token == null)
            throw new LineBlockException("One of " + types + " required", currentToken);
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
            Token operator = currentToken;
            return new UnaryOpNode(operator, parseExpression());
        } else if (match(List.of(TokenType.PLACEBLOCK)) != null) {
            return parsePlaceBlock();
        } else if (match(List.of(TokenType.IF)) != null) {
            return parseIf();
        }

        throw new LineBlockException("Wrong block", currentToken);
    }

    private AstNode parseVariable() {
        Token assign = currentToken;

        VariableNode variableNode;
        AstNode expressionNode;
        if (match(List.of(TokenType.VAR_INDEX)) != null) {
            List<Token> varIndexTokens = new ArrayList<Token>();
            varIndexTokens.add(currentToken);
            varIndexTokens.addAll(collectTokens(List.of(TokenType.VAR_INDEX)));
            variableNode = new VariableNode(varIndexTokens);
            expressionNode = parseExpression();
        } else {
            expressionNode = parseExpression();
            variableNode = new VariableNode(collectTokens(List.of(TokenType.VAR_INDEX)));
        }

        BinaryOpNode binaryOpNode = new BinaryOpNode(assign, variableNode, expressionNode);
        return binaryOpNode;
    }

    private AstNode parseTerm() {
        AstNode node = parseFactor();

        while (match(List.of(
            TokenType.MUL, TokenType.DIV, TokenType.MOD, TokenType.POW,
            TokenType.BIT_AND, TokenType.BIT_OR, TokenType.BIT_XOR, TokenType.SHL, TokenType.SHR
        )) != null) {
            Token token = currentToken;
            node = new BinaryOpNode(token, node, parseFactor());
        }

        return node;
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

        } else if (match(List.of(TokenType.USE_VAR)) != null) {
            return new VariableNode(collectTokens(List.of(TokenType.VAR_INDEX)));

        } else if (match(List.of(TokenType.PI, TokenType.E)) != null) {
            return new ConstantNode(currentToken);
        }
        throw new LineBlockException("Expected another value", currentToken);
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

    private AstNode parsePlaceBlock() {
        AstNode placeX = null;
        AstNode placeY = null;
        AstNode placeZ = null;

        BlockPos placeBlockPos = currentToken.pos;
        BlockPos fourthArgumentPos = null;

        for (int i = 0; i < 4; i++) {
            if (match(List.of(
                TokenType.FIRST_ARGUMENT, TokenType.SECOND_ARGUMENT,
                TokenType.THRID_ARGUMENT, TokenType.FOURTH_ARGUMENT
            )) != null) {
                Token argToken = currentToken;

                switch (argToken.type) {
                    case FIRST_ARGUMENT -> placeX = parseExpression();
                    case SECOND_ARGUMENT -> placeY = parseExpression();
                    case THRID_ARGUMENT -> placeZ = parseExpression();
                    case FOURTH_ARGUMENT -> fourthArgumentPos = argToken.pos;
                    default -> {}
                }
            } else {
                position++; // if fourth argument and tokenized block
                i--;
            }
        }

        int x = fourthArgumentPos.getX() + (fourthArgumentPos.getX() - placeBlockPos.getX());
        int y = fourthArgumentPos.getY() + (fourthArgumentPos.getY() - placeBlockPos.getY());
        int z = fourthArgumentPos.getZ() + (fourthArgumentPos.getZ() - placeBlockPos.getZ());

        BlockPos pos = new BlockPos(new Vec3i(x, y, z));
        return new PlaceBlockNode(placeX, placeY, placeZ, world.getBlockState(pos).getBlock());
    }

    private AstNode parseIfBranche() {
        BlockNode root = new BlockNode();

        while (match(List.of(TokenType.BRANCH_END)) == null) {
            AstNode codeLineNode = parseLine();
            root.addStatement(codeLineNode);
        }

        return root;
    }

    private AstNode parseIf() {
        AstNode conditionNode = null;
        AstNode thenBranchNode = null;
        AstNode elseBranchNode = null;

        for (int i = 0; i < 3; i++) {
            if (match(List.of(
                TokenType.FIRST_ARGUMENT, TokenType.SECOND_ARGUMENT,
                TokenType.THRID_ARGUMENT
            )) != null) {
                Token argToken = currentToken;

                switch (argToken.type) {
                    case FIRST_ARGUMENT -> conditionNode = parseExpression();
                    case SECOND_ARGUMENT -> thenBranchNode = parseIfBranche();
                    case THRID_ARGUMENT -> elseBranchNode = parseIfBranche();
                    default -> {}
                }
            } else {
                break; // if no else, then break
            }
        }

        return new IfNode(conditionNode, thenBranchNode, elseBranchNode);
    }
}
