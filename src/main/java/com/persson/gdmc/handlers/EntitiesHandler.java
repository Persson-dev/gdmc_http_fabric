package com.persson.gdmc.handlers;

import com.google.gson.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.persson.gdmc.utils.TagUtils;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EntitiesHandler extends HandlerBase {

	// PUT/GET: x, y, z positions
	private int x;
	private int y;
	private int z;

	// GET: Ranges in the x, y, z directions (can be negative). Defaults to 1.
	private int dx;
	private int dy;
	private int dz;

	// GET: Search entities using a Target Selector
	// (https://minecraft.wiki/w/Target_selectors).
	// Defaults to "@e[x=x,y=y,z=z,dx=dx,dy=dy,dz=dz]" (find all entities within
	// area).
	private String entitySelectorString;

	// GET: Whether to include entity data
	// https://minecraft.wiki/w/Entity_format#Entity_Format
	private boolean includeData;

	// GET/PUT/DELETE/PATCH: Search for entities within a specific dimension.
	// For GET requests this only works if selector string contains position
	// arguments.
	private String dimension;

	public EntitiesHandler(MinecraftServer mcServer) {
		super(mcServer);
	}

	@Override
	protected void internalHandle(HttpExchange httpExchange) throws IOException {

		// query parameters
		Map<String, String> queryParams = parseQueryString(httpExchange.getRequestURI().getRawQuery());

		try {
			x = Integer.parseInt(queryParams.getOrDefault("x", "0"));
			y = Integer.parseInt(queryParams.getOrDefault("y", "0"));
			z = Integer.parseInt(queryParams.getOrDefault("z", "0"));

			dx = Integer.parseInt(queryParams.getOrDefault("dx", "1"));
			dy = Integer.parseInt(queryParams.getOrDefault("dy", "1"));
			dz = Integer.parseInt(queryParams.getOrDefault("dz", "1"));

			entitySelectorString = queryParams.getOrDefault("selector", null);

			includeData = Boolean.parseBoolean(queryParams.getOrDefault("includeData", "false"));

			dimension = queryParams.getOrDefault("dimension", null);
		} catch (NumberFormatException e) {
			throw new HttpException("Could not parse query parameter: " + e.getMessage(), 400);
		}

		String method = httpExchange.getRequestMethod().toLowerCase();

		JsonArray response;

		switch (method) {
			case "put" -> {
				response = putEntitiesHandler(httpExchange.getRequestBody());
			}
			case "get" -> {
				response = getEntitiesHandler();
			}
			case "delete" -> {
				response = deleteEntitiesHandler(httpExchange.getRequestBody());
			}
			case "patch" -> {
				response = patchEntitiesHandler(httpExchange.getRequestBody());
			}
			default -> {
				throw new HttpException("Method not allowed. Only PUT, GET, DELETE and PATCH requests are supported.",
						405);
			}
		}

		// Response headers
		Headers responseHeaders = httpExchange.getResponseHeaders();
		setDefaultResponseHeaders(responseHeaders);

		resolveRequest(httpExchange, response.toString());
	}

	/**
	 * @param requestBody request body of entity summon instructions
	 * @return summon results
	 */
	private JsonArray putEntitiesHandler(InputStream requestBody) {
		ServerCommandSource cmdSrc = createCommandSource("GDMC-EntitiesHandler", dimension, new Vec3d(x, y, z), null);
		ServerWorld serverLevel = getServerLevel(dimension);

		JsonArray returnValues = new JsonArray();
		JsonArray entityDescriptionList = parseJsonArray(requestBody);

		for (JsonElement entityDescription : entityDescriptionList) {
			SummonEntityInstruction summonEntityInstruction;
			try {
				JsonObject json = entityDescription.getAsJsonObject();
				summonEntityInstruction = new SummonEntityInstruction(json, cmdSrc);
			} catch (UnsupportedOperationException | IllegalStateException | CommandSyntaxException e) {
				returnValues.add(instructionStatus(false, e.getMessage()));
				continue;
			}
			returnValues.add(summonEntityInstruction.summon(serverLevel));
		}

		return returnValues;
	}

	/**
	 * @return list of entity information
	 */
	private JsonArray getEntitiesHandler() {

		StringReader entitySelectorStringReader = new StringReader(
				entitySelectorString != null && !entitySelectorString.isBlank() ? entitySelectorString
						: "@e[x=%s,y=%s,z=%s,dx=%s,dy=%s,dz=%s]".formatted(x, y, z, dx, dy, dz));
		try {
			EntitySelector entitySelector = EntityArgumentType.entities().parse(entitySelectorStringReader);
			ServerCommandSource cmdSrc = createCommandSource("GDMC-EntitiesHandler", dimension, new Vec3d(x, y, z), null);
			List<? extends Entity> entityList = entitySelector.getEntities(cmdSrc);

			JsonArray returnList = new JsonArray();
			for (Entity entity : entityList) {
				EntityType<?> entityType = entity.getType();
				Identifier identifier = EntityType.getId(entityType);
				String entityId =  entityType.isSaveable() && identifier != null ? identifier.toString() : null;
				if (entityId == null) {
					continue;
				}
				JsonObject json = new JsonObject();
				json.addProperty("uuid", entity.getUuidAsString());
				if (includeData) {
					NbtCompound entityNbt = new NbtCompound();
					entity.writeNbt(entityNbt);
					json.addProperty("data", entityNbt.asString());
				}
				returnList.add(json);
			}
			return returnList;
		} catch (CommandSyntaxException e) {
			throw new HttpException("Malformed entity target selector: " + e.getMessage(), 400);
		}

	}

	/**
	 * @param requestBody request body of entity removal instructions
	 * @return entity removal results
	 */
	private JsonArray deleteEntitiesHandler(InputStream requestBody) {

		ServerWorld level = getServerLevel(dimension);

		JsonArray returnValues = new JsonArray();

		JsonArray jsonListUUID = parseJsonArray(requestBody);

		for (JsonElement jsonElement : jsonListUUID) {
			String stringUUID;
			try {
				stringUUID = jsonElement.getAsString();
			} catch (UnsupportedOperationException | IllegalStateException e) {
				returnValues.add(instructionStatus(false, "Invalid UUID"));
				continue;
			}
			if (stringUUID.isBlank()) {
				returnValues.add(instructionStatus(false, "Invalid UUID"));
				continue;
			}
			Entity entityToBeRemoved;
			try {
				entityToBeRemoved = level.getEntity(UUID.fromString(stringUUID));
			} catch (IllegalArgumentException e) {
				returnValues.add(instructionStatus(false, e.getMessage()));
				continue;
			}

			if (entityToBeRemoved != null) {
				if (entityToBeRemoved.isRemoved()) {
					returnValues.add(instructionStatus(false));
				} else {
					entityToBeRemoved.remove(Entity.RemovalReason.DISCARDED);
					returnValues.add(instructionStatus(true));
				}
				continue;
			}
			returnValues.add(instructionStatus(false));
		}
		return returnValues;
	}

	/**
	 * @param requestBody request body of entity patch instructions
	 * @return entity patch status results
	 */
	private JsonArray patchEntitiesHandler(InputStream requestBody) {

		ServerWorld level = getServerLevel(dimension);

		JsonArray returnValues = new JsonArray();

		JsonArray jsonList = parseJsonArray(requestBody);

		for (JsonElement entityDescription : jsonList) {
			PatchEntityInstruction patchEntityInstruction;
			try {
				JsonObject json = entityDescription.getAsJsonObject();
				patchEntityInstruction = new PatchEntityInstruction(json);
			} catch (IllegalStateException | UnsupportedOperationException | NullPointerException
					| IllegalArgumentException | CommandSyntaxException e) {
				returnValues.add(instructionStatus(false, e.getMessage()));
				continue;
			}
			try {
				if (patchEntityInstruction.applyPatch(level)) {
					returnValues.add(instructionStatus(true));
					continue;
				}
			} catch (CrashException e) {
				returnValues.add(instructionStatus(false, e.getMessage()));
				continue;
			}
			returnValues.add(instructionStatus(false));
		}

		return returnValues;
	}

	private final static class SummonEntityInstruction {

		private final Identifier entityResourceLocation;
		private final Vec3d entityPosition;
		private NbtCompound entityData;

		SummonEntityInstruction(JsonObject summonInstructionInput, ServerCommandSource commandSourceStack)
				throws CommandSyntaxException, IllegalStateException, UnsupportedOperationException {
			String positionArgumentString = "";
			if (summonInstructionInput.has("x") && summonInstructionInput.has("y") && summonInstructionInput.has("z")) {
				positionArgumentString = summonInstructionInput.get("x").getAsString() + " "
						+ summonInstructionInput.get("y").getAsString() + " "
						+ summonInstructionInput.get("z").getAsString();
			}
			String entityIDString = summonInstructionInput.has("id") ? summonInstructionInput.get("id").getAsString()
					: "";
			String entityDataString = summonInstructionInput.has("data")
					? summonInstructionInput.get("data").getAsString()
					: "";

			StringReader sr = new StringReader(positionArgumentString + " " + entityIDString + " " + entityDataString);

			entityPosition = Vec3ArgumentType.vec3().parse(sr).toAbsolutePos(commandSourceStack);
			sr.skip();

			entityResourceLocation = IdentifierArgumentType.identifier().parse(sr);
			sr.skip();

			try {
				entityDataString = sr.getRemaining();
				if (entityDataString.isBlank()) {
					entityDataString = "{}";
				}
				entityData = StringNbtReader.parse(entityDataString);
			} catch (StringIndexOutOfBoundsException e) {
				entityData = new NbtCompound();
			}

		}

		public JsonObject summon(ServerWorld level) {

			if (!level.isValidForSetBlock(new BlockPos(
					(int) entityPosition.x,
					(int) entityPosition.y,
					(int) entityPosition.z))) {
				return instructionStatus(false, "Position is not in spawnable bounds");
			}

			entityData.putString("id", entityResourceLocation.toString());

			Entity entity = EntityType.loadEntityWithPassengers(entityData, level, (_entity) -> {
				_entity.setPosition(entityPosition);
				return _entity;
			});
			if (entity == null) {
				return instructionStatus(false, "Entity could not be spawned");
			}
			entity.checkDespawn();
			if (entity.isRemoved()) {
				return instructionStatus(false,
						"Entity was removed right after spawn for reason: " + entity.getRemovalReason());
			}
			if (!level.spawnNewEntityAndPassengers(entity)) {
				return instructionStatus(false, "Entity with this UUID already exists");
			}
			return instructionStatus(true, entity.getUuidAsString());
		}

	}

	/**
	 * Model to encapsulate parsing of data patches, finding existing entities with
	 * that {@link UUID}, applying the patch and returning a success/fail status.
	 */
	private final static class PatchEntityInstruction {
		private final UUID uuid;
		private final NbtCompound patchData;

		PatchEntityInstruction(JsonObject inputData) throws IllegalArgumentException, CommandSyntaxException,
				UnsupportedOperationException, NullPointerException {
			uuid = UUID.fromString(inputData.get("uuid").getAsString());
			patchData = StringNbtReader.parse(inputData.get("data").getAsString());
		}

		public boolean applyPatch(ServerWorld level) throws CrashException {
			if (uuid == null) {
				return false;
			}
			Entity entity = level.getEntity(uuid);
			if (entity == null) {
				return false;
			}

			NbtCompound entityNbt = new NbtCompound();
			entity.saveNbt(entityNbt);

			NbtCompound patchedData = TagUtils.mergeTags(entityNbt, patchData);
			if (entityNbt.equals(patchedData)) {
				return false;
			}
			entity.readNbt(patchedData);
			return true;
		}
	}
}
