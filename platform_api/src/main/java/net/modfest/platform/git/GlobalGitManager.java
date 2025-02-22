package net.modfest.platform.git;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GlobalGitManager {
	// A list of all git roots which are registered as beans
	@Autowired
	private List<GitRootPath> gitPaths;

	public void withScope(GitScope scope, Runnable r) {
		for (var p : gitPaths) {
			p.getScopeManager().setScope(scope);
		}
		r.run();
		for (var p : gitPaths) {
			p.getScopeManager().closeScope();
		}
	}
}
