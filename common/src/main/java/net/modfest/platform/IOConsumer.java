package net.modfest.platform;

import java.io.IOException;

/**
 * Like {@link java.util.function.Consumer}, but throws io exceptions.
 */
@FunctionalInterface
public interface IOConsumer<T> {
	void accept(T t) throws IOException;
}
