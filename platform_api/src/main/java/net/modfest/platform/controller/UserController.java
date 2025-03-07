package net.modfest.platform.controller;

import jakarta.servlet.http.HttpServletRequest;
import net.modfest.platform.misc.EventSource;
import net.modfest.platform.misc.ModrinthIdUtils;
import net.modfest.platform.misc.PlatformStandardException;
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
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;
import java.util.HashSet;
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

	@GetMapping(value = "/users/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@RequiresPermissions(Permissions.Users.LIST_ALL)
	public ResponseEntity<SseEmitter> subscribeUserChanges() {
		var emitter = new SseEmitter(-1L);
		// Immediately send something
		// this ensures the client has a reconnection time, but it also
		// ensures the connection has been used. Because if the connection
		// times out (see above) before anything was sent, some funky stuff happens
		try {
			emitter.send(SseEmitter.event().reconnectTime(1000));
		} catch (Throwable e) {}
		EventSource.Subscriber<String> subscriber = (userId) -> {
			try {
				emitter.send(SseEmitter.event().data(userId));
			} catch (Throwable t) {
				// Close connection and cancel subscription
				try {
					emitter.complete();
				} catch (Throwable b) {
					throw new EventSource.CancelSubscriptionException();
				}
			}
		};
		// If the emitter is closed due to any other reason: unsubscribe it
		// *we don't want memory leaks*
		emitter.onCompletion(() -> {
			service.userEventSource().unsubscribe(subscriber);
		});
		// Add it as a subscriber
		service.userEventSource().subscribe(subscriber);

		return ResponseEntity.ok()
			.cacheControl(CacheControl.noCache())
			.body(emitter);
	}

	@PostMapping("/users")
	@RequiresPermissions(Permissions.Users.CREATE)
	public UserData createUser(@RequestBody UserCreateData data) throws PlatformStandardException {
		try {
			var id = service.create(data);
			return service.getByMfId(id);
		} catch (UserService.InvalidModrinthIdException e) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Unknown modrinth id: "+data.modrinthId()
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
	public List<SubmissionResponseData> getUserSubmissions(HttpServletRequest request, @PathVariable String id, @RequestParam(required = false) String eventFilter) {
		var user = getSingleUser(id);
		EventData filter = null;
		if (eventFilter != null) {
			filter = eventService.getEventById(eventFilter);
			if (filter == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event "+eventFilter+" does not exist");
		}
		return submissionService
			.getSubmissionsFromUser(user, filter)
			.map(s -> submissionService.addResponseInfo(request, s))
			.toList();
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

	@PutMapping("/user/{id}/minecraft/{username}")
	public void addUserMinecraft(@PathVariable String id, @PathVariable String username) {
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

		String uuid = service.getMinecraftId(username);
		if (uuid == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "A minecraft profile with that username does not exist");
		}

		// Perform operation
		var accounts = new HashSet<>(user.minecraftAccounts());
		accounts.add(uuid);
		service.save(user.withMinecraftAccounts(accounts));
	}

	@DeleteMapping("/user/{id}/minecraft/{username}")
	public void deleteUserMinecraft(@PathVariable String id, @PathVariable String username) {
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

		String uuid = service.getMinecraftId(username);
		if (uuid == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "A minecraft profile with that username does not exist");
		}

		// Perform operation
		var accounts = new HashSet<>(user.minecraftAccounts());
		if (!accounts.contains(uuid)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "That minecraft account isn't associated with this user");
		}
		accounts.remove(uuid);
		service.save(user.withMinecraftAccounts(accounts));
	}

	@PostMapping("/admin/update_user")
	@RequiresPermissions(Permissions.Users.FORCE_EDIT)
	public void forceUpdateUser(@RequestBody UserData data) {
		var subject = SecurityUtils.getSubject();
		var edit_others = subject.isPermitted(Permissions.Users.EDIT_OTHERS);
		var owns = PermissionUtils.owns(subject, data);

		if (!owns && !edit_others) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may not edit this user");
		}

		service.save(data);
	}
}
