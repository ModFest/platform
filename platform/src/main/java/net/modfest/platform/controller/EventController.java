package net.modfest.platform.controller;

import net.modfest.platform.pojo.EventData;
import net.modfest.platform.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/event", produces = MediaType.APPLICATION_JSON_VALUE)
public class EventController {
	@Autowired
	private EventRepository repository;

	@GetMapping("/{id}")
	public EventData getEvent(@PathVariable String id) {
		return repository.get(id);
	}
}
