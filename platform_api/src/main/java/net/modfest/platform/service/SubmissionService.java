package net.modfest.platform.service;

import net.modfest.platform.pojo.EventData;
import net.modfest.platform.pojo.SubmissionData;
import net.modfest.platform.pojo.UserData;
import net.modfest.platform.repository.SubmissionRepository;
import nl.theepicblock.dukerinth.ModrinthApi;
import nl.theepicblock.dukerinth.VersionFilter;
import nl.theepicblock.dukerinth.models.Project;
import nl.theepicblock.dukerinth.models.TeamMember;
import nl.theepicblock.dukerinth.models.Version;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
	private ModrinthApi modrinth;

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
		var organization = modrinth.projects().getProjectOrganization(modrinthProjectId);

		// Returns null if project no does exist
		if (members == null) return null;

		return Stream.concat(
			members.stream(),
			(organization == null ? List.<TeamMember>of() : organization.members).stream()
		).map(mrData -> userService.getByMfId(mrData.user.id))
			.filter(Objects::nonNull);
	}

	public SubmissionData makeModrinthSubmission(String eventId, String mrProjectId) {
		var project = modrinth.projects().getProject(mrProjectId);
		var subId = project.slug; // Normalize id by using slug. Just in case the user entered an actual id
		if (subId == null) subId = project.id;
		var subKey = new SubmissionRepository.SubmissionId(eventId, subId);

		if (submissionRepository.contains(subKey)) {
			throw new RuntimeException("submission already exists");
		}

		var authors = getUsersForRinthProject(subId);
		var latest = getLatestModrinth(subId, eventService.getEventById(eventId));

		if (latest == null) {
			throw new RuntimeException("No latest version");
		}

		var primaryFile = latest.files.stream().filter(f -> f.primary).findAny().orElse(latest.files.get(0));

		submissionRepository.save(
			new SubmissionData(
				subId,
				eventId,
				project.title,
				project.description,
				authors.map(UserData::id).collect(Collectors.toSet()),
				new SubmissionData.FileData(
					new SubmissionData.FileData.Modrinth(
						project.id,
						latest.id
					)
				),
				getImages(project),
				primaryFile.url,
				project.sourceUrl,
				new SubmissionData.Awards(
					Set.of(),
					Set.of()
				)
			)
		);
		return submissionRepository.get(subKey);
	}

	private static SubmissionData.Images getImages(Project mrProject) {
		return new SubmissionData.Images(
			mrProject.iconUrl,
			mrProject.gallery.stream().filter(item -> item.featured).map(item -> item.url).findFirst().orElse(null)
		);
	}

	/**
	 * Retrieves the most recent version of a modrinth project
	 * @param event The event this version will be for. Used for filtering
	 */
	private @Nullable Version getLatestModrinth(String mrProject, EventData event) {
		var filter = VersionFilter.ofLoader(event.mod_loader())
			.andGameVersion(event.minecraft_version());
		return modrinth.projects().getVersions(mrProject, filter)
			.stream()
			.max(Comparator.comparing(v -> v.datePublished))
			.orElse(null);
	}
}
