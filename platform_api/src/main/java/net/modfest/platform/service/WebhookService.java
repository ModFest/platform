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

	private record Field(String name, String value) {
	}

	private record Footer(String text, @Nullable String icon_url) {
	}

	private record Image(String url) {
	}

	private record Embed(String title, String description, int color, List<Field> fields, Footer footer, Image image,
						   Image thumbnail) {
	}

	private record WebhookPayload(List<Embed> embeds) {
	}

	private Embed embedForSubmission(SubmissionData data, String event, int eventColor) {
		return embedForSubmission(data, event, eventColor, null, null);
	}

	private Embed embedForSubmission(SubmissionData data, String event, int eventColor, @Nullable List<Field> fields) {
		return embedForSubmission(data, event, eventColor, fields, null);
	}

	private Embed embedForSubmission(SubmissionData data, String event, int eventColor, @Nullable List<Field> fields, @Nullable Footer footer) {
		return embedForSubmission(data, event, eventColor, fields, footer, null);
	}

	private Embed embedForSubmission(SubmissionData data, String event, int eventColor, @Nullable List<Field> fields, @Nullable Footer footer, @Nullable Image image) {
		return new Embed(
			data.name(),
			event,
			eventColor,
			fields,
			footer,
			image,
			getImage(data, ImageService.SubmissionImageType.ICON)
		);
	}

	private @Nullable Image getImage(SubmissionData data, ImageService.SubmissionImageType type) {
		var subKey = new SubmissionRepository.SubmissionId(data.event(), data.id());
		var icon = imageService.getImageUrl(null, subKey, type);

		if (icon == null) {
			return null;
		}

		return new Image(icon);
	}

	private void sendEmbed(Embed embed) {
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

	private String timestampRelative(int minutes) {
		var currentTime = System.currentTimeMillis() / 1000L;
		var time = currentTime + minutes * 60L;

		return "<t:" + time + ":R>";
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
	public void submissionImageChanged(SubmissionData data, ImageService.SubmissionImageType type) {
		var typeString = switch (type) {
			case TEST -> "Test";
			case CLAIM -> "Claim";
			case BUILD -> "Build";
			default -> null;
		};

		if (typeString == null) {
			return;
		}

		var embed = embedForSubmission(data, typeString + " image set", 16777048, null, null, getImage(data, type));
		sendEmbed(embed);
	}

	@Async
	public void editSubmissionBooth(SubmissionData data, SubmissionData.BoothData edit) {
		var fields = new ArrayList<Field>();

		if (edit.itemIcon() != null) {
			fields.add(new Field("Item icon", edit.itemIcon()));
		}

		if (edit.markerPos() != null) {
			fields.add(new Field("Marker position", edit.markerPos().toFormattedString()));
		}

		if (edit.minutesToComplete() != null) {
			fields.add(new Field("Complete", timestampRelative(edit.minutesToComplete())));
		}

		if(edit.shards() != null) {
			fields.add(new Field("Shards", edit.shards().toString()));
		}

		if(edit.status() != null) {
			fields.add(new Field("Status", edit.status().name()));
		}

		if(edit.warp() != null) {
			fields.add(new Field("Warp", edit.warp().toFormattedString()));
		}

		var embed = embedForSubmission(data, "Boot data changed", 16777048, fields);
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
			null,
			null
		);

		sendEmbed(embed);
	}
}
