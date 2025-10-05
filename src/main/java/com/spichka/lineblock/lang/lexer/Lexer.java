package com.spichka.lineblock.lang.lexer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.state.property.Properties;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class Lexer {
    private final World world;
    private BlockPos currentPos;
    private Direction direction;

    public Lexer(World world, BlockPos startPos, Direction direction) {
        this.world = world;
        this.currentPos = startPos;
        this.direction = direction;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        Token token;
        while ((token = nextToken()) != null) {
            tokens.add(token);

            if (
                token.type == TokenType.PRINT ||
                token.type == TokenType.GOTO ||
                token.type == TokenType.PLACEBLOCK ||
                token.type == TokenType.INT ||
                token.type == TokenType.FLOAT ||
                token.type == TokenType.STRING ||
                token.type == TokenType.BOOL ||
                token.type == TokenType.USE_VAR ||
                token.type == TokenType.IF
            ) {
                for (Direction dir : Direction.values()) {
                    if (dir == direction || dir == direction.getOpposite())
                        continue;
                    BlockPos neighborPos = token.pos.offset(dir);
                    BlockState state = world.getBlockState(neighborPos);
                    TokenType neighborTokenType = TokenType.fromBlock(state.getBlock());

                    if (neighborTokenType != null) {
                        Lexer lexer = new Lexer(world, neighborPos, dir);
                        tokens.addAll(lexer.tokenize());
                    }
                }
            }
        }

        return tokens;
    }

    private Token nextToken() {
        BlockState state = world.getBlockState(currentPos);
        TokenType type = TokenType.fromBlock(state.getBlock());

        if (type == null) {
            if (state.isOf(Blocks.OBSERVER)) {
                direction = state.get(Properties.FACING);
                currentPos = currentPos.offset(direction);
                return nextToken();
            }
            return null;
        }

        Token token = new Token(currentPos, type);
        currentPos = currentPos.offset(direction);
        return token;
    }
}