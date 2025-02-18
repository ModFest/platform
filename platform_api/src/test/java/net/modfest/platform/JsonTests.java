package net.modfest.platform;

import com.google.gson.GsonBuilder;
import net.modfest.platform.gson.GsonCommon;
import net.modfest.platform.pojo.EventData;
import net.modfest.platform.pojo.SubmissionData;
import net.modfest.platform.pojo.UserData;
import net.modfest.platform.pojo.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public class JsonTests {
	@ParameterizedTest
	@MethodSource("testObjects")
	public void roundtripJson(Object serializableObject) {
		var gsonBuilder = new GsonBuilder();
		GsonCommon.configureGson(gsonBuilder);
		var gson = gsonBuilder.create();

		var jsonString = gson.toJson(serializableObject);
		var reserializedObject = gson.fromJson(jsonString, serializableObject.getClass());

		Assertions.assertEquals(serializableObject, reserializedObject, "Object should remain the same after serializing");
	}

	public static List<Object> testObjects() {
		return List.of(
			new UserData(
				"123456",
				"Bob",
				"Bob",
				"he/she/it/they/xe/xem",
				"abcd",
				"1234",
				"Definitely a real person",
				"https://i.imgflip.com/4awf2i.jpg",
				Set.of("Badge"),
				Set.of("bc2+5i"),
				UserRole.TEAM_MEMBER
			),
			new EventData(
				"bc2+5i",
				"Blanketcon 2+5i",
				"The elusive blanketcon that definitely exists",
				EventData.Phase.PLANNING,
				List.of(
					new EventData.DateRange("Announced", "wefefwuifhui", EventData.Phase.PLANNING, Instant.EPOCH, Instant.EPOCH)
				),
				new EventData.Images(
					"a", "b", "c", "d"
				),
				new EventData.Colors(
					"a", "b"
				),
				new EventData.DiscordRoles(
					"a", "b"
				),
				"optifine",
				"1.34",
				"boop",
				List.of(
					new EventData.DescriptionItem<>(new EventData.DescriptionItem.Markdown("# HELLO"))
				)
			),
			new SubmissionData(
				"a",
				"b",
				"a b",
				"dawda",
				Set.of("Bob"),
				new SubmissionData.FileData(new SubmissionData.FileData.Modrinth(
					"dwaodi",
					"dwdadw"
				)),
				new SubmissionData.Images(
					"e",
					"b"
				),
				"https://a",
				"bb",
				new SubmissionData.Awards(
					Set.of(),
					Set.of()
				)
			)
		);
	}
}
