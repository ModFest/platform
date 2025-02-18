package net.modfest.platform;

import java.io.IOException;

/**
 * Like {@link java.util.function.Function}, but throws io exceptions.
 */
@FunctionalInterface
public interface IOFunction<T,R> {
	R apply(T v) throws IOException;
}
