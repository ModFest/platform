package net.modfest.platform.service;

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

	public String getImageUrl(HttpServletRequest request, SubmissionRepository.SubmissionId subKey, SubmissionImageType type) {
		var imageInfo = repository.getImageInfo(getImageLocationKey(subKey, type));
		if (imageInfo == null) {
			return null;
		}

		String imageCdn = config.getImageCdnUrl();
		if (imageCdn == null) {
			// Platform will be acting as the cdn
			var baseUrl = request.getRequestURL().toString();
			if (baseUrl.endsWith("/")) {
				baseUrl = baseUrl.substring(0, baseUrl.length()-1);
			}
			imageCdn = baseUrl+"/imagecdn";
		}
		return imageCdn+getImageLocationKey(subKey, type)+"."+imageInfo.extension();
	}

	private String getImageLocationKey(SubmissionRepository.SubmissionId subKey, SubmissionImageType type) {
		return "submission/"+subKey.event()+"/"+subKey.id()+"/"+type.suffix;
	}

	public enum SubmissionImageType {
		ICON("icon"),
		SCREENSHOT("screenshot");
		private final String suffix;

		SubmissionImageType(String suffix) {
			this.suffix = suffix;
		}
	}
}
