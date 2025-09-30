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

    public Lexer(World world, BlockPos startPos) {
        this.world = world;
        this.currentPos = startPos;
        this.direction = Direction.EAST;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        Token token;
        while ((token = nextToken()) != null) {
            tokens.add(token);

            // TODO: If this type has something on sides
        }

        return tokens;
    }

    private Token nextToken() {
        currentPos = currentPos.offset(direction);
        BlockState state = world.getBlockState(currentPos);
        TokenType type = TokenType.fromBlock(state.getBlock());

        if (type == null) {
            if (state.isOf(Blocks.OBSERVER)) {
                direction = state.get(Properties.FACING);
                return nextToken();
            }
            return null;
        }

        return new Token(currentPos, type);
    }
}
