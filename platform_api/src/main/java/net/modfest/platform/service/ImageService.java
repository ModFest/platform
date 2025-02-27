package net.modfest.platform.service;

import net.modfest.platform.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class ImageService {
	@Autowired
	private ImageRepository repository;

	public void downloadSubmissionImage(String url, String event, String subId, SubmissionImageType type) {
		try {
			repository.download(new URI(url), "submission/"+event+"/"+subId+"-"+type.suffix);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
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
