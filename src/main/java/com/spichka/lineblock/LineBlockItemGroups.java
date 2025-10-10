package com.spichka.lineblock;

import com.spichka.lineblock.lang.lexer.TokenType;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class LineBlockItemGroups {

    public static final ItemGroup LINEBLOCK_GROUP = FabricItemGroup.builder()
        .icon(() -> new ItemStack(Items.COMMAND_BLOCK))
        .displayName(Text.translatable("LineBlock"))
        .entries((context, entries) -> {
            for (Block block : TokenType.BLOCK_TO_TYPE.keySet())
                entries.add(block.asItem());
            entries.add(Blocks.OBSERVER.asItem());
        })
        .build();

    public static void initialize() {
        Registry.register(Registries.ITEM_GROUP, new Identifier(LineBlock.MOD_ID, "lineblock_group"), LINEBLOCK_GROUP);
    }
}
