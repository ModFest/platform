package net.modfest.platform.repository;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import net.modfest.platform.git.GitScope;
import net.modfest.platform.git.ManagedDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Repository
@Scope("singleton")
public class ImageRepository {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageRepository.class);
	private final ManagedDirectory imageStore;
	private final HttpClient client;
	private static final Set<String> supportedExtensions = Set.of(
		"png",
		"jpg",
		"webp",
		"gif"
	);

	public ImageRepository(@Qualifier("datadir") ManagedDirectory datadir) {
		this.imageStore = datadir.getSubDirectory("images");
		this.client = HttpClient.newBuilder().build();
	}

	@PostConstruct
	private void init() {
		try {
			this.imageStore.createDirectories();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the root path. This provides no guarantees for atomicity.
	 */
	public Path getRootPathUnsafe() {
		return imageStore.withRead(r -> r);
	}

	public @Nullable ImageInfo getImageInfo(String id) {
		return this.imageStore.withRead(path -> {
			for (var ext : supportedExtensions) {
				if (Files.exists(path.resolve(id+"."+ext))) {
					return new ImageInfo(ext);
				}
			}
			// File not found
			return null;
		});
	}

	public void download(URI url, String id) {
		var p = url.getPath();
		var extensionTmp = last(p.split("\\."));
		if (extensionTmp.equals("jpeg")) {
			extensionTmp = "jpg";
		}

		if (!supportedExtensions.contains(extensionTmp)) {
			throw new IllegalArgumentException("Invalid filetype "+extensionTmp);
		}

		// Since we're doing async operations
		var originalScope = this.imageStore.getCurrentScope();
		var extension = extensionTmp;

		client.sendAsync(
			HttpRequest.newBuilder(url).GET().build(),
			HttpResponse.BodyHandlers.ofByteArray()
		).thenAccept(response -> {
			if (response.statusCode() != 200) {
				LOGGER.error("Failed to store image {}. Got status code {}", url, response.statusCode());
			} else {
				String originalSha = null;
				try {
					originalSha = originalScope.commitSha();
				} catch (TimeoutException e) {
					LOGGER.warn("Image request can't be attributed to original commit", e);
				}
				var gitScope = originalSha == null ? originalScope : new GitScope("Writing image for #"+originalSha);
				this.imageStore.runWithScope(gitScope, () -> {
					this.imageStore.writePerformant((path, logger) -> {
						var writePath = path.resolve(id+"."+extension);
						try {
							Files.createDirectories(writePath.getParent());
							Files.write(writePath, response.body(), StandardOpenOption.CREATE);
							logger.logWrite(writePath);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
				});
			}
		}).exceptionally(t -> {
			LOGGER.error("Error storing image from {}", url, t);
			return null;
		});
	}

	private static <T> T last(T[] d) {
		return d[d.length-1];
	}

	public record ImageInfo(String extension) {}
}
