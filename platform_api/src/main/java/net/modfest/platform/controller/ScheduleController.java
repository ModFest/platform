package net.modfest.platform.controller;

import net.modfest.platform.misc.PlatformStandardException;
import net.modfest.platform.pojo.ScheduleEntryCreate;
import net.modfest.platform.pojo.ScheduleEntryData;
import net.modfest.platform.security.Permissions;
import net.modfest.platform.service.ScheduleService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ScheduleController {
	@Autowired
	private ScheduleService service;

	@GetMapping("/event/{eventId}/schedule")
	public List<ScheduleEntryData> getSchedule(@PathVariable String eventId) {
		return service.getSchedule(eventId).toList();
	}

	@PostMapping("/schedule")
	@RequiresPermissions(Permissions.Event.EDIT_SCHEDULE)
	public ScheduleEntryData createScheduleEntry(@RequestBody ScheduleEntryCreate createData) throws PlatformStandardException {
		return service.createEntry(createData);
	}

	@PostMapping("/schedule/{scheduleId}")
	@RequiresPermissions(Permissions.Event.EDIT_SCHEDULE)
	public void updateScheduleEntry(@PathVariable String scheduleId, @RequestBody ScheduleEntryCreate createData) throws PlatformStandardException {
		service.updateEntry(scheduleId, createData);
	}

	@DeleteMapping("/schedule/{scheduleId}")
	@RequiresPermissions(Permissions.Event.EDIT_SCHEDULE)
	public void deleteScheduleEntry(@PathVariable String scheduleId) {
		service.removeEntry(scheduleId);
	}
}
