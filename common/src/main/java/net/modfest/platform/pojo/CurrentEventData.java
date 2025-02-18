package net.modfest.platform.pojo;

import org.jspecify.annotations.Nullable;

/**
 * Stores information about the current event.
 * This is used by, for example, certain discord commands that apply only to the current event.
 * @param event the id of the current event
 */
public record CurrentEventData(@Nullable String event) {
}
