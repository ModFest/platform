package net.modfest.platform.repository;

import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import lombok.Locked;
import net.modfest.platform.configuration.GsonConfig;
import net.modfest.platform.configuration.PlatformConfig;
import net.modfest.platform.pojo.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A simple repository for storing data as json.
 * Stored data is identified by a string, and can be queried by that string.
 * Data will be stored under a subdirectory of the configured {@link PlatformConfig#getDatadir()}.
 * @param <T> The type of the data stored. This class should be immutable!!
 */
public abstract class AbstractJsonRepository<T extends Data> {
	@Autowired
	private PlatformConfig platformConfig;
	@Autowired
	private Gson gson;

	private final String name;
	private final Class<T> clazz;
	private Path root;
	/**
	 * This is our in-memory storage for quick lookups.
	 * It should be kept in-sync with the filesystem
	 */
	private final Map<String, T> store = new HashMap<>();
	/**
	 * Lock used both for writing to the filesystem and to manage {@link #store}.
	 */
	private final ReadWriteLock dataLock = new ReentrantReadWriteLock();

	public AbstractJsonRepository(String name, Class<T> clazz) {
		this.name = name;
		this.clazz = clazz;
	}

	@PostConstruct
	private void init() throws IOException {
		this.root = platformConfig.getDatadir().resolve(name);
		// Initialize storage
		readFromFilesystem();
	}

	@Locked.Write("dataLock") // Write lock, because we're writing to our internal store
	public void readFromFilesystem() throws IOException {
		if (!Files.exists(this.root)) {
			Files.createDirectories(this.root);
		}
		this.store.clear();
		try (var files = Files.newDirectoryStream(this.root)) {
			for (var file : files) {
				T data = this.gson.fromJson(new FileReader(file.toFile()), this.clazz);
				if (this.store.containsKey(data.id())) {
					throw new RuntimeException("Duplicate id in "+this.name+" repository! '"+data.id()+"' appeared twice! Please resolve this manually");
				}
				this.store.put(data.id(), data);
			}
		}
	}

	@Locked.Write("dataLock")
	public void save(T data) throws IOException {
		// Write data to json file first
		validateId(data.id());
		var file = this.root.resolve(data.id()+".json");
		this.gson.toJson(data, new FileWriter(file.toFile()));

		// Keep our in-memory storage up to date
		this.store.put(data.id(), data);
	}

	@Locked.Read("dataLock")
	public T get(String id) {
		return this.store.get(id);
	}

	@Locked.Read("dataLock")
	public Iterable<T> getAll() {
		return this.store.values();
	}

	private void validateId(String id) throws IllegalArgumentException {
		if (id.contains("/")) {
			throw new IllegalArgumentException("Illegal character '/' in id: '"+id+"'");
		}
	}
}
