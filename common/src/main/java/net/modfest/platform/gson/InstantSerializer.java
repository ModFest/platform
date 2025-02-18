package net.modfest.platform.gson;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;

public class InstantSerializer implements JsonDeserializer<Instant>, JsonSerializer<Instant> {
	@Override
	public Instant deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		// Instant.parse uses ISO-8601 format
		return Instant.parse(jsonElement.getAsString());
	}

	@Override
	public JsonElement serialize(Instant instant, Type type, JsonSerializationContext jsonSerializationContext) {
		// Instant.toString uses ISO-8601 format
		return new JsonPrimitive(instant.toString());
	}
}
