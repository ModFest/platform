package net.modfest.platform.git;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a file or directory which is managed.
 * All writes/reads to a managed directory are logged.
 */
public interface ManagedPath {
	void write(Consumer<Path> runner);
	void read(Consumer<Path> runner);
	<R> R withRead(Function<Path, R> runner);
	void runWithScope(GitScope scope, Runnable r);
}
