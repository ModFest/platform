package net.modfest.platform.repository;

import net.modfest.platform.pojo.CurrentEventData;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Repository;

/**
 * Yep, this is a whole class just dedicated to storing which event is currently happening
 */
@Repository
public class CurrentEventRepository extends AbstractSingleJsonStorage<CurrentEventData> {
	protected CurrentEventRepository() {
		super("currentEvent.json", CurrentEventData.class);
	}

	@Override
	protected @NonNull CurrentEventData createDefault() {
		return new CurrentEventData(null);
	}
}
