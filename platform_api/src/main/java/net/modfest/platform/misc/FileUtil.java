package net.modfest.platform.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Consumer;

public class FileUtil {
	/**
	 * Runs the provided consumer for each file in the directory given by dir. Works recursively
	 */
	public static void iterDir(Path dir, Consumer<Path> consumer) {
		// Store in an array first, to avoid concurrent mutation shenanigans
		var paths = new ArrayList<Path>();

		try (var walker = Files.walk(dir)) {
			walker.forEach(f -> {
				if (!Files.isDirectory(f)) {
					paths.add(f);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		for (var path : paths) {
			consumer.accept(path);
		}
	}
}
