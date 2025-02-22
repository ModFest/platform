package net.modfest.platform.git;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GlobalGitManager {
	// A list of all git roots which are registered as beans
	@Autowired
	private List<GitRootPath> gitPaths;

	public void setScope(GitScope scope) {
		for (var p : gitPaths) {
			p.getScopeManager().setScope(scope);
		}
	}

	public void closeScope() {
		for (var p : gitPaths) {
			p.getScopeManager().closeScope();
		}
	}

	public void withScope(GitScope scope, Runnable r) {
		setScope(scope);
		r.run();
		closeScope();
	}
}
