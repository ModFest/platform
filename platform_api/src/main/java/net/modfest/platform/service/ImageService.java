package net.modfest.platform.service;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import net.modfest.platform.configuration.PlatformConfig;
import net.modfest.platform.repository.ImageRepository;
import net.modfest.platform.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class ImageService {
	@Autowired
	private ImageRepository repository;
	@Autowired
	private PlatformConfig config;

	public void downloadSubmissionImage(String url, SubmissionRepository.SubmissionId subKey, SubmissionImageType type) {
		try {
			repository.download(new URI(url), getImageLocationKey(subKey, type));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Retrieves a public url for a given image. When running in a local dev environment, it uses
	 * the {@code request} parameter to look up the url this page was accessed from and determine
	 * a link to platform from there.
	 * In prod, {@link PlatformConfig#getImageCdnUrl()} will be set and the URL will be determined
	 * relative to that.
	 * @param request Used to help find a public url from which platform is accessible. Used only if there is no image CDN configured
	 * @param subKey The key to the submission we want to look up an image for
	 * @param type The type of image to retrieve.
	 * @return A url at which this image is available. May be {@code null} when running in dev without proper
	 * context. May also be null if the image does not exist.
	 */
	public @Nullable String getImageUrl(@Nullable HttpServletRequest request, SubmissionRepository.SubmissionId subKey, SubmissionImageType type) {
		var imageInfo = repository.getImageInfo(getImageLocationKey(subKey, type));
		if (imageInfo == null) {
			return null;
		}

		String imageCdn = config.getImageCdnUrl();
		if (imageCdn == null) {
			if(request == null) {
				return null;
			}

			// Platform will be acting as the cdn
			var baseUrl = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort();
			imageCdn = baseUrl+"/imagecdn/";
		}
		return imageCdn+getImageLocationKey(subKey, type)+"."+imageInfo.extension();
	}

	private String getImageLocationKey(SubmissionRepository.SubmissionId subKey, SubmissionImageType type) {
		return "submission/"+subKey.event()+"/"+subKey.id()+"/"+type.suffix;
	}

	public enum SubmissionImageType {
		ICON("icon"),
		SCREENSHOT("screenshot"),
		TEST("test"),
		CLAIM("claim"),
		BUILD("build");

		private final String suffix;

		SubmissionImageType(String suffix) {
			this.suffix = suffix;
		}
	}
}
