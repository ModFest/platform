package net.modfest.platform.repository;

import net.modfest.platform.git.ManagedDirectory;
import net.modfest.platform.misc.JsonUtil;
import net.modfest.platform.pojo.EventData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;

@Repository
@Scope("singleton")
public class EventRepository extends AbstractJsonRepository<EventData, String> {
	// The @Qualifier("datadir") ensures that spring will give us the object marked as "datadir"
	public EventRepository(@Autowired JsonUtil json, @Qualifier("datadir") ManagedDirectory datadir) {
		super(json, datadir.getSubDirectory("events"), "event", EventData.class);
	}

	@Override
	protected void validateEdit(EventData previous, EventData current) throws ConstraintViolationException {
		// TODO
	}

	@Override
	protected String getId(EventData data) {
		return data.id();
	}

	@Override
	protected Path getLocation(EventData data) {
		return Path.of(data.id()+".json");
	}
}
