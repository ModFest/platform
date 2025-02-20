package net.modfest.platform.migrations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class MigratorUtils {
	/**
	 * Utility to execute a function on each file in a folder. Children are found using
	 * {@link Files#newDirectoryStream(Path)}. This function calculates the list of files ahead of time,
	 * such that any file operations done by the edit function won't mess things up
	 */
	public static void executeForAllFiles(Path p, PathConsumer editFunction) {
		try {
			if (!Files.exists(p)) {
				// Nothing exists, nothing to iterate
				return;
			}
			var pathList = new ArrayList<Path>();
			try (var files = Files.newDirectoryStream(p)) {
				for (var file : files) {
					pathList.add(file);
				}
			}

			for (var path : pathList) {
				editFunction.accept(path);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public interface PathConsumer {
		void accept(Path p) throws IOException;
	}
}
