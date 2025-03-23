package net.modfest.platform.infra;

import net.modfest.platform.configuration.GitConfig;
import net.modfest.platform.git.GitRootPath;
import net.modfest.platform.git.ManagedDirectory;

import java.io.IOException;
import java.nio.file.Path;

public class Utils {
	public static ManagedDirectory getTestDir(Path dir) {
		try {
			return new GitRootPath(dir, new GitConfig("debug", "debug@example.org", null, null));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
