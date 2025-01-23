package net.modfest.platform.misc;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * The preferred way to interact with json. Ensures proper handling of encodings,
 * and ensures files are written to atomically.
 */
@Service
public class JsonUtil {
	@Autowired
	private Gson gson;

	/**
	 * Read json from a file.
	 */
	public <T> T readJson(Path path, Class<T> clazz) {
		try (var reader = new FileReader(path.toFile(), StandardCharsets.UTF_8)) {
			return gson.fromJson(reader, clazz);
		} catch (IOException e) {
			// We don't expect to get any io errors, and we don't really
			// have a good way of dealing with them. So we can better just promote them
			// to unchecked exceptions, since we'd be passing them up the chain any way
			// Maybe in the future we can assure that io errors are even harder errors,
			// and will shut the server down.
			throw new RuntimeException(e);
		}
	}

	/**
	 * Serializes an object to json and writes it to the target
	 * path. Serialization is done using gson. This method should be
	 * the preferred way of writing to files, as it uses an atomic write
	 *
	 * @see Gson#toJson(Object, Appendable)
	 * @param target The file that should be written to
	 * @param object The object to be serialized
	 */
	public void writeJson(Path target, Object object) {
		// Do an atomic write operation.
		// this means we'll write to a temporary file,
		// then copy that file atomically once done
		try {
			var tmp = tempFile(target);
			try (var writer = new FileWriter(tmp.toFile(), StandardCharsets.UTF_8)) {
				gson.toJson(object, writer);
			}
			Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			// Same reasoning as in #readJson
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gives us a temporary place to write to
	 */
	private Path tempFile(Path targetFile) {
		var hash = UUID.randomUUID().toString().substring(0,8);
		return targetFile.resolveSibling(targetFile.getFileName() + ".temp." + hash);
	}
}
