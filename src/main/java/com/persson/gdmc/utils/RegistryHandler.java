package com.persson.gdmc.utils;

import com.mojang.brigadier.CommandDispatcher;
import com.persson.gdmc.commands.GetHttpInterfacePort;
import com.persson.gdmc.commands.SetBuildAreaCommand;
import com.persson.gdmc.commands.SetHttpInterfacePort;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;

public class RegistryHandler {

    public static void registerCommands(MinecraftServer minecraftServer) {
        CommandDispatcher<ServerCommandSource> dispatcher = minecraftServer.getCommandManager().getDispatcher();
        SetBuildAreaCommand.register(dispatcher);
        SetHttpInterfacePort.register(dispatcher);
        GetHttpInterfacePort.register(dispatcher);
    }
}
