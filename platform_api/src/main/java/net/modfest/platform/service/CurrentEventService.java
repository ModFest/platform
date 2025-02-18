package net.modfest.platform.service;

import net.modfest.platform.misc.PlatformStandardException;
import net.modfest.platform.pojo.CurrentEventData;
import net.modfest.platform.pojo.PlatformErrorResponse;
import net.modfest.platform.repository.CurrentEventRepository;
import net.modfest.platform.repository.EventRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

	public void setCurrentEvent(@NonNull CurrentEventData data) throws PlatformStandardException {
		// Validate that the data is correct
		if (data.event() != null && !eventRepository.contains(data.event())) {
			// Note that null *is* a valid event
			throw new PlatformStandardException(PlatformErrorResponse.ErrorType.EVENT_NO_EXIST, data.event());
		}

		// It's good!
		repository.save(data);
	}
}
