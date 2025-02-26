package net.modfest.platform.controller;

import net.modfest.platform.pojo.EventData;
import net.modfest.platform.pojo.SubmissionData;
import net.modfest.platform.pojo.SubmissionPatchData;
import net.modfest.platform.pojo.SubmitRequestModrinth;
import net.modfest.platform.security.PermissionUtils;
import net.modfest.platform.security.Permissions;
import net.modfest.platform.service.EventService;
import net.modfest.platform.service.SubmissionService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class EventController {
	@Autowired
	private SubmissionService service;
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
	public void register(@PathVariable String id, @PathVariable String userId) {
		setRegistration(id, userId, true);
	}

	@DeleteMapping("/event/{id}/registrations/{userId}")
	public void unregister(@PathVariable String id, @PathVariable String userId) {
		setRegistration(id, userId, false);
	}

	public void setRegistration(@PathVariable String id, @RequestBody String userId, boolean registered) {
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

	@GetMapping("/event/{eventId}/submissions")
	public List<SubmissionData> getSubmissions(@PathVariable String eventId) {
		var event = getEvent(eventId);
		return service.getSubmissionsFromEvent(event).toList();
	}

	@PostMapping(value = "/event/{eventId}/submissions", params = "type=modrinth")
	@RequiresPermissions(Permissions.Event.SUBMIT)
	public SubmissionData makeSubmission(@PathVariable String eventId, @RequestBody SubmitRequestModrinth submission) {
		var event = getEvent(eventId);
		var subject = SecurityUtils.getSubject();
		var bypass = subject.isPermitted(Permissions.Event.SUBMIT_BYPASS);
		var can_others = subject.isPermitted(Permissions.Event.SUBMIT_OTHER);

		var authors = service.getUsersForRinthProject(submission.modrinthProject());
		if (authors == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project doesn't exist");
		}

		var self = authors.anyMatch(d -> PermissionUtils.owns(subject, d));

		if (!event.phase().canSubmit() && !bypass) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Event does not accept submissions");
		}

		if (!self && !can_others) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
				"You don't have permissions to submit for people other than yourself");
		}

		return service.makeModrinthSubmission(eventId, submission.modrinthProject());
	}

	@PatchMapping("/event/{eventId}/submission/{subId}")
	public void editSubmissionData(@PathVariable String eventId, @PathVariable String subId, @RequestBody SubmissionPatchData editData) {
		getEvent(eventId);
		var subject = SecurityUtils.getSubject();
		var can_others = subject.isPermitted(Permissions.Event.EDIT_OTHER_SUBMISSION);
		var submission = service.getSubmission(eventId, subId);
		if (submission == null) {
			throw new IllegalArgumentException();// TODO
		}
		if (!PermissionUtils.owns(subject, submission) && !can_others) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
				"You do not have permissions to edit this data");
		}

		service.editSubmission(submission, editData);
	}
}
