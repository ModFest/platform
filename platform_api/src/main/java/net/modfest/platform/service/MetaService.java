package net.modfest.platform.service;

import net.modfest.platform.repository.DiskCachedData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Defines some operations that affect platform itself, instead of its data
 */
@Service
public class MetaService {
	@Autowired
	private List<DiskCachedData> diskBasedRepositories;

	/**
	 * Invalidates the in-memory cache from all {@link net.modfest.platform.repository.DiskCachedData} objects
	 * known to Spring
	 */
	public void reloadFromDisk() throws IOException {
		for (DiskCachedData diskBasedRepository : diskBasedRepositories) {
			diskBasedRepository.readFromFilesystem();
		}
	}
}
