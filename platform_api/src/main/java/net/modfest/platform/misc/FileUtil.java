package net.modfest.platform.misc;

import net.modfest.platform.IOConsumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtil {
	/**
	 * Runs the provided consumer for each file in the directory given by dir.
	 * Uses {@link Files#newDirectoryStream(Path)} internally
	 */
	public static void iterDir(Path dir, IOConsumer<Path> consumer) throws IOException {
		try (var stream = Files.newDirectoryStream(dir)) {
			for (var file : stream) {
				consumer.accept(file);
			}
		}
	}
}
