package com.spichka.lineblock.lang.lexer;

import java.util.ArrayList;
import java.util.List;

import com.spichka.lineblock.LineBlock;

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
            LineBlock.LOGGER.info("2");
            tokens.add(token);

            if (
                token.type == TokenType.INT_ASSIGN ||
                token.type == TokenType.FLOAT_ASSIGN ||
                token.type == TokenType.STRING_ASSIGN ||
                token.type == TokenType.BOOL_ASSIGN ||
                token.type == TokenType.USE_VAR
            ) {
                for (Direction dir : Direction.values()) {
                    if (dir == direction || dir == direction.getOpposite())
                        continue;

                    BlockPos neighborPos = currentPos.offset(dir);
                    Lexer lexer = new Lexer(world, neighborPos, dir);
                    tokens.addAll(lexer.tokenize());
                }
            }
        }

        for (Token t : tokens)
            LineBlock.LOGGER.info(t.toString());

        return tokens;
    }

    private Token nextToken() {
        BlockState state = world.getBlockState(currentPos);
        TokenType type = TokenType.fromBlock(state.getBlock());
        LineBlock.LOGGER.info("1");

        if (type == null) {
            if (state.isOf(Blocks.OBSERVER)) {
                LineBlock.LOGGER.info("3");
                direction = state.get(Properties.FACING);
                currentPos = currentPos.offset(direction);
                return nextToken();
            }
            LineBlock.LOGGER.info("4");
            return null;
        }

        Token token = new Token(currentPos, type);
        currentPos = currentPos.offset(direction);
        return token;
    }
}
