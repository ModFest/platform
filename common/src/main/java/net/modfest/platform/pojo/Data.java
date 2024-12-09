package net.modfest.platform.pojo;

import org.jspecify.annotations.NonNull;

/**
 * Represents a piece of <em>immutable</em> data, which has a unique id
 */
public interface Data {
	@NonNull
	String id();
}
