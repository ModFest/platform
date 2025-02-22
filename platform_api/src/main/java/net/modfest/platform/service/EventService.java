package net.modfest.platform.service;

import jakarta.annotation.PostConstruct;
import net.modfest.platform.git.GitScope;
import net.modfest.platform.git.GlobalGitManager;
import net.modfest.platform.pojo.EventData;
import net.modfest.platform.pojo.UserData;
import net.modfest.platform.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EventService {
	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private SubmissionService submissionService;
	@Autowired
	private UserService userService;
	@Autowired
	private GlobalGitManager git;

	public EventData getEventById(String id) {
		return eventRepository.get(id);
	}

	public Collection<EventData> getAll() {
		return eventRepository.getAll();
	}

	// If the event has registrations open:
	//   if a user has zero submissions, they can freely register/unregister
	//   if a user has more than zero submissions, they must be registered and cannot unregister
	// If the event has registrations closed:
	//   the user is only registered if they have at least one submission

	public void setPhase(EventData data, EventData.Phase target) {
		eventRepository.save(data.withPhase(target));
		if (data.phase().canRegister() != target.canRegister()) {
			fixRegistrationData(data);
		}
	}

	/**
	 * Checks if a user can unregister.
	 * WARNING: Does not take into account the phase!
	 */
	public boolean canUnregister(EventData event, UserData user) {
		return allEventAuthors(event).noneMatch(u -> Objects.equals(u, user.id()));
	}

	/**
	 * Forcibly sets a user to be registered/unregistered.
	 * WARNING: Does not take into account the phase!
	 */
	public void setRegistered(EventData event, UserData user, boolean registered) {
		userService.save(user.withRegistration(event, registered));
	}

	public void fixRegistrationData(EventData event) {
		if (event.phase().canRegister()) {
			// The event has registrations open. Users with zero submissions can freely register
			// and unregister. But if a user has a submission, they must be registered
			allEventAuthors(event).forEach(authorId -> {
				var u = userService.getByMfId(authorId);
				if (!u.registered().contains(event.id())) {
					userService.save(u.withRegistration(event, true));
				}
			});
		} else {
			// The event has registrations closed. Users are registered if they have submissions,
			// and only if they have submissions
			var authors = allEventAuthors(event).collect(Collectors.toSet());
			for (var u : userService.getAll()) {
				if (u.registered().contains(event.id()) != authors.contains(u.id())) {
					userService.save(u.withRegistration(event, authors.contains(u.id())));
				}
			}
		}
	}

	private Stream<String> allEventAuthors(EventData event) {
		return submissionService
			.getSubmissionsFromEvent(event)
			.flatMap(e -> e.authors().stream());
	}

	@PostConstruct
	public void fixAllRegistrationData() {
		git.withScope(new GitScope("Fix registration data"), () -> {
			for (var e : getAll()) {
				fixRegistrationData(e);
			}
		});
	}
}
