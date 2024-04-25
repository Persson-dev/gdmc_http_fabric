package com.persson.gdmc.utils;

import com.google.gson.*;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.LowercaseEnumTypeAdapterFactory;
import net.minecraft.util.Util;

import java.lang.reflect.Type;

public class ChatComponentDataExtractor {

	private static final ExclusionStrategy exclusionStrategy = new ExclusionStrategy() {
		@Override
		public boolean shouldSkipField(FieldAttributes f) {
			return f.getName().equals("decomposedWith") ||
				f.getName().equals("decomposedParts") ||
				f.getName().equals("siblings") ||
				f.getName().equals("style") ||
				f.getName().equals("visualOrderText");
		}

		@Override
		public boolean shouldSkipClass(Class<?> clazz) {
			return false;
		}
	};

	private static final JsonSerializer<MutableText> serializer = new JsonSerializer<>() {
		@Override
		public JsonElement serialize(MutableText component, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject jsonObject = new JsonObject();

			extractValues(jsonObject, component);

			return jsonObject;
		}

		private static void extractValues(JsonObject targetJson, MutableText component) {
			TextContent componentContents = component.getContent();
			if (componentContents != TextContent.EMPTY && componentContents instanceof TranslatableTextContent) {
				TranslatableTextContent translatableContents = (TranslatableTextContent) componentContents;
				if (translatableContents.getArgs().length > 0) {
					JsonArray jsonArgsArray = new JsonArray();
					for (Object object : translatableContents.getArgs()) {
						if (object instanceof MutableText) {
							extractValues(targetJson, (MutableText) object);
							continue;
						}
						if (object instanceof Number) {
							jsonArgsArray.add((Number)object);
						} else if (object instanceof Boolean) {
							jsonArgsArray.add((Boolean)object);
						} else {
							jsonArgsArray.add(String.valueOf(object));
						}
					}
					if (!jsonArgsArray.isEmpty()) {
						targetJson.add(translatableContents.getKey(), jsonArgsArray);
					}
				}
			}
		}
	};

	private static final Gson CUSTOM_GSON = Util.make(() -> {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.disableHtmlEscaping();
		gsonBuilder.addSerializationExclusionStrategy(exclusionStrategy);
		gsonBuilder.registerTypeHierarchyAdapter(MutableText.class, serializer);
		gsonBuilder.registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory());
		return gsonBuilder.create();
	});

	public static JsonElement toJsonTree(Text component) {
		return CUSTOM_GSON.toJsonTree(component.copy());
	}
}
