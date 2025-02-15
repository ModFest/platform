package net.modfest.platform.repository;

import net.modfest.platform.git.ManagedDirectory;
import net.modfest.platform.misc.JsonUtil;
import net.modfest.platform.pojo.ScheduleEntryData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;

@Repository
@Scope("singleton")
public class ScheduleRepository extends AbstractJsonRepository<ScheduleEntryData, String> {
	// The @Qualifier("datadir") ensures that spring will give us the object marked as "datadir"
	public ScheduleRepository(@Autowired JsonUtil json, @Qualifier("datadir") ManagedDirectory datadir) {
		super(json, datadir.getSubDirectory("schedule"), "schedule", ScheduleEntryData.class);
	}

	@Override
	protected void validateEdit(ScheduleEntryData previous, ScheduleEntryData current) throws ConstraintViolationException {
		// TODO
	}

	@Override
	protected String getId(ScheduleEntryData data) {
		return data.id();
	}

	@Override
	protected Path getLocation(ScheduleEntryData data) {
		return Path.of(data.event()).resolve(data.id()+".json");
	}
}
