package net.modfest.platform.service;

import jakarta.validation.ValidationException;
import net.modfest.platform.pojo.CurrentEventData;
import net.modfest.platform.repository.CurrentEventRepository;
import net.modfest.platform.repository.EventRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CurrentEventService {
	@Autowired
	private CurrentEventRepository repository;
	@Autowired
	private EventRepository eventRepository;

	@NonNull
	public CurrentEventData getCurrentEvent() {
		return repository.get();
	}

	public void setCurrentEvent(@NonNull CurrentEventData data) throws IOException {
		// Validate that the data is correct
		if (data.event() != null && !eventRepository.contains(data.event())) {
			// Note that null *is* a valid event
			throw new ValidationException("Current event must refer to a valid event, or null");
		}

		// It's good!
		repository.save(data);
	}
}
