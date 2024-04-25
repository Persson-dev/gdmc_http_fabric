package com.persson.gdmc.utils;

import net.minecraft.server.command.CommandOutput;
import net.minecraft.text.Text;

public class CustomCommandSource implements CommandOutput {

    private Text lastOutput;

    @Override
    public void sendMessage(Text message) {
        lastOutput = message.copy();
    }

    @Override
    public boolean shouldBroadcastConsoleToOps() {
        return false;
    }

    @Override
    public boolean shouldReceiveFeedback() {
        return false;
    }

    @Override
    public boolean shouldTrackOutput() {
        return false;
    }

    public Text getLastOutput() {
        return this.lastOutput;
    }
}
