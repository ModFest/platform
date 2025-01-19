package net.modfest.platform.repository;

import net.modfest.platform.git.ManagedDirectory;
import net.modfest.platform.pojo.EventData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("singleton")
public class EventRepository extends AbstractJsonRepository<EventData> {
	public EventRepository(@Qualifier("datadir") ManagedDirectory datadir) {
		super(datadir.getSubDirectory("events"), "event", EventData.class);
	}

	@Override
	protected void validateEdit(EventData previous, EventData current) throws ConstraintViolationException {
		// TODO
	}
}
