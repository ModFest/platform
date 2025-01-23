package net.modfest.platform.controller;

import net.modfest.platform.misc.ModrinthIdUtils;
import net.modfest.platform.pojo.*;
import net.modfest.platform.security.PermissionUtils;
import net.modfest.platform.security.Permissions;
import net.modfest.platform.service.EventService;
import net.modfest.platform.service.SubmissionService;
import net.modfest.platform.service.UserService;
import nl.theepicblock.dukerinth.ModrinthApi;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@RestController
public class UserController {
	@Autowired
	private UserService service;
	@Autowired
	private EventService eventService;
	@Autowired
	private SubmissionService submissionService;
	@Autowired
	private ModrinthApi modrinthApi;

	@GetMapping("/users")
	@RequiresPermissions(Permissions.Users.LIST_ALL)
	public Collection<UserData> listAll() {
		return service.getAll();
	}

	@PutMapping("/users")
	@RequiresPermissions(Permissions.Users.CREATE)
	public void createUser(@RequestBody UserCreateData data) {
		try {
			service.create(data);
		} catch (UserService.InvalidModrinthIdException e) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Unknown modrinth id: "+data.modrinthId()
			);
		} catch (UserService.UserAlreadyExistsException e) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				e.getMessage()
			);
		}
	}

	@GetMapping("/user/{id}")
	public UserData getSingleUser(@PathVariable String id) {
		if (Objects.equals(id, "@me")) {
			var principal = SecurityUtils.getSubject().getPrincipal();
			if (principal instanceof UserData user) {
				return user;
			} else {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is not associated with a user");
			}
		}

		UserData user;
		if (id.startsWith("mr:")) {
			// I will allow people to put modrinth slugs in here,
			// let's hope nobody uses them for persistence :(
			var mrId = id.substring(3);
			if (!ModrinthIdUtils.isValidModrinthId(mrId)) {
				var apiResult = modrinthApi.users().getUser(mrId);
				if (apiResult != null) mrId = apiResult.id;
			}
			user = service.getByModrinthId(mrId);
		} else if (id.startsWith("dc:")) {
			user = service.getByDiscordId(id.substring(3));
		} else {
			user = service.getByMfId(id);
		}

		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such user exists");
		}
		return user;
	}

	@GetMapping("/user/{id}/submissions")
	public List<SubmissionData> getUserSubmissions(@PathVariable String id, @RequestParam(required = false) String eventFilter) {
		var user = getSingleUser(id);
		EventData filter = null;
		if (eventFilter != null) {
			filter = eventService.getEventById(eventFilter);
			if (filter == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event "+eventFilter+" does not exist");
		}
		return submissionService.getSubmissionsFromUser(user, filter).toList();
	}

	@PatchMapping("/user/{id}")
	public void editUserData(@PathVariable String id, @RequestBody UserPatchData data) {
		var user = getSingleUser(id);

		// Check permissions
		// In order for the request to be allowed, the person making the request needs
		// to either be editing their own data, or they need to have the EDIT_OTHERS permission
		var subject = SecurityUtils.getSubject();
		var edit_others = subject.isPermitted(Permissions.Users.EDIT_OTHERS);
		var owns = PermissionUtils.owns(subject, user);

		if (!owns && !edit_others) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may not edit this user");
		}

		// Perform operation
		var newUser = user;
		if (data.bio() != null) {
			newUser = newUser.withBio(data.bio());
		}
		if (data.name() != null) {
			newUser = newUser.withName(data.name());
		}
		if (data.pronouns() != null) {
			newUser = newUser.withPronouns(data.pronouns());
		}

		service.save(newUser);
	}
}
