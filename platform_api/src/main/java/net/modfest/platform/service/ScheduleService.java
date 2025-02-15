package net.modfest.platform.service;

import net.modfest.platform.misc.MfUserId;
import net.modfest.platform.misc.PlatformStandardException;
import net.modfest.platform.pojo.ScheduleEntryCreate;
import net.modfest.platform.pojo.ScheduleEntryData;
import net.modfest.platform.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.stream.Stream;

import static net.modfest.platform.pojo.PlatformErrorResponse.ErrorType.EVENT_NO_EXIST;
import static net.modfest.platform.pojo.PlatformErrorResponse.ErrorType.USER_NO_EXIST;

@Service
public class ScheduleService {
	@Autowired
	private ScheduleRepository repository;
	@Autowired
	private EventService eventService;
	@Autowired
	private UserService userService;

	public ScheduleEntryData createEntry(ScheduleEntryCreate createData) throws PlatformStandardException {
		var eventId = Objects.requireNonNull(createData.event());
		if (eventService.getEventById(eventId) == null) {
			throw new PlatformStandardException(EVENT_NO_EXIST, eventId);
		}

		// Create a new id, we reuse the user id generation
		String id;
		do {
			id = MfUserId.generateRandom();
		} while (repository.contains(id));

		var entry = new ScheduleEntryData(
			id,
			eventId,
			Objects.requireNonNull(createData.title()),
			Objects.requireNonNull(createData.type()),
			Objects.requireNonNull(createData.description()),
			Objects.requireNonNull(createData.location()),
			Objects.requireNonNull(createData.authors()),
			Objects.requireNonNull(createData.start()),
			Objects.requireNonNull(createData.end())
		);

		for (var a : createData.authors()) {
			if (userService.getByMfId(a) == null) {
				throw new PlatformStandardException(USER_NO_EXIST, a);
			}
		}

		repository.save(entry);

		return entry;
	}

	public Stream<ScheduleEntryData> getSchedule(String event) {
		return repository.getAll().stream().filter(schedule -> schedule.event().equals(event));
	}

	public void updateEntry(String scheduleId, ScheduleEntryCreate updateData) throws PlatformStandardException {
		var oldData = repository.get(scheduleId);
		if (oldData == null) {
			throw new NullPointerException();
		}

		if (updateData.event() != null && eventService.getEventById(updateData.event()) == null) {
			throw new PlatformStandardException(EVENT_NO_EXIST, updateData.event());
		}

		if (updateData.authors() != null) {
			for (var a : updateData.authors()) {
				if (userService.getByMfId(a) == null) {
					throw new PlatformStandardException(USER_NO_EXIST, a);
				}
			}
		}

		var newData = new ScheduleEntryData(
			scheduleId,
			updateData.event() == null ? oldData.event() : updateData.event(),
			updateData.title() == null ? oldData.title() : updateData.title(),
			updateData.type() == null ? oldData.type() : updateData.type(),
			updateData.location() == null ? oldData.location() : updateData.location(),
			updateData.description() == null ? oldData.description() : updateData.description(),
			updateData.authors() == null ? oldData.authors() : updateData.authors(),
			updateData.start() == null ? oldData.start() : updateData.start(),
			updateData.end() == null ? oldData.end() : updateData.end()
		);

		repository.save(newData);
	}

	public void removeEntry(String scheduleId) {
		repository.delete(scheduleId);
	}
}
