package com.spichka.lineblock.lang.lexer;

import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public enum TokenType {
    PLUS, MINUS, MUL, DIV, MOD, POW, RPAR, LPAR, // math
    BIT_AND, BIT_OR, BIT_XOR, BIT_NOT, SHL, SHR, // bit
    ZERO, ONE, // 0 1
    AND, OR, NOT, XOR, // logic
    EQ, NE, GT, LT, GE, LE, // equation
    SIN, COS, TAN, ASIN, ACOS, ATAN, ABS, CEIL, FLOOR, // math func
    PI, E, // constants
    COMMAND, STOP, PRINT, GOTO, PLACEBLOCK, // other
    IF, BRANCH_END, // if
    FIRST_ARGUMENT, SECOND_ARGUMENT, THRID_ARGUMENT, FOURTH_ARGUMENT, // argument
    INT, FLOAT, STRING, BOOL, VAR_INDEX, USE_VAR; // variables

    private static final HashMap<Block, TokenType> BLOCK_TO_TYPE = new HashMap<>();

    static {
        // math
        BLOCK_TO_TYPE.put(Blocks.IRON_ORE, PLUS);
        BLOCK_TO_TYPE.put(Blocks.COPPER_ORE, MINUS);
        BLOCK_TO_TYPE.put(Blocks.GOLD_ORE, MUL);
        BLOCK_TO_TYPE.put(Blocks.REDSTONE_ORE, DIV);
        BLOCK_TO_TYPE.put(Blocks.EMERALD_ORE, MOD);
        BLOCK_TO_TYPE.put(Blocks.LAPIS_ORE, POW);
        BLOCK_TO_TYPE.put(Blocks.STRIPPED_CRIMSON_HYPHAE, LPAR);
        BLOCK_TO_TYPE.put(Blocks.CRIMSON_HYPHAE, RPAR);

        // bit
        BLOCK_TO_TYPE.put(Blocks.DEEPSLATE_IRON_ORE, BIT_AND);
        BLOCK_TO_TYPE.put(Blocks.DEEPSLATE_COPPER_ORE, BIT_OR);
        BLOCK_TO_TYPE.put(Blocks.DEEPSLATE_GOLD_ORE, BIT_XOR);
        BLOCK_TO_TYPE.put(Blocks.DEEPSLATE_LAPIS_ORE, BIT_NOT);
        BLOCK_TO_TYPE.put(Blocks.DEEPSLATE_REDSTONE_ORE, SHL);
        BLOCK_TO_TYPE.put(Blocks.DEEPSLATE_EMERALD_ORE, SHR);

        // 0 1
        BLOCK_TO_TYPE.put(Blocks.WHITE_WOOL, ZERO);
        BLOCK_TO_TYPE.put(Blocks.BLACK_WOOL, ONE);

        // logic
        BLOCK_TO_TYPE.put(Blocks.NETHER_GOLD_ORE, AND);
        BLOCK_TO_TYPE.put(Blocks.NETHER_QUARTZ_ORE, OR);
        BLOCK_TO_TYPE.put(Blocks.ANCIENT_DEBRIS, NOT);
        BLOCK_TO_TYPE.put(Blocks.NETHERRACK, XOR);

        // equation
        BLOCK_TO_TYPE.put(Blocks.STONE, EQ);
        BLOCK_TO_TYPE.put(Blocks.DEEPSLATE, NE);
        BLOCK_TO_TYPE.put(Blocks.GRANITE, GT);
        BLOCK_TO_TYPE.put(Blocks.DIORITE, LT);
        BLOCK_TO_TYPE.put(Blocks.ANDESITE, GE);
        BLOCK_TO_TYPE.put(Blocks.POLISHED_ANDESITE, LE);

        // math func
        BLOCK_TO_TYPE.put(Blocks.WHITE_GLAZED_TERRACOTTA, SIN);
        BLOCK_TO_TYPE.put(Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA, COS);
        BLOCK_TO_TYPE.put(Blocks.GRAY_GLAZED_TERRACOTTA, TAN);
        BLOCK_TO_TYPE.put(Blocks.BLACK_GLAZED_TERRACOTTA, ASIN);
        BLOCK_TO_TYPE.put(Blocks.BROWN_GLAZED_TERRACOTTA, ACOS);
        BLOCK_TO_TYPE.put(Blocks.RED_GLAZED_TERRACOTTA, ATAN);
        BLOCK_TO_TYPE.put(Blocks.ORANGE_GLAZED_TERRACOTTA, ABS);
        BLOCK_TO_TYPE.put(Blocks.YELLOW_GLAZED_TERRACOTTA, CEIL);
        BLOCK_TO_TYPE.put(Blocks.LIME_GLAZED_TERRACOTTA, FLOOR);

        // constants
        BLOCK_TO_TYPE.put(Blocks.GREEN_GLAZED_TERRACOTTA, PI);
        BLOCK_TO_TYPE.put(Blocks.CYAN_GLAZED_TERRACOTTA, E);

        // other
        BLOCK_TO_TYPE.put(Blocks.COMMAND_BLOCK, COMMAND);
        BLOCK_TO_TYPE.put(Blocks.DARK_PRISMARINE, PRINT);
        BLOCK_TO_TYPE.put(Blocks.OBSIDIAN, GOTO);
        BLOCK_TO_TYPE.put(Blocks.TNT, STOP);
        BLOCK_TO_TYPE.put(Blocks.PISTON, PLACEBLOCK);

        // if
        BLOCK_TO_TYPE.put(Blocks.OAK_WOOD, IF);
        BLOCK_TO_TYPE.put(Blocks.STRIPPED_OAK_WOOD, BRANCH_END);

        // arguments
        BLOCK_TO_TYPE.put(Blocks.GLASS, FIRST_ARGUMENT);
        BLOCK_TO_TYPE.put(Blocks.TINTED_GLASS, SECOND_ARGUMENT);
        BLOCK_TO_TYPE.put(Blocks.RED_STAINED_GLASS, THRID_ARGUMENT);
        BLOCK_TO_TYPE.put(Blocks.LIME_STAINED_GLASS, FOURTH_ARGUMENT);

        // variables
        BLOCK_TO_TYPE.put(Blocks.DIAMOND_BLOCK, INT);
        BLOCK_TO_TYPE.put(Blocks.GOLD_BLOCK, FLOAT);
        BLOCK_TO_TYPE.put(Blocks.IRON_BLOCK, STRING);
        BLOCK_TO_TYPE.put(Blocks.EMERALD_BLOCK, BOOL);
        BLOCK_TO_TYPE.put(Blocks.CHISELED_STONE_BRICKS, USE_VAR);
        BLOCK_TO_TYPE.put(Blocks.SMOOTH_STONE, VAR_INDEX);
    }
    
    public static TokenType fromBlock(Block block) {
        return BLOCK_TO_TYPE.getOrDefault(block, null);
    }
}
