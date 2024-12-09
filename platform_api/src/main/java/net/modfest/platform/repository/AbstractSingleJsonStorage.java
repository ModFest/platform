package net.modfest.platform.repository;

import jakarta.annotation.PostConstruct;
import lombok.Locked;
import net.modfest.platform.configuration.PlatformConfig;
import net.modfest.platform.misc.JsonUtil;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A simple repository that stores a single bit of information.
 * @apiNote Please ensure {@link T} is an immutable class. ({@link lombok.With} is an easy way to do mutation).
 * 			This ensures that the data inside the cache can't be modified without it being properly saved to disk.
 * @param <T> The type of the data. Should be immutable!!
 */
public abstract class AbstractSingleJsonStorage<T> implements DiskCachedData {
	@Autowired
	private PlatformConfig platformConfig;
	@Autowired
	private JsonUtil jsonUtil;

	private final String name;
	private final Class<T> clazz;
	private Path file;
	/**
	 * Lock used both for writing to the filesystem and to manage {@link #cache}.
	 */
	private final ReadWriteLock dataLock = new ReentrantReadWriteLock();

	private T cache;

	/**
	 * @param fileName Filename where this data should be stored. Will be relative to
	 * 				   {@link PlatformConfig#getDatadir()}. This should include the extension
	 * 				   of the file. Example: "myfile.json"
	 * @param clazz The class of the data stored
	 */
	protected AbstractSingleJsonStorage(String fileName, Class<T> clazz) {
		this.name = fileName;
		this.clazz = clazz;
	}

	@PostConstruct
	private void init() throws IOException {
		this.file = platformConfig.getDatadir().resolve(name);
		// Initialize storage
		readFromFilesystem();
	}

	// Write lock, because we're writing to our internal store, and we might be writing if
	// the data is not initialized yet
	@Locked.Write("dataLock")
	@Override
	public void readFromFilesystem() throws IOException {
		if (!Files.exists(this.file)) {
			Files.createDirectories(this.file.getParent());
			var defaultData = this.createDefault();
			if (defaultData == null) {
				throw new RuntimeException(new NullPointerException("Default data is null"));
			}
			writeToFile(defaultData);
		}

		this.cache = this.jsonUtil.readJson(this.file, this.clazz);
		if (this.cache == null) {
			throw new RuntimeException(this.file+" appears broken! Please fix, or delete it and let it regenerate");
		}
	}

	private void writeToFile(T data) throws IOException {
		this.jsonUtil.writeJson(this.file, data);
	}

	@Locked.Read("dataLock")
	@NonNull
	public T get() {
		if (this.cache == null) {
			throw new IllegalStateException("Tried to access data that does not yet exist");
		}
		return this.cache;
	}

	@Locked.Write("dataLock")
	public void save(@NonNull T data) throws IOException {
		writeToFile(data);
		this.cache = data;
	}

	@NonNull
	protected abstract T createDefault();
}
