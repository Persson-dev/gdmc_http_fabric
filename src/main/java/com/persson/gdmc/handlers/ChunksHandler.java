package com.persson.gdmc.handlers;

import com.persson.gdmc.utils.BuildArea;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.WorldChunk;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class ChunksHandler extends HandlerBase {

    public ChunksHandler(MinecraftServer mcServer) {
        super(mcServer);
    }

    @Override
    public void internalHandle(HttpExchange httpExchange) throws IOException {

        String method = httpExchange.getRequestMethod().toLowerCase();
        if(!method.equals("get")) {
            throw new HttpException("Method not allowed. Only GET requests are supported.", 405);
        }

        // query parameters
        Map<String, String> queryParams = parseQueryString(httpExchange.getRequestURI().getRawQuery());

        // GET: Chunk coordinate at origin.
        int chunkX;
        int chunkZ;

        // GET: Ranges in the x and z directions (can be negative). Defaults to 1.
        int chunkDX;
        int chunkDZ;

        // GET: if true, constrain getting biomes within the current build area.
        boolean withinBuildArea;

        String dimension;

        BuildArea.BuildAreaInstance buildArea = null;
        try {
            buildArea = BuildArea.getBuildArea();
        } catch (HttpException ignored) {}

        try {
            if (queryParams.get("x") == null && buildArea != null) {
                chunkX = buildArea.sectionFrom.x;
            } else {
                chunkX = Integer.parseInt(queryParams.getOrDefault("x", "0"));
            }

            if (queryParams.get("z") == null && buildArea != null) {
                chunkZ = buildArea.sectionFrom.z;
            } else {
                chunkZ = Integer.parseInt(queryParams.getOrDefault("z", "0"));
            }

            if (queryParams.get("dx") == null && buildArea != null) {
                chunkDX = buildArea.sectionTo.x - buildArea.sectionFrom.x;
            } else {
                chunkDX = Integer.parseInt(queryParams.getOrDefault("dx", "1"));
            }

            if (queryParams.get("dz") == null && buildArea != null) {
                chunkDZ = buildArea.sectionTo.z - buildArea.sectionFrom.z;
            } else {
                chunkDZ = Integer.parseInt(queryParams.getOrDefault("dz", "1"));
            }

            withinBuildArea = Boolean.parseBoolean(queryParams.getOrDefault("withinBuildArea", "false"));

            dimension = queryParams.getOrDefault("dimension", null);
        } catch (NumberFormatException e) {
            String message = "Could not parse query parameter: " + e.getMessage();
            throw new HttpException(message, 400);
        }

        // Check if clients wants a response in plain-text. If not, return response
        // in a binary format.
        Headers requestHeaders = httpExchange.getRequestHeaders();
        String acceptHeader = getHeader(requestHeaders, "Accept", "*/*");
        boolean returnPlainText = acceptHeader.equals("text/plain");

        // If "Accept-Encoding" header is set to "gzip" and the client expects a binary format,
        // compress the result using GZIP before sending out the response.
        String acceptEncodingHeader = getHeader(requestHeaders, "Accept-Encoding", "*");
        boolean returnCompressed = acceptEncodingHeader.equals("gzip");

        ServerWorld serverLevel = getServerLevel(dimension);

        // Gather all chunk data within the given range.
        // Constrain start and end position to that of the build area if withinBuildArea is true.
        BlockBox box = BuildArea.clampChunksToBuildArea(createBoundingBox(
            chunkX, 0, chunkZ,
            chunkDX, 0, chunkDZ
        ), withinBuildArea);

        LinkedHashMap<ChunkPos, NbtCompound> chunkMap = new LinkedHashMap<>();
        for (int rangeZ = box.getMinZ(); rangeZ <= box.getMaxZ(); rangeZ++) {
            for (int rangeX = box.getMinX(); rangeX <= box.getMaxX(); rangeX++) {
                chunkMap.put(new ChunkPos(rangeX, rangeZ), null);
            }
        }
        chunkMap.keySet().parallelStream().forEach(chunkPos -> {
            WorldChunk chunk = serverLevel.getChunk(chunkPos.x, chunkPos.z);
            NbtCompound chunkNBT = ChunkSerializer.serialize(serverLevel, chunk);
            chunkMap.replace(chunkPos, chunkNBT);
        });
        NbtList chunkList = new NbtList();
        chunkList.addAll(chunkMap.values());

        NbtCompound bodyNBT = new NbtCompound();
        bodyNBT.put("Chunks", chunkList);
        bodyNBT.putInt("ChunkX", box.getMinX());
        bodyNBT.putInt("ChunkZ", box.getMinZ());
        bodyNBT.putInt("ChunkDX", (box.getMaxX() - box.getMinX()) + 1);
        bodyNBT.putInt("ChunkDZ", (box.getMaxZ() - box.getMinZ()) + 1);

        // Response header and response body
        Headers responseHeaders = httpExchange.getResponseHeaders();
        if (returnPlainText) {
            String responseString = bodyNBT.toString();

            setResponseHeadersContentTypePlain(responseHeaders);
            resolveRequest(httpExchange, responseString);
            return;
        }

        setResponseHeadersContentTypeBinary(responseHeaders, returnCompressed);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (returnCompressed) {
            GZIPOutputStream dos = new GZIPOutputStream(baos);
            NbtIo.writeCompressed(bodyNBT, dos);
            dos.flush();
            byte[] responseBytes = baos.toByteArray();

            resolveRequest(httpExchange, responseBytes);
            return;
        }
        DataOutputStream dos = new DataOutputStream(baos);
        NbtIo.write(bodyNBT, dos);
        dos.flush();
        byte[] responseBytes = baos.toByteArray();

        resolveRequest(httpExchange, responseBytes);
    }
}
