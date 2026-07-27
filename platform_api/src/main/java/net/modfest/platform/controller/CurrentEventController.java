package net.modfest.platform.controller;

import net.modfest.platform.misc.PlatformStandardException;
import net.modfest.platform.pojo.CurrentEventData;
import net.modfest.platform.security.Permissions;
import net.modfest.platform.service.CurrentEventService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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
	@RequiresPermissions(Permissions.Event.EDIT_CURRENT)
	public void setCurrentEvent(@RequestBody CurrentEventData data) throws PlatformStandardException {
		service.setCurrentEvent(data);
	}
}
