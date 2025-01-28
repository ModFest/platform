package net.modfest.platform.gson;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import net.modfest.platform.pojo.EventData;

import java.time.Instant;

public class GsonCommon {
	/**
	 * @return a gson instance configured to work with platform's java objects
	 */
	public static void configureGson(GsonBuilder builder) {
		builder
			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
			.registerTypeHierarchyAdapter(Enum.class, new EnumToLowerCaseJsonConverter())
			.registerTypeAdapter(Instant.class, new InstantSerializer())
			.registerTypeAdapter(EventData.DescriptionItem.class, new EventData.DescriptionItem.TypeAdapter())
			.setPrettyPrinting()
			.serializeNulls()
			.setLenient();
	}
}
