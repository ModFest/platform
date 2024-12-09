package net.modfest.platform.controller;

import net.modfest.platform.pojo.CurrentEventData;
import net.modfest.platform.service.CurrentEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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
	public void setCurrentEvent(@RequestBody CurrentEventData data) throws IOException {
		service.setCurrentEvent(data);
	}
}
