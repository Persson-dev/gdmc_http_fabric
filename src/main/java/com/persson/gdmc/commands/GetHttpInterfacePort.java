package com.persson.gdmc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.persson.gdmc.GdmcHttpServer;
import com.persson.gdmc.utils.Feedback;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class GetHttpInterfacePort {

	private static final String COMMAND_NAME = "gethttpport";

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal(COMMAND_NAME)
			.executes(GetHttpInterfacePort :: perform)
		);
	}

	private static int perform(CommandContext<ServerCommandSource> commandSourceContext) {
		int currentPort = GdmcHttpServer.getCurrentHttpPort();
		Feedback.sendSucces(
			commandSourceContext,
			Feedback.chatMessage("Current GDMC-HTTP port: ").append(Feedback.copyOnClickText(String.valueOf(currentPort)))
		);
		return currentPort;
	}
}
