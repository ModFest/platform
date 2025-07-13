package net.modfest.platform.service;

import jakarta.annotation.Nullable;
import net.modfest.platform.pojo.SubmissionData;
import net.modfest.platform.pojo.SubmissionPatchData;
import net.modfest.platform.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import nl.theepicblock.dukerinth.models.Version;

import java.util.*;

@Service
public class WebhookService {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebhookService.class);

	@Value("${webhook.discord.url}")
	private String discordWebhookUrl;

	@Autowired
	private ImageService imageService;

	private final RestTemplate restTemplate = new RestTemplate();

	protected record Field(String name, String value) {
	}

	protected record Footer(String text, @Nullable String icon_url) {
	}

	protected record Image(String url) {
	}

	protected record Embed(String title, String description, int color, List<Field> fields, Footer footer,
						   Image thumbnail) {
	}

	protected record WebhookPayload(List<Embed> embeds) {
	}

	Embed embedForSubmission(SubmissionData data, String event, int eventColor) {
		return embedForSubmission(data, event, eventColor, null, null);
	}

	Embed embedForSubmission(SubmissionData data, String event, int eventColor, @Nullable List<Field> fields) {
		return embedForSubmission(data, event, eventColor, fields, null);
	}

	Embed embedForSubmission(SubmissionData data, String event, int eventColor, @Nullable List<Field> fields, @Nullable Footer footer) {
		return new Embed(
			data.name(),
			event,
			eventColor,
			fields,
			footer,
			getIcon(data)
		);
	}

	private @Nullable Image getIcon(SubmissionData data) {
		var subKey = new SubmissionRepository.SubmissionId(data.event(), data.id());
		var icon = imageService.getImageUrl(null, subKey, ImageService.SubmissionImageType.ICON);

		if (icon == null) {
			return null;
		}

		return new Image(icon);
	}

	protected void sendEmbed(Embed embed) {
		if (discordWebhookUrl == null) {
			return;
		}

		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		var payload = new WebhookPayload(List.of(embed));

		try {
			ResponseEntity<String> response = restTemplate.postForEntity(discordWebhookUrl, new HttpEntity<>(payload, headers), String.class);
			if (!response.getStatusCode().is2xxSuccessful()) {
				LOGGER.error("Failed to send Discord embed: {}", response.getStatusCode());
			}
		} catch (Exception e) {
			LOGGER.error("Error sending Discord webhook: {}", e.getMessage());
		}
	}


	private String maskedLink(String name, String url) {
		return String.format("[%s](%s)", name, url);
	}

	private String projectUrl(String projectId) {
		return "https://modrinth.com/project/" + projectId;
	}

	private String formatVersion(Version version) {
		return maskedLink(version.versionNumber, projectUrl(version.projectId) + "/version/" + version.id);
	}

	@Async
	public void makeSubmission(SubmissionData data, int submissionCount) {
		var fields = new ArrayList<Field>();

		fields.add(new Field("Source", data.source()));

		if ((data.platform().inner() instanceof SubmissionData.AssociatedData.Modrinth mr)) {
			fields.add(new Field("Project", projectUrl(mr.projectId())));
		}

		var embed = embedForSubmission(
			data,
			"Submitted",
			3466376,
			fields,
			new Footer(submissionCount + " submissions so far", null)
		);

		sendEmbed(embed);
	}

	@Async
	public void updateSubmissionVersion(SubmissionData data, @Nullable Version newVersion, SubmissionData.AssociatedData.Modrinth mr, @Nullable List<Version> versions) {
		var fields = new ArrayList<Field>();

		if (newVersion != null) {
			fields.add(new Field("Version", formatVersion(newVersion)));
			fields.add(new Field("Changelog", newVersion.changelog));
		}

		if (versions != null) {
			var previousVersion = versions.stream().filter(v -> v.id.equals(mr.versionId())).findAny();
			previousVersion.ifPresent(version -> fields.add(new Field("Previous Version", formatVersion(version))));
		}

		var embed = embedForSubmission(
			data,
			"Updated Version",
			5814783,
			fields
		);

		sendEmbed(embed);
	}

	@Async
	public void editSubmission(SubmissionPatchData edit, SubmissionData data) {
		var fields = new ArrayList<Field>();

		if (edit.sourceUrl() != null) {
			fields.add(new Field("Source url", edit.sourceUrl()));
		}

		if (edit.downloadUrl() != null) {
			fields.add(new Field("Download url", edit.downloadUrl()));
		}

		if (edit.homepage() != null) {
			fields.add(new Field("Homepage", edit.homepage()));
		}

		if (fields.isEmpty()) {
			return;
		}

		var embed = embedForSubmission(
			data,
			"Edited submission",
			5814783,
			fields
		);

		sendEmbed(embed);
	}

	@Async
	public void deleteSubmission(SubmissionData data) {
		var embed = embedForSubmission(
			data,
			"Submission revoked",
			16715365
		);

		sendEmbed(embed);
	}

	@Async
	public void phaseChanged(String eventName, String phase, int count) {
		var embed = new Embed(
			eventName,
			String.format("Phase switched to \"%s\"", phase),
			16772110,
			null,
			new Footer(String.format("Currently %d submissions", count), null),
			null
		);

		sendEmbed(embed);
	}
}
