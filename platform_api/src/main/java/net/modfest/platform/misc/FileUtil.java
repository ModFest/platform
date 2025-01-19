package net.modfest.platform.misc;

import net.modfest.platform.IOConsumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class FileUtil {
	/**
	 * Runs the provided consumer for each file in the directory given by dir. Works recursively
	 */
	public static void iterDir(Path dir, IOConsumer<Path> consumer) throws IOException {
		// Store in an array first, to avoid concurrent mutation shenanigans
		var paths = new ArrayList<Path>();

		try (var walker = Files.walk(dir)) {
			walker.forEach(f -> {
				if (!Files.isDirectory(f)) {
					paths.add(f);
				}
			});
		}

		for (var path : paths) {
			consumer.accept(path);
		}
	}
}
