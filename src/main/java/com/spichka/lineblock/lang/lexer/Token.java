package com.spichka.lineblock.lang.lexer;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Token {
    public BlockPos pos;
    public TokenType type;

    public Token(BlockPos pos, TokenType type) {
        this.pos = pos;
        this.type = type;
    }

    // factory method
    public static Token fromWorld(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        TokenType type = TokenType.fromBlock(block);

        if (type == null)
            return null;

        return new Token(pos, type);
    }

    @Override
    public String toString() {
        return "Token{" +
                "pos=" + pos +
                ", type=" + type +
                '}';
    }
}
