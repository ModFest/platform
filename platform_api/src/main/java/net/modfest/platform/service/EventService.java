package net.modfest.platform.service;

import net.modfest.platform.pojo.EventData;
import net.modfest.platform.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
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

	public EventData getEventById(String id) {
		return eventRepository.get(id);
	}

	// If the event has registrations open:
	//   if a user has zero submissions, they can freely register/deregister
	//   if a user has more than zero submissions, they must be registered and cannot deregister
	// If the event has registrations closed:
	//   the user is only registered if they have at least one submission

	public void setPhase(EventData data, EventData.Phase target) throws IOException {
		eventRepository.save(data.withPhase(target));
		if (data.phase().canRegister() != target.canRegister()) {
			resetRegistrationData(data);
		}
	}

	public void resetRegistrationData(EventData event) {
		AtomicReference<IOException> hadException = new AtomicReference<>();
		if (event.phase().canRegister()) {
			// The event has registrations open. Users with zero submissions can freely register
			// and deregister. But if a user has a submission, they must be registered
			allEventAuthors(event).forEach(authorId -> {
				var u = userService.getByMfId(authorId);
				if (!u.registered().contains(event.id())) {
					try {
						userService.save(u.setRegistration(event, true));
					} catch (IOException e) {
						hadException.set(e);
					}
				}
			});
		} else {
			// The event has registrations closed. Users are registered if they have submissions,
			// and only if they have submissions
			var authors = allEventAuthors(event).collect(Collectors.toSet());
			for (var u : userService.getAll()) {
				if (u.registered().contains(event.id()) != authors.contains(u.id())) {
					try {
						userService.save(u.setRegistration(event, authors.contains(u.id())));
					} catch (IOException e) {
						hadException.set(e);
					}
				}
			}
		}

		if (hadException.get() != null) {
			throw new RuntimeException(hadException.get());
		}
	}

	private Stream<String> allEventAuthors(EventData event) {
		return submissionService
			.getSubmissionsFromEvent(event)
			.flatMap(e -> e.authors().stream());
	}
}
