package net.modfest.platform.service;

import net.modfest.platform.pojo.EventData;
import net.modfest.platform.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventService {
	@Autowired
	private EventRepository eventRepository;

	public EventData getEventById(String id) {
		return eventRepository.get(id);
	}
}
