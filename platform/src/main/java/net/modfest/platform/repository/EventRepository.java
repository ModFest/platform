package net.modfest.platform.repository;

import net.modfest.platform.pojo.EventData;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("singleton")
public class EventRepository extends AbstractJsonRepository<EventData> {
	public EventRepository() {
		super("events", EventData.class);
	}
}
