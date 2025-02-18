package net.modfest.platform.git;

import java.io.IOException;

public interface ManagedFile extends ManagedPath {
	void createParentDirectories() throws IOException;
}
