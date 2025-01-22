package net.modfest.platform.controller;

import net.modfest.platform.pojo.EventData;
import net.modfest.platform.security.PermissionUtils;
import net.modfest.platform.security.Permissions;
import net.modfest.platform.service.EventService;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Collection;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class EventController {
	@Autowired
	private EventService eventService;
	@Autowired
	private UserController userController;

	@GetMapping("/events")
	public Collection<EventData> getAllEvents() {
		return eventService.getAll();
	}

	@GetMapping("/event/{id}")
	public EventData getEvent(@PathVariable String id) {
		var event = eventService.getEventById(id);
		if (event == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such event exists");
		}
		return event;
	}

	@PutMapping("/event/{id}/registrations/{userId}")
	public void register(@PathVariable String id, @PathVariable String userId) throws IOException {
		setRegistration(id, userId, true);
	}

	@DeleteMapping("/event/{id}/registrations/{userId}")
	public void unregister(@PathVariable String id, @PathVariable String userId) throws IOException {
		setRegistration(id, userId, false);
	}

	public void setRegistration(@PathVariable String id, @RequestBody String userId, boolean registered) throws IOException {
		var event = getEvent(id);
		// Get the user as if we were requesting them from /user/{id}
		var user = userController.getSingleUser(userId);

		var subject = SecurityUtils.getSubject();
		var bypass = subject.isPermitted(Permissions.Event.BYPASS_REGISTRATIONS);
		var can_others = subject.isPermitted(Permissions.Event.REGISTER_OTHERS);
		var self = PermissionUtils.owns(subject, user);

		if (!event.phase().canRegister() && !bypass) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
				registered ? "Event does not accept registrations" : "Event does not accept unregistrations");
		}

		if (!registered && !eventService.canUnregister(event, user)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
				"The user has made submissions to this event. Please unsubmit before unregistering.");
		}

		if (!self && !can_others) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
				registered ? "You don't have permissions to register people other than yourself"
							: "You don't have permissions to unregister people other than yourself");
		}
		eventService.setRegistered(event, user, registered);
	}
}
