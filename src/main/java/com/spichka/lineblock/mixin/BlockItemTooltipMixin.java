package com.spichka.lineblock.mixin;

import com.spichka.lineblock.lang.lexer.TokenType;

import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BlockItem.class)
public abstract class BlockItemTooltipMixin {
    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void lineblock$addTypeTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context, CallbackInfo ci) {
        BlockItem blockItem = (BlockItem) (Object) this;
        TokenType type = TokenType.fromBlock(blockItem.getBlock());
        if (type != null)
            tooltip.add(Text.literal("[LineBlock] " + type.name()));
        else if (blockItem.getBlock() == Blocks.OBSERVER)
            tooltip.add(Text.literal("[LineBlock] DIRECTION"));
    }
}
