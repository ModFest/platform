package net.modfest.platform.controller;

import jakarta.servlet.http.HttpServletRequest;
import net.modfest.platform.misc.PlatformStandardException;
import net.modfest.platform.pojo.*;
import net.modfest.platform.repository.SubmissionRepository;
import net.modfest.platform.security.PermissionUtils;
import net.modfest.platform.security.Permissions;
import net.modfest.platform.service.EventService;
import net.modfest.platform.service.ImageService;
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
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class EventController {
	@Autowired
	private SubmissionService service;
	@Autowired
	private EventService eventService;
	@Autowired
	private UserController userController;
	@Autowired
	private ImageService imageService;

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
	public UserData register(@PathVariable String id, @PathVariable String userId) {
		return setRegistration(id, userId, true);
	}

	@DeleteMapping("/event/{id}/registrations/{userId}")
	public UserData unregister(@PathVariable String id, @PathVariable String userId) {
		return setRegistration(id, userId, false);
	}

	private UserData setRegistration(@PathVariable String id, @RequestBody String userId, boolean registered) {
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
		return eventService.setRegistered(event, user, registered);
	}

	@GetMapping("/event/{eventId}/submission/{subId}")
	public SubmissionResponseData getSingleSubmission(HttpServletRequest request, @PathVariable String eventId, @PathVariable String subId) {
		return service.addResponseInfo(
			request, service.getSubmission(eventId, subId)
		);
	}

	@GetMapping("/event/{eventId}/submissions")
	public List<SubmissionResponseData> getSubmissions(HttpServletRequest request, @PathVariable String eventId) {
		var event = getEvent(eventId);
		return service
			.getSubmissionsFromEvent(event)
			.map(s -> service.addResponseInfo(request, s))
			.toList();
	}

	@PostMapping(value = "/event/{eventId}/submissions", params = "type=other")
	@RequiresPermissions(Permissions.Event.SUBMIT)
	public SubmissionResponseData makeSubmissionOther(HttpServletRequest request, @PathVariable String eventId, @RequestBody SubmitRequestOther submission) {
		var event = getEvent(eventId);
		var subject = SecurityUtils.getSubject();
		var bypass = subject.isPermitted(Permissions.Event.SUBMIT_BYPASS);
		var can_others = subject.isPermitted(Permissions.Event.SUBMIT_OTHER);

		var authors = submission.authors()
			.stream()
			.map(id -> userController.getSingleUser(id)).collect(Collectors.toSet());
		var self = authors.stream().anyMatch(d -> PermissionUtils.owns(subject, d));

		if (!event.phase().canSubmit() && !bypass) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Event does not accept submissions");
		}

		if (!self && !can_others) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
				"You don't have permissions to submit for people other than yourself");
		}

		return service.addResponseInfo(request, service.makeSubmissionOther(event, authors, submission));
	}

	@PostMapping(value = "/event/{eventId}/submissions", params = "type=modrinth")
	@RequiresPermissions(Permissions.Event.SUBMIT)
	public SubmissionResponseData makeSubmissionModrinth(HttpServletRequest request, @PathVariable String eventId, @RequestBody SubmitRequestModrinth submission) {
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

		return service.addResponseInfo(
			request,
			service.makeSubmissionModrinth(
				eventId,
				(subject.getPrincipal() instanceof UserData d) ? Set.of(d) : Set.of(),
				submission.modrinthProject()));
	}

	@PatchMapping("/event/{eventId}/submission/{subId}")
	public SubmissionResponseData editSubmissionData(HttpServletRequest request, @PathVariable String eventId, @PathVariable String subId, @RequestBody SubmissionPatchData editData) throws PlatformStandardException {
		var event = getEvent(eventId);
		var submission = service.getSubmission(eventId, subId);
		if (submission == null) {
			throw new IllegalArgumentException();// TODO
		}

		checkCanEdit(event, submission);

		return service.addResponseInfo(
			request,
			service.editSubmission(submission, editData)
		);
	}

	@PutMapping("/event/{eventId}/submission/{subId}/updateVersion")
	public SubmissionResponseData updateSubmissionVersion(HttpServletRequest request, @PathVariable String eventId, @PathVariable String subId) throws PlatformStandardException {
		var event = getEvent(eventId);
		var submission = service.getSubmission(eventId, subId);
		if (submission == null) {
			throw new IllegalArgumentException();// TODO
		}

		checkCanEdit(event, submission);

		return service.addResponseInfo(
			request,
			service.updateSubmissionVersion(submission)
		);
	}

	@PutMapping("/event/{eventId}/submission/{subId}/updateMeta")
	public SubmissionResponseData updateSubmissionMeta(HttpServletRequest request, @PathVariable String eventId, @PathVariable String subId) throws PlatformStandardException {
		var event = getEvent(eventId);
		var submission = service.getSubmission(eventId, subId);
		if (submission == null) {
			throw new IllegalArgumentException();// TODO
		}

		checkCanEdit(event, submission);

		return service.addResponseInfo(
			request,
			service.updateSubmissionMeta(submission)
		);
	}

	@DeleteMapping("/event/{eventId}/submission/{subId}/authors/{userId}")
	public SubmissionResponseData deleteSubmissionAuthor(HttpServletRequest request, @PathVariable String eventId, @PathVariable String subId, @PathVariable String userId) throws PlatformStandardException {
		var event = getEvent(eventId);
		var submission = service.getSubmission(eventId, subId);
		if (submission == null) {
			throw new IllegalArgumentException();// TODO
		}

		checkCanEdit(event, submission);

		if (submission.authors().size() < 2) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"You cannot remove a user from a solo submission");
		}

		UserData user = userController.getSingleUser(userId);

		var subject = SecurityUtils.getSubject();
		var edit_others = subject.isPermitted(Permissions.Users.EDIT_OTHERS);
		var owns = PermissionUtils.owns(subject, user);

		if (!owns && !edit_others) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may not edit this user");
		}

		return service.addResponseInfo(
			request, service.leaveSubmission(submission, user)
		);
	}

	@PutMapping("/event/{eventId}/submission/{subId}/authors/{userId}")
	public SubmissionResponseData addSubmissionAuthor(HttpServletRequest request, @PathVariable String eventId, @PathVariable String subId, @PathVariable String userId) throws PlatformStandardException {
		var event = getEvent(eventId);
		var submission = service.getSubmission(eventId, subId);
		if (submission == null) {
			throw new IllegalArgumentException();// TODO
		}

		checkCanEdit(event, submission);

		UserData user = userController.getSingleUser(userId);

		if (submission.authors().contains(user.id())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"You cannot add a user that's already in a submission");
		}

		return service.addResponseInfo(
			request, service.addSubmissionAuthor(submission, user)
		);
	}

	@PatchMapping("/event/{eventId}/submission/{subId}/image/{type}")
	public SubmissionResponseData editSubmissionImage(HttpServletRequest request, @PathVariable String eventId, @PathVariable String subId, @PathVariable String type, @RequestBody String url) throws PlatformStandardException {
		var event = getEvent(eventId);
		var submission = service.getSubmission(eventId, subId);
		if (submission == null) {
			throw new IllegalArgumentException();// TODO
		}

		checkCanEdit(event, submission);

		var typeEnum = switch (type) {
			case "icon" -> ImageService.SubmissionImageType.ICON;
			case "screenshot" -> ImageService.SubmissionImageType.SCREENSHOT;
			case "test" -> ImageService.SubmissionImageType.TEST;
			case "claim" -> ImageService.SubmissionImageType.CLAIM;
			case "build" -> ImageService.SubmissionImageType.BUILD;
			case null, default -> throw new IllegalArgumentException("Invalid type " + type);
		};

		imageService.downloadSubmissionImage(url, new SubmissionRepository.SubmissionId(eventId, subId), typeEnum);

		return service.addResponseInfo(
			request, service.getSubmission(eventId, subId)
		);
	}

	@DeleteMapping("/event/{eventId}/submission/{subId}")
	public void deleteSubmission(HttpServletRequest request, @PathVariable String eventId, @PathVariable String subId) throws PlatformStandardException {
		var event = getEvent(eventId);
		var submission = service.getSubmission(eventId, subId);
		if (submission == null) {
			throw new IllegalArgumentException();// TODO
		}

		checkCanEdit(event, submission);

		service.deleteSubmission(eventId, subId);
	}

	/**
	 * Checks if a submission is allowed to be edited by the current security subject
	 * @param event The event the submission belongs to
	 */
	private void checkCanEdit(EventData event, SubmissionData submission) throws PlatformStandardException {
		var subject = SecurityUtils.getSubject();
		var can_others = subject.isPermitted(Permissions.Event.EDIT_OTHER_SUBMISSION);
		if (!PermissionUtils.owns(subject, submission) && !can_others) {
			throw new PlatformStandardException(PlatformErrorResponse.ErrorType.PERMISSION_ERROR, "You're not an author of this submission");
		}
		var bypass_phase = subject.isPermitted(Permissions.Event.EDIT_PHASE_BYPASS);
		if (!event.phase().canUpdateSubmission() && !bypass_phase) {
			throw new PlatformStandardException(PlatformErrorResponse.ErrorType.PERMISSION_ERROR, "Submissions are frozen for this event");
		}
	}
}
