package com.spichka.lineblock;

import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.spichka.lineblock.lang.exceptions.ExceptionUtils;
import com.spichka.lineblock.lang.exceptions.LineBlockException;
import com.spichka.lineblock.lang.lexer.Lexer;
import com.spichka.lineblock.lang.lexer.Token;
import com.spichka.lineblock.lang.parser.Parser;
import com.spichka.lineblock.lang.parser.ast.AstNode;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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

        try {
            LineBlock.LOGGER.info("1. Lexer");
            Lexer lexer = new Lexer(world, pos, Direction.EAST);
            List<Token> tokens = lexer.tokenize();
            for (Token t : tokens)
                LineBlock.LOGGER.info(t.toString());

            LineBlock.LOGGER.info("2. Parser");
            Parser parser = new Parser(world, tokens);
            AstNode root = parser.parse();
            LineBlock.LOGGER.info(root.toString());

            LineBlock.LOGGER.info("3. Interpreter");
        } catch (LineBlockException e) {
            LineBlock.LOGGER.error("Error: ", e);
            ExceptionUtils.showError(source, e);
            return 0;
        }

        return Command.SINGLE_SUCCESS;
    }
}
