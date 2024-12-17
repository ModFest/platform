package net.modfest.platform.git;

import net.modfest.platform.IOConsumer;
import net.modfest.platform.IOFunction;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents a file or directory which is managed.
 * All writes/reads to a managed directory are logged.
 */
public interface ManagedPath {
	void write(IOConsumer<Path> runner) throws IOException;
	void read(IOConsumer<Path> runner) throws IOException;
	<R> R withRead(IOFunction<Path, R> runner) throws IOException;
}
