package com.spichka.lineblock;

import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.spichka.lineblock.lang.lexer.Lexer;
import com.spichka.lineblock.lang.lexer.Token;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RunCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                CommandManager.literal("run")
                    .then(CommandManager.argument("x", net.minecraft.command.argument.BlockPosArgumentType.blockPos())
                        .executes(ctx -> execute(ctx)))
            );
        });
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        BlockPos pos = net.minecraft.command.argument.BlockPosArgumentType.getBlockPos(context, "x");
        World world = source.getWorld();

        Lexer lexer = new Lexer(world, pos);
        List<Token> tokens = lexer.tokenize();

        world.getServer().sendMessage(Text.of("Starts from Block: " + pos));

        return Command.SINGLE_SUCCESS;
    }
}
