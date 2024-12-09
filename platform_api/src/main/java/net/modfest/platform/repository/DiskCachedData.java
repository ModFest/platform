package net.modfest.platform.repository;

import java.io.IOException;

/**
 * Represents a repository that stores data in the filesystem,
 * but keeps an in-memory cache of that data instead of reading from disk on
 * every operation
 */
public interface DiskCachedData {
	/**
	 * Invalidates the in-memory cache and reads everything back from the filesystem.
	 * @implNote This method should do the proper synchronization to
	 * 			 ensure that reads have correct behaviour if they're done whilst
	 * 			 this method is being called. For example, you might lock the in-memory
	 * 			 database to prevent it from being queried whilst not all entries have been filled
	 * 			 yet.
	 */
	void readFromFilesystem() throws IOException;
}
