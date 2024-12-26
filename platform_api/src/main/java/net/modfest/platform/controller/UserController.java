package net.modfest.platform.controller;

import net.modfest.platform.pojo.UserData;
import net.modfest.platform.pojo.UserPatchData;
import net.modfest.platform.security.PermissionUtils;
import net.modfest.platform.security.Permissions;
import net.modfest.platform.service.UserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Collection;

@RestController
public class UserController {
	@Autowired
	private UserService service;

	@GetMapping("/users")
	@RequiresPermissions(Permissions.Users.LIST_ALL)
	public Collection<UserData> listAll() {
		return service.getAll();
	}

	@GetMapping("/user/{id}")
	public UserData getSingleUser(@PathVariable String id) {
		var user = service.getByMfId(id);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such user exists");
		}
		return user;
	}

	@PatchMapping("/user/{id}")
	public void editUserData(@PathVariable String id, @RequestBody UserPatchData data) throws IOException {
		var user = service.getByMfId(id);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such user exists");
		}

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
