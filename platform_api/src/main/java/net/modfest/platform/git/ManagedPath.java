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
	/**
	 * Serves the same function as {@link #write(Consumer)} but avoids having to diff the entire
	 * folder. In order for this to work you MUST register/log all paths which you've edited
	 */
	void writePerformant(PerformantWriter runner);
	void read(Consumer<Path> runner);
	<R> R withRead(Function<Path, R> runner);
	GitScope getCurrentScope();
	void runWithScope(GitScope scope, Runnable r);

	@FunctionalInterface
	interface PerformantWriter {
		void doWrite(Path pathToWriteTo, WriteLogger logger);
	}

	interface WriteLogger {
		/**
		 * @param path The path that was written to
		 */
		void logWrite(Path path);
	}
}
