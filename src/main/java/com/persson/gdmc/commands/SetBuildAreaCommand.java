package com.persson.gdmc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.persson.gdmc.utils.BuildArea;
import com.persson.gdmc.utils.Feedback;

import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;

public final class SetBuildAreaCommand {

    private static final String COMMAND_NAME = "setbuildarea";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal(COMMAND_NAME)
            .executes(SetBuildAreaCommand :: unsetBuildArea)
            .then(
                CommandManager.argument("from", BlockPosArgumentType.blockPos())
            .then(
                CommandManager.argument("to", BlockPosArgumentType.blockPos())
            .executes( context -> setBuildArea(
                context,
                context.getArgument("from", PosArgument.class).toAbsoluteBlockPos(context.getSource()),
                context.getArgument("to", PosArgument.class).toAbsoluteBlockPos(context.getSource())
        )))));
    }

    private static int unsetBuildArea(CommandContext<ServerCommandSource> commandSourceStackCommandContext) {
        BuildArea.unsetBuildArea();
        Feedback.sendSucces(
            commandSourceStackCommandContext,
            Feedback.chatMessage("Build area unset")
        );
        return 1;
    }

    private static int setBuildArea(CommandContext<ServerCommandSource> commandSourceContext, BlockPos from, BlockPos to) {
        BuildArea.BuildAreaInstance newBuildArea = BuildArea.setBuildArea(from, to);
        Feedback.sendSucces(
            commandSourceContext,
            Feedback.chatMessage("Build area set ").append(
                Feedback.copyOnClickText(
                    String.format("from %s to %s", newBuildArea.from.toShortString(), newBuildArea.to.toShortString()),
                    BuildArea.toJSONString()
                )
            )
        );
        return 1;
    }
}
