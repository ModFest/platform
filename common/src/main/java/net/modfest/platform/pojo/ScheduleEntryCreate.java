package net.modfest.platform.pojo;

import java.time.Instant;
import java.util.List;

/**
 * Used for creating / updating schedule entries
 */
public record ScheduleEntryCreate(
	String event,
	String title,
	String type,
	String location,
	String description,
	List<String> authors,
	Instant start,
	Instant end) {
}
