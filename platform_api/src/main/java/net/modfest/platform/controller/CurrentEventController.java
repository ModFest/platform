package net.modfest.platform.controller;

import jakarta.validation.ValidationException;
import net.modfest.platform.pojo.CurrentEventData;
import net.modfest.platform.service.CurrentEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class CurrentEventController {
	@Autowired
	private CurrentEventService service;

	@GetMapping("/currentevent/")
	public CurrentEventData getCurrentEvent() {
		return service.getCurrentEvent();
	}

	@PutMapping("/currentevent/")
	public void setCurrentEvent(@RequestBody CurrentEventData data) {
		try {
			service.setCurrentEvent(data);
		} catch (ValidationException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}
}
