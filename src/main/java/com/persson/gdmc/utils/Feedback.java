package com.persson.gdmc.utils;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

public class Feedback {

	public static MutableText chatMessage(String str) {
		return modNamePrefix().append(str);
	}

	private static MutableText modNamePrefix() {
		return Texts.bracketed(Text.literal("GDMC-HTTP").formatted(Formatting.DARK_AQUA))
				.append(" ");
	}

	public static MutableText copyOnClickText(String str) {
		return copyOnClickText(str, str);
	}

	public static MutableText copyOnClickText(String str, String clipboardContent) {
		return Text.literal(str)
				.styled((style) -> style.withUnderline(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, clipboardContent))
						.withHoverEvent(
								new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("chat.copy.click")))
						.withInsertion(str));
	}

	public static void sendSucces(CommandContext<ServerCommandSource> commandSourceContext, MutableText message) {
		ServerCommandSource source = commandSourceContext.getSource();
		source.sendFeedback(() -> message, true);
	}

}
