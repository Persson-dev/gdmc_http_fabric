package com.persson.gdmc.handlers;

import net.minecraft.SharedConstants;
import com.google.gson.JsonObject;
import com.persson.gdmc.GdmcHttpMod;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;

public class InterfaceInfoHandler extends HandlerBase {
	public InterfaceInfoHandler(MinecraftServer mcServer) {
		super(mcServer);
	}

	@Override
	protected void internalHandle(HttpExchange httpExchange) throws IOException {
		String method = httpExchange.getRequestMethod().toLowerCase();

		if (!method.equals("options")) {
			throw new HttpException("Method not allowed. Only OPTIONS requests are supported.", 405);
		}

		Headers responseHeaders = httpExchange.getResponseHeaders();
		setDefaultResponseHeaders(responseHeaders);

		JsonObject json = new JsonObject();
		json.addProperty("minecraftVersion", SharedConstants.getGameVersion().getName());
		// Return DataVersion (https://minecraft.wiki/w/Data_version) of current
		// Minecraft version.
		// Beware that the current server world might be created in an older version of
		// Minecraft and
		// hence might have a different DataVersion.
		json.addProperty(SharedConstants.DATA_VERSION_KEY, SharedConstants.getGameVersion().getSaveVersion().getId());
		json.addProperty("interfaceVersion", GdmcHttpMod.MOD_VERSION);

		resolveRequest(httpExchange, json.toString());
	}
}
