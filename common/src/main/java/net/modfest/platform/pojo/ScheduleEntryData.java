package net.modfest.platform.pojo;

import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.List;

/**
 * An event (specifically blanketcons) might have a schedule with a bunch of panels, etc.
 * Each of those events will have an associated schedule data json object.
 * @param event the event that this schedule is for
 * @param id randomly generated id
 * @param type Example values: [{@code "panel"}, {@code "keynote"}, {@code "Q&A"}]
 * @param location For example: {@code "Main Stage"}
 */
public record ScheduleEntryData(
	@NonNull String id,
	@NonNull String event,
	@NonNull String title,
	@NonNull String type,
	@NonNull String location,
	@NonNull String description,
	@NonNull List<@NonNull String> authors,
	@NonNull Instant start,
	@NonNull Instant end
) {}
