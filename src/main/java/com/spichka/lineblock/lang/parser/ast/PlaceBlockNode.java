package com.spichka.lineblock.lang.parser.ast;

import net.minecraft.block.Block;

public class PlaceBlockNode extends AstNode {
    public final AstNode x;
    public final AstNode y;
    public final AstNode z;
    public final Block block;

    public PlaceBlockNode(AstNode x, AstNode y, AstNode z, Block block) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
    }

    @Override
    public String toString() {
        return "PlaceBlock(" + x + ", " + y + ", " + z + ", " + block + ")";
    }
}
