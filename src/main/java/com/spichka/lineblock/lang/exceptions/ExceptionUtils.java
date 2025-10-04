package com.spichka.lineblock.lang.exceptions;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ExceptionUtils {
    public static void showError(ServerCommandSource source, LineBlockException e) {
        Text errorText = Text.literal("[LineBlock] " + e.getMessage()).formatted(Formatting.RED);

        if (source.getEntity() instanceof net.minecraft.server.network.ServerPlayerEntity player) {
            player.sendMessage(errorText, false);
        } else {
            source.sendFeedback(() -> errorText, true);
        }
    }
}
