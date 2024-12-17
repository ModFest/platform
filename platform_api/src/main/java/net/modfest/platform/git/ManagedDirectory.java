package net.modfest.platform.git;

import org.jspecify.annotations.NonNull;

import java.io.IOException;

public interface ManagedDirectory extends ManagedPath {
	@NonNull
	ManagedDirectory getSubDirectory(String name);
	@NonNull
	ManagedFile getSubFile(String name);


	void createDirectories() throws IOException;
}
