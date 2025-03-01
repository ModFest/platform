package net.modfest.platform.repository;

import jakarta.annotation.PostConstruct;
import lombok.Locked;
import net.modfest.platform.git.ManagedDirectory;
import net.modfest.platform.misc.EventSource;
import net.modfest.platform.misc.FileUtil;
import net.modfest.platform.misc.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A simple repository for storing data as json.
 * Stored data is identified by a string, and can be queried by that string.
 * Data will be stored under a subdirectory of the configured datadir.
 * This class is designed for low-volume, but easily auditable storage. Consider creating something else for
 * any purpose that needs more than about a thousand entries.
 *
 * @apiNote Please ensure {@link Data} is an immutable class. ({@link lombok.With} is an easy way to do mutation).
 * 			This ensures that the data inside the cache can't be modified without it being properly saved to disk.
 * @param <Data> The type of the data stored. This class should be immutable!!
 */
public abstract class AbstractJsonRepository<Data, Id> implements DiskCachedData {
	private JsonUtil jsonUtil;

	private final String name;
	private final Class<Data> clazz;
	private ManagedDirectory root;
	/**
	 * This is our in-memory storage for quick lookups.
	 * It should be kept in-sync with the filesystem
	 */
	private Map<Id, Data> store = new HashMap<>();
	/**
	 * Lock used both for writing to the filesystem and to manage {@link #store}.
	 */
	private final ReadWriteLock dataLock = new ReentrantReadWriteLock();

	public final EventSource<Id> onDataUpdated = new EventSource<>();

	public AbstractJsonRepository(@Autowired JsonUtil jsonUtil, ManagedDirectory root, String name, Class<Data> clazz) {
		this.jsonUtil = jsonUtil;
		this.root = root;
		this.name = name;
		this.clazz = clazz;
	}

	@PostConstruct
	private void init() {
		// Initialize storage
		readFromFilesystem();
	}

	@Override
	@Locked.Write("dataLock") // Write lock, because we're writing to our internal store
	public void readFromFilesystem() {
		// Ensure the folder is created
		try {
			this.root.createDirectories();

			var prevData = this.store;
			this.store = new HashMap<>();

			this.root.read(p -> {
				FileUtil.iterDir(p, file -> {
					Data data = this.jsonUtil.readJson(file, this.clazz);

					if (this.store.containsKey(getId(data))) {
						throw new RuntimeException("Duplicate id in " + this.name + " repository! '" + getId(data) + "' appeared twice! Please resolve this manually");
					}

					var expectedPath = p.resolve(getLocation(data));
					if (!Objects.equals(file.toAbsolutePath(), expectedPath.toAbsolutePath())) {
						throw new RuntimeException("Data is in the wrong location. Expected " + p.relativize(expectedPath) + " but found " + p.relativize(file));
					}

					this.store.put(getId(data), data);
				});
			});

			// Check for any changes and notify listeners of them
			if (prevData != null) {
				this.store.forEach((key, value) -> {
					if (!Objects.equals(value, prevData.get(key))) {
						onDataUpdated.emit(key);
					}
				});
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Locked.Write("dataLock")
	public void save(@NonNull Data newData) {
		// Run validations
		var prev = this.store.get(getId(newData));
		validateEdit(prev, newData);

		// Write data to json file first
		this.root.writePerformant((p, logger) -> {
			var newLocation = p.resolve(getLocation(newData));
			this.jsonUtil.writeJson(newLocation, newData);
			logger.logWrite(newLocation);
		});

		// Keep our in-memory storage up to date
		this.store.put(getId(newData), newData);

		onDataUpdated.emit(getId(newData));
	}

	@Locked.Write("dataLock")
	public void delete(@NonNull Id id) {
		var data = this.get(id);
		this.root.write(p -> {
			try {
				Files.delete(
					p.resolve(getLocation(data))
				);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		this.store.remove(id);
		onDataUpdated.emit(id);
	}

	@Locked.Read("dataLock")
	@Nullable
	public Data get(@NonNull Id id) {
		return this.store.get(id);
	}

	@Locked.Read("dataLock")
	public boolean contains(@NonNull Id id) {
		return this.store.containsKey(id);
	}

	@Locked.Read("dataLock")
	@NonNull
	public Collection<Data> getAll() {
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
	abstract protected void validateEdit(@Nullable Data previous, @NonNull Data current) throws ConstraintViolationException;

	abstract protected Id getId(Data data);

	abstract protected Path getLocation(Data data);
}
