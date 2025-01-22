package net.modfest.platform.service;

import net.modfest.platform.pojo.EventData;
import net.modfest.platform.pojo.SubmissionData;
import net.modfest.platform.pojo.UserData;
import net.modfest.platform.repository.SubmissionRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
public class SubmissionService {
	@Autowired
	private SubmissionRepository submissionRepository;

	/**
	 * Retrieve all submissions made by a user
	 * @param filter If non-null, only submissions associated with that event will be returned
	 */
	public Stream<SubmissionData> getSubmissionsFromUser(UserData user, @Nullable EventData filter) {
		var result = submissionRepository.getAll()
			.stream()
			.filter(submission -> submission.authors().contains(user.id()));
		if (filter != null) {
			result = result.filter(s -> s.event().equals(filter.id()));
		}
		return result;
	}

	public Stream<SubmissionData> getSubmissionsFromEvent(EventData event) {
		return submissionRepository.getAll()
				.stream()
				.filter(s -> s.event().equals(event.id()));
	}
}
