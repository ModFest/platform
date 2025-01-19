package net.modfest.platform.repository;

import jakarta.annotation.PostConstruct;
import lombok.Locked;
import net.modfest.platform.configuration.PlatformConfig;
import net.modfest.platform.git.ManagedDirectory;
import net.modfest.platform.misc.FileUtil;
import net.modfest.platform.misc.JsonUtil;
import net.modfest.platform.pojo.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A simple repository for storing data as json.
 * Stored data is identified by a string, and can be queried by that string.
 * Data will be stored under a subdirectory of the configured {@link PlatformConfig#getDatadir()}.
 * This class is designed for low-volume, but easily auditable storage. Consider creating something else for
 * any purpose that needs more than about a thousand entries.
 *
 * @apiNote Please ensure {@link T} is an immutable class. ({@link lombok.With} is an easy way to do mutation).
 * 			This ensures that the data inside the cache can't be modified without it being properly saved to disk.
 * @param <T> The type of the data stored. This class should be immutable!!
 */
public abstract class AbstractJsonRepository<T extends Data> implements DiskCachedData {
	private JsonUtil jsonUtil;

	private final String name;
	private final Class<T> clazz;
	private ManagedDirectory root;
	/**
	 * This is our in-memory storage for quick lookups.
	 * It should be kept in-sync with the filesystem
	 */
	private final Map<String, T> store = new HashMap<>();
	/**
	 * Lock used both for writing to the filesystem and to manage {@link #store}.
	 */
	private final ReadWriteLock dataLock = new ReentrantReadWriteLock();

	public AbstractJsonRepository(@Autowired JsonUtil jsonUtil, ManagedDirectory root, String name, Class<T> clazz) {
		this.jsonUtil = jsonUtil;
		this.root = root;
		this.name = name;
		this.clazz = clazz;
	}

	@PostConstruct
	private void init() throws IOException {
		// Initialize storage
		readFromFilesystem();
	}

	@Override
	@Locked.Write("dataLock") // Write lock, because we're writing to our internal store
	public void readFromFilesystem() throws IOException {
		// Ensure the folder is created
		this.root.createDirectories();

		this.store.clear();

		this.root.read(p -> {
			FileUtil.iterDir(p, file -> {
				T data = this.jsonUtil.readJson(file, this.clazz);
				if (this.store.containsKey(data.id())) {
					throw new RuntimeException("Duplicate id in "+this.name+" repository! '"+data.id()+"' appeared twice! Please resolve this manually");
				}
				this.store.put(data.id(), data);
			});
		});
	}

	@Locked.Write("dataLock")
	public void save(@NonNull T newData) throws IOException {
		// Run validations
		validateId(newData.id());
		var prev = this.store.get(newData.id());
		validateEdit(prev, newData);

		// Write data to json file first
		this.root.write(p -> {
			this.jsonUtil.writeJson(p.resolve(newData.id()+".json"), newData);
		});

		// Keep our in-memory storage up to date
		this.store.put(newData.id(), newData);
	}

	@Locked.Read("dataLock")
	@Nullable
	public T get(@NonNull String id) {
		return this.store.get(id);
	}

	@Locked.Read("dataLock")
	public boolean contains(@NonNull String id) {
		return this.store.containsKey(id);
	}

	@Locked.Read("dataLock")
	@NonNull
	public Collection<T> getAll() {
		return this.store.values();
	}

	private void validateId(@NonNull String id) throws ConstraintViolationException {
		if (id.contains("/")) {
			throw new ConstraintViolationException("Illegal character '/' in id: '"+id+"'");
		}
	}

	/**
	 * Called whenever a database entry is edited. This should be overwritten
	 * to ensure integrity of database constraints.
	 * @param previous The previous value of the database entry (might be null if the entry is newly created)
	 * @param current The new value of the database entry   
	 */
	abstract protected void validateEdit(@Nullable T previous, @NonNull T current) throws ConstraintViolationException;
}
