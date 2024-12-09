package net.modfest.platform.controller;

import net.modfest.platform.pojo.EventData;
import net.modfest.platform.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class EventController {
	@Autowired
	private EventRepository repository;

	@GetMapping("/events")
	public Collection<EventData> getAllEvents() {
		return repository.getAll();
	}

	@GetMapping("/event/{id}")
	public EventData getEvent(@PathVariable String id) {
		var event = repository.get(id);
		if (event == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such event exists");
		}
		return event;
	}
}
