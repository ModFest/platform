package net.modfest.platform.repository;

import net.modfest.platform.git.ManagedDirectory;
import net.modfest.platform.misc.JsonUtil;
import net.modfest.platform.pojo.SubmissionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;

@Repository
@Scope("singleton")
public class SubmissionRepository extends AbstractJsonRepository<SubmissionData, SubmissionRepository.SubmissionId> {
	// The @Qualifier("datadir") ensures that spring will give us the object marked as "datadir"
	public SubmissionRepository(@Autowired JsonUtil json, @Qualifier("datadir") ManagedDirectory datadir) {
		super(json, datadir.getSubDirectory("submissions"), "submissions", SubmissionData.class);
	}

	@Override
	protected void validateEdit(SubmissionData previous, SubmissionData current) throws ConstraintViolationException {
		// TODO
	}

	@Override
	protected SubmissionId getId(SubmissionData data) {
		return new SubmissionId(data.event(), data.id());
	}

	@Override
	protected Path getLocation(SubmissionData data) {
		return Path.of(data.event()).resolve(data.id()+".json");
	}

	public record SubmissionId(String event, String id) {

	}
}
