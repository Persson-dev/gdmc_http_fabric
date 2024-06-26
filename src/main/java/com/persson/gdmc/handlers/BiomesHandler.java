package com.persson.gdmc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.persson.gdmc.utils.BuildArea;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class BiomesHandler extends HandlerBase {

	public BiomesHandler(MinecraftServer mcServer) {
		super(mcServer);
	}

	@Override
	protected void internalHandle(HttpExchange httpExchange) throws IOException {
		// query parameters
		Map<String, String> queryParams = parseQueryString(httpExchange.getRequestURI().getRawQuery());

		// GET: x, y, z positions
		int x;
		int y;
		int z;

		// GET: Ranges in the x, y, z directions (can be negative). Defaults to 1.
		int dx;
		int dy;
		int dz;

		// GET: if true, constrain getting biomes within the current build area.
		boolean withinBuildArea;

		String dimension;

		try {
			x = Integer.parseInt(queryParams.getOrDefault("x", "0"));
			y = Integer.parseInt(queryParams.getOrDefault("y", "0"));
			z = Integer.parseInt(queryParams.getOrDefault("z", "0"));

			dx = Integer.parseInt(queryParams.getOrDefault("dx", "1"));
			dy = Integer.parseInt(queryParams.getOrDefault("dy", "1"));
			dz = Integer.parseInt(queryParams.getOrDefault("dz", "1"));

			withinBuildArea = Boolean.parseBoolean(queryParams.getOrDefault("withinBuildArea", "false"));

			dimension = queryParams.getOrDefault("dimension", null);
		} catch (NumberFormatException e) {
			throw new HttpException("Could not parse query parameter: " + e.getMessage(), 400);
		}

		String method = httpExchange.getRequestMethod().toLowerCase();

		if (!method.equals("get")) {
			throw new HttpException("Method not allowed. Only GET requests are supported.", 405);
		}

		// Response headers
		Headers responseHeaders = httpExchange.getResponseHeaders();
		setDefaultResponseHeaders(responseHeaders);

		JsonArray responseList = new JsonArray();

		ServerWorld serverLevel = getServerLevel(dimension);

		// Calculate boundaries of area of blocks to gather biome information on.
		BlockBox box = BuildArea.clampToBuildArea(createBoundingBox(x, y, z, dx, dy, dz), withinBuildArea);

		// Create an ordered map with an entry for every block position we want to know the biome of.
		LinkedHashMap<BlockPos, JsonObject> blockPosMap = new LinkedHashMap<>();
		HashMap<ChunkPos, WorldChunk> chunkPosMap = new HashMap<>();
		for (int rangeX = box.getMinX(); rangeX <= box.getMaxX(); rangeX++) {
			for (int rangeY = box.getMinY(); rangeY <= box.getMaxY(); rangeY++) {
				for (int rangeZ = box.getMinZ(); rangeZ <= box.getMaxZ(); rangeZ++) {
					BlockPos blockPos = new BlockPos(rangeX, rangeY, rangeZ);
					blockPosMap.put(blockPos, null);
					chunkPosMap.put(new ChunkPos(blockPos), null);
				}
			}
		}
		chunkPosMap.keySet().parallelStream().forEach(chunkPos -> chunkPosMap.replace(chunkPos, serverLevel.getChunk(chunkPos.x, chunkPos.z)));
		// Gather biome information for each position in parallel.
		blockPosMap.keySet().parallelStream().forEach(blockPos -> {
			WorldChunk levelChunk = chunkPosMap.get(new ChunkPos(blockPos));
			Optional<RegistryKey<Biome>> biomeResourceKey = levelChunk.getBiomeForNoiseGen(blockPos.getX(), blockPos.getY(), blockPos.getZ()).getKey();
			if (biomeResourceKey.isPresent()) {
				if (serverLevel.isInBuildLimit(blockPos)) {
					JsonObject json = new JsonObject();
					json.addProperty("id", biomeResourceKey.get().toString());
					json.addProperty("x", blockPos.getX());
					json.addProperty("y", blockPos.getY());
					json.addProperty("z", blockPos.getZ());
					blockPosMap.replace(blockPos, json);
				}
			}
		});
		// Create a JsonArray with JsonObject, each contain a key-value pair for
		// the x, y, z position and the namespaced biome name.
		for (JsonObject biomeJson : blockPosMap.values()) {
			if (biomeJson != null) {
				responseList.add(biomeJson);
			}
		}

		resolveRequest(httpExchange, responseList.toString());
	}
}
