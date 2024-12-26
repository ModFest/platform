package net.modfest.platform.controller;

import net.modfest.platform.pojo.UserData;
import net.modfest.platform.security.Permissions;
import net.modfest.platform.service.UserService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
	public UserData getSingle(@PathVariable String id) {
		var user = service.getByMfId(id);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such user exists");
		}
		return user;
	}
}
