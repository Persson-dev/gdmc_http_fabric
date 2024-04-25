package com.persson.gdmc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.persson.gdmc.config.GdmcHttpConfig;
import com.persson.gdmc.utils.Feedback;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public final class SetHttpInterfacePort {

	private static final String COMMAND_NAME = "sethttpport";

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal(COMMAND_NAME)
				.executes(SetHttpInterfacePort::unsetInterfacePort)
				.then(CommandManager.argument("port", IntegerArgumentType.integer(0, 65535))
						.executes(context -> setInterfacePort(context,
								IntegerArgumentType.getInteger(context, "port")))));
	}

	private static int unsetInterfacePort(CommandContext<ServerCommandSource> commandSourceStack) {
		int defaultPort = GdmcHttpConfig.DEFAULT_HTTP_INTERFACE_PORT;
		GdmcHttpConfig.HTTP_INTERFACE_PORT = defaultPort;
		Feedback.sendSucces(
				commandSourceStack,
				Feedback.chatMessage("Port changed back to default value of ")
						.append(Feedback.copyOnClickText(String.valueOf(defaultPort)))
						.append(". Reload the world for it to take effect."));
		return defaultPort;
	}

	private static int setInterfacePort(CommandContext<ServerCommandSource> commandSourceContext, int newPortNumber) {
		if (newPortNumber > 65535 || newPortNumber < 0) {
			commandSourceContext.getSource().sendError(
					Feedback.chatMessage(String.format("Cannot change port number to %s: %s", newPortNumber)));
			return newPortNumber;
		}
		GdmcHttpConfig.HTTP_INTERFACE_PORT = newPortNumber;
		Feedback.sendSucces(
				commandSourceContext,
				Feedback.chatMessage("Port changed to ").append(Feedback.copyOnClickText(String.valueOf(newPortNumber)))
						.append(". Reload the world for it to take effect."));
		return newPortNumber;
	}
}
