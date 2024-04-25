package com.persson.gdmc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.persson.gdmc.config.GdmcHttpConfig;
import com.persson.gdmc.utils.Feedback;
import com.persson.gdmc.utils.RegistryHandler;

import java.io.IOException;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarting;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopping;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Join;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class GdmcHttpMod implements ModInitializer, ServerStarting, ServerStopping, Join {
    public static final String MODID = "gdmc_http_interface";

    public static String MOD_VERSION;

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger(MODID);
    private static String configFilePath;

    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        ModContainer modData = loader.getModContainer(MODID).get();

        MOD_VERSION = modData.getMetadata().getVersion().getFriendlyString();
        configFilePath = loader.getConfigDir() + "/" + MODID + ".json";

        ServerLifecycleEvents.SERVER_STARTING.register(this);
        ServerLifecycleEvents.SERVER_STOPPING.register(this);
        ServerPlayConnectionEvents.JOIN.register(this);
    }

    @Override
    public void onServerStarting(MinecraftServer minecraftServer) {
        // do something when the server starts
        LOGGER.info("Server starting");

        GdmcHttpConfig.loadConfig(configFilePath);
        RegistryHandler.registerCommands(minecraftServer);

        try {
            GdmcHttpServer.startServer(minecraftServer);
            minecraftServer.sendMessage(successMessage());
        } catch (IOException e) {
            LOGGER.warn("Unable to start server!");
            minecraftServer.sendMessage(failureMessage());
        }
    }

    @Override
    public void onServerStopping(MinecraftServer server) {
        LOGGER.info("Server stopping");

        GdmcHttpServer.stopServer();
        GdmcHttpConfig.saveConfig(configFilePath);
    }

    @Override
    public void onPlayReady(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        if (player != null) {
            player.sendMessage(GdmcHttpServer.hasHtppServerStarted ? successMessage() : failureMessage());
        }
    }

    private static Text successMessage() {
        return Feedback.chatMessage("Server started at ").append(
                Feedback.copyOnClickText(String.format("http://localhost:%s/", GdmcHttpServer.getCurrentHttpPort())));
    }

    private static Text failureMessage() {
        return Feedback.chatMessage("GDMC-HTTP server failed to start!");
    }
}
