package net.modfest.platform.controller;

import net.modfest.platform.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;

// This controller will only be active if no image cdn was specified.
// In prod, you should specify an image cdn
@Conditional(ImageCdn.ImageCdnCondition.class)
@RestController
public class ImageCdn {
	@Autowired
	private ImageRepository repository;

	@GetMapping(value = "/imageCdn/{*path}")
	public ResponseEntity<FileSystemResource> getImage(@PathVariable String path) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		var basePath = repository.getRootPathUnsafe();
		var resolvedPath = basePath.resolve(path).toAbsolutePath();
		if (!resolvedPath.startsWith(basePath)) {
			// Path traversal attack >:(
			throw new IllegalArgumentException("You do not have permission to access this file");
		}

		if (!Files.exists(resolvedPath)) {
			return ResponseEntity.notFound().build();
		}

		var response = ResponseEntity.ok();
		if (path.endsWith("png")) {
			response.contentType(MediaType.IMAGE_PNG);
		} else if (path.endsWith("jpg")) {
			response.contentType(MediaType.IMAGE_JPEG);
		} else if (path.endsWith("gif")) {
			response.contentType(MediaType.IMAGE_GIF);
		} else if (path.endsWith("webp")) {
			response.contentType(MediaType.parseMediaType("image/webp"));
		} else {
			response.contentType(MediaType.APPLICATION_OCTET_STREAM);
		}
		return response.body(new FileSystemResource(resolvedPath));
	}

	static class ImageCdnCondition implements Condition {
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return !context.getEnvironment().containsProperty("platform.imagecdnurl");
		}
	}
}
