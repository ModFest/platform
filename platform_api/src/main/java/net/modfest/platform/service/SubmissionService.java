package net.modfest.platform.service;

import jakarta.servlet.http.HttpServletRequest;
import net.modfest.platform.misc.PlatformStandardException;
import net.modfest.platform.pojo.*;
import net.modfest.platform.repository.SubmissionRepository;
import nl.theepicblock.dukerinth.ModrinthApi;
import nl.theepicblock.dukerinth.VersionFilter;
import nl.theepicblock.dukerinth.models.Project;
import nl.theepicblock.dukerinth.models.TeamMember;
import nl.theepicblock.dukerinth.models.Version;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SubmissionService {
	@Autowired
	private SubmissionRepository submissionRepository;
	@Autowired
	private UserService userService;
	@Lazy
	@Autowired
	private EventService eventService;
	@Autowired
	private ImageService imageService;
	@Autowired
	private ModrinthApi modrinth;

	public SubmissionData getSubmission(String eventId, String subId) {
		return submissionRepository.get(new SubmissionRepository.SubmissionId(eventId, subId));
	}

	public SubmissionData editSubmission(SubmissionData data, SubmissionPatchData edit) {
		if (edit.name() != null) {
			data = data.withName(edit.name());
		}
		if (edit.description() != null) {
			data = data.withDescription(edit.description());
		}
		if (edit.sourceUrl() != null) {
			data = data.withSource(edit.sourceUrl().isBlank() ? null : edit.sourceUrl());
		}
		if (edit.homepage() != null) {
			if (data.platform().inner() instanceof SubmissionData.AssociatedData.Other o) {
				var newInner = o.withHomepageUrl(edit.homepage().isBlank() ? null : edit.homepage());
				data = data.withPlatform(new SubmissionData.AssociatedData(newInner));
			} else {
				throw new IllegalStateException("Editing homepage is only allowed for 'other' submissions");
			}
		}
		if (edit.downloadUrl() != null) {
			if (data.platform().inner() instanceof SubmissionData.AssociatedData.Other o) {
				var newInner = o.withDownloadUrl(edit.downloadUrl().isBlank() ? null : edit.downloadUrl());
				data = data.withPlatform(new SubmissionData.AssociatedData(newInner));
			} else {
				throw new IllegalStateException("Editing download url is only allowed for 'other' submissions");
			}
		}
		submissionRepository.save(data);
		return getSubmission(data.event(), data.id());
	}

	public SubmissionData editSubmissionBooth(SubmissionData data, SubmissionData.BoothData edit) {
		if (data.boothData() != null) {
			if (edit.markerPos() == null) edit = edit.withMarkerPos(data.boothData().markerPos());
			if (edit.warp() == null) edit = edit.withWarp(data.boothData().warp());
			if (edit.itemIcon() == null) edit = edit.withItemIcon(data.boothData().itemIcon());
			if (edit.shards() == null) edit = edit.withShards(data.boothData().shards());
			if (edit.minutesToComplete() == null) edit = edit.withMinutesToComplete(data.boothData().minutesToComplete());
			if (edit.status() == null) edit = edit.withStatus(data.boothData().status());
		}
		data = data.withBoothData(edit);
		submissionRepository.save(data);
		return getSubmission(data.event(), data.id());
	}

	public SubmissionData updateSubmissionVersion(SubmissionData data) throws PlatformStandardException {
		if (!(data.platform().inner() instanceof SubmissionData.AssociatedData.Modrinth mr)) {
			throw new PlatformStandardException(PlatformErrorResponse.ErrorType.UPDATE_NON_MODRINTH, null);
		}

		var project = modrinth.projects().getProject(mr.projectId());

		if (project == null) {
			throw PlatformStandardException.doesntExist(PlatformErrorResponse.IdType.MRPROJECT, mr.projectId());
		}

		var latest = getLatestModrinth(mr.projectId(), eventService.getEventById(data.event()), project.projectType);

		var newData = data.withPlatform(new SubmissionData.AssociatedData(
			new SubmissionData.AssociatedData.Modrinth(
				project.id,
				latest == null ? null : latest.id
			)
		));

		submissionRepository.save(newData);
		return getSubmission(newData.event(), newData.id());
	}

	public SubmissionData updateSubmissionMeta(SubmissionData data) throws PlatformStandardException {
		if (!(data.platform().inner() instanceof SubmissionData.AssociatedData.Modrinth mr)) {
			throw new PlatformStandardException(PlatformErrorResponse.ErrorType.UPDATE_NON_MODRINTH, null);
		}

		var project = modrinth.projects().getProject(mr.projectId());

		if (project == null) {
			throw PlatformStandardException.doesntExist(PlatformErrorResponse.IdType.MRPROJECT, mr.projectId());
		}

		var subKey = new SubmissionRepository.SubmissionId(data.event(), data.id());

		if (project.iconUrl != null) {
			imageService.downloadSubmissionImage(project.iconUrl, subKey, ImageService.SubmissionImageType.ICON);
		}
		var galleryUrl = getGalleryUrl(project);
		if (galleryUrl != null) {
			imageService.downloadSubmissionImage(galleryUrl, subKey, ImageService.SubmissionImageType.SCREENSHOT);
		}

		var newData = data.withName(project.title).withDescription(project.description).withSource(project.sourceUrl);

		submissionRepository.save(newData);
		return getSubmission(newData.event(), newData.id());
	}

	public SubmissionData updateSubmissionBooth(SubmissionData data) throws PlatformStandardException {
		if (!(data.platform().inner() instanceof SubmissionData.AssociatedData.Modrinth mr)) {
			throw new PlatformStandardException(PlatformErrorResponse.ErrorType.UPDATE_NON_MODRINTH, null);
		}

		var project = modrinth.projects().getProject(mr.projectId());

		if (project == null) {
			throw PlatformStandardException.doesntExist(PlatformErrorResponse.IdType.MRPROJECT, mr.projectId());
		}

		var subKey = new SubmissionRepository.SubmissionId(data.event(), data.id());

		if (project.iconUrl != null) {
			imageService.downloadSubmissionImage(project.iconUrl, subKey, ImageService.SubmissionImageType.ICON);
		}
		var galleryUrl = getGalleryUrl(project);
		if (galleryUrl != null) {
			imageService.downloadSubmissionImage(galleryUrl, subKey, ImageService.SubmissionImageType.SCREENSHOT);
		}

		var newData = data.withName(project.title).withDescription(project.description).withSource(project.sourceUrl);

		submissionRepository.save(newData);
		return getSubmission(newData.event(), newData.id());
	}

	public SubmissionData leaveSubmission(SubmissionData data, UserData author) {
		data.authors().remove(author.id());
		submissionRepository.save(data);
		return data;
	}

	public SubmissionData addSubmissionAuthor(SubmissionData data, UserData author) {
		data.authors().add(author.id());
		submissionRepository.save(data);
		return data;
	}

	public void deleteSubmission(String eventId, String subId) {
		submissionRepository.delete(new SubmissionRepository.SubmissionId(eventId, subId));
	}

	/**
	 * Retrieve all submissions made by a user
	 * @param filter If non-null, only submissions associated with that event will be returned
	 */
	public Stream<SubmissionData> getSubmissionsFromUser(UserData user, @Nullable EventData filter) {
		var result = submissionRepository.getAll()
			.stream()
			.filter(submission -> submission.authors().contains(user.id()));
		if (filter != null) {
			result = result.filter(s -> s.event().equals(filter.id()));
		}
		return result;
	}

	public Stream<SubmissionData> getSubmissionsFromEvent(EventData event) {
		return submissionRepository.getAll()
				.stream()
				.filter(s -> s.event().equals(event.id()));
	}

	public Stream<UserData> getUsersForRinthProject(String modrinthProjectId) {
		var members = modrinth.projects().getProjectMembers(modrinthProjectId);

		// Returns null if project no does exist
		if (members == null) return null;

		var organization = modrinth.projects().getProjectOrganization(modrinthProjectId);

		return Stream.concat(
			members.stream(),
			(organization == null ? List.<TeamMember>of() : organization.members).stream()
		).map(mrData -> userService.getByModrinthId(mrData.user.id))
			.filter(Objects::nonNull);
	}

	public SubmissionData makeSubmissionOther(EventData event, Set<UserData> authors, SubmitRequestOther submitData) {
		var subId = submitData.name()
			.toLowerCase(Locale.ROOT)
			.replace(" ", "_")
			.replaceAll("[^a-z0-9_\\-\\s]", "");
		if (subId.isBlank()) {
			throw new IllegalStateException();
		}
		var idKey = new SubmissionRepository.SubmissionId(event.id(), subId);
		if (submissionRepository.contains(idKey)) {
			throw new IllegalStateException("Submission id already exists");
		}
		var submission = new SubmissionData(
			subId,
			event.id(),
			submitData.name(),
			submitData.description(),
			authors.stream().map(UserData::id).collect(Collectors.toSet()),
			new SubmissionData.AssociatedData(
				new SubmissionData.AssociatedData.Other(
					submitData.homepage(),
					submitData.downloadUrl()
				)
			),
			submitData.sourceUrl(),
			null,
			null,
			new SubmissionData.Awards(
				Set.of(),
				Set.of()
			)
		);
		submissionRepository.save(submission);
		return submission;
	}

	public SubmissionData makeSubmissionModrinth(String eventId, Set<UserData> authors, String mrProjectId) {
		var project = modrinth.projects().getProject(mrProjectId);
		var subId = project.slug; // Normalize id by using slug. Just in case the user entered an actual id
		if (subId == null) subId = project.id;
		var subKey = new SubmissionRepository.SubmissionId(eventId, subId);

		if (submissionRepository.contains(subKey)) {
			throw new RuntimeException("submission already exists");
		}

		if (project.sourceUrl == null || project.sourceUrl.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Modrinth submissions MUST have visible source code!");
		}

		var latest = getLatestModrinth(subId, eventService.getEventById(eventId), project.projectType);

		if (project.iconUrl != null) {
			imageService.downloadSubmissionImage(project.iconUrl, subKey, ImageService.SubmissionImageType.ICON);
		}
		var galleryUrl = getGalleryUrl(project);
		if (galleryUrl != null) {
			imageService.downloadSubmissionImage(galleryUrl, subKey, ImageService.SubmissionImageType.SCREENSHOT);
		}
		submissionRepository.save(
			new SubmissionData(
				subId,
				eventId,
				project.title,
				project.description,
				authors.stream().map(UserData::id).collect(Collectors.toSet()),
				new SubmissionData.AssociatedData(
					new SubmissionData.AssociatedData.Modrinth(
						project.id,
						latest == null ? null : latest.id
					)
				),
				project.sourceUrl,
				null,
				null,
				new SubmissionData.Awards(
					Set.of(),
					Set.of()
				)
			)
		);
		return submissionRepository.get(subKey);
	}

	private static @Nullable String getGalleryUrl(Project mrProject) {
		return mrProject.gallery
			.stream()
			.filter(item -> item.featured)
			.map(item -> item.url)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Retrieves the most recent version of a modrinth project
	 *
	 * @param event       The event this version will be for. Used for filtering
	 * @param projectType The modrinth project type. Used for filtering
	 */
	private @Nullable Version getLatestModrinth(String mrProject, EventData event, String projectType) {
		if (projectType.equals("modpack")) return null;
		var filter = VersionFilter.ofLoaders(List.of(event.mod_loader(), "minecraft", "datapack", "iris"))
			.andGameVersion(event.minecraft_version());
		return modrinth.projects().getVersions(mrProject, filter)
			.stream()
			.max(Comparator.comparing(v -> v.datePublished))
			.orElse(null);
	}

	/**
	 * Adds data which is only available in the http response, but not actually stored
	 */
	public SubmissionResponseData addResponseInfo(HttpServletRequest request, SubmissionData data) {
		var subKey = new SubmissionRepository.SubmissionId(data.event(), data.id());
		return SubmissionResponseData.fromData(
			data,
			new SubmissionResponseData.Images(
				imageService.getImageUrl(request, subKey, ImageService.SubmissionImageType.ICON),
				imageService.getImageUrl(request, subKey, ImageService.SubmissionImageType.SCREENSHOT),
				imageService.getImageUrl(request, subKey, ImageService.SubmissionImageType.TEST),
				imageService.getImageUrl(request, subKey, ImageService.SubmissionImageType.CLAIM),
				imageService.getImageUrl(request, subKey, ImageService.SubmissionImageType.BUILD)
			)
		);
	}
}
