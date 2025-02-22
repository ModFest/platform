package net.modfest.platform.git;

import net.modfest.platform.configuration.GitConfig;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.EmptyCommitException;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GitScopeManager {
	private final Git git;
	private final GitConfig config;
	private final ThreadLocal<GitScope> gitScopes = new ThreadLocal<>();
	private final Lock scopeLock = new ReentrantLock();

	public GitScopeManager(Git git, GitConfig config) {
		this.git = git;
		this.config = config;
	}

	public void setScope(GitScope scope) {
		this.gitScopes.set(scope);
	}

	public void closeScope() {
		finalizeScope(this.gitScopes.get());
		this.gitScopes.remove();
	}

	public void runWithScope(GitScope scope, Runnable r) {
		setScope(scope);
		r.run();
		closeScope();
	}

	void runWithScopedGit(GitFunction r) {
		var threadScope = gitScopes.get();
		try {
			scopeLock.lock();
			// The lock is now owned by this thread, which means that our
			// thread scope is the active scope.
			r.accept(this.git);
		} catch (GitAPIException e) {
			throw new RuntimeException(e);
		} finally {
			if (threadScope == null) {
				// There wasn't any active scope, so we pretend there's an anonymous scope.
				// The anonymous scope will close immediately
				var anonymousScope = new GitScope("PLATFORM CHANGE");
				finalizeScope(anonymousScope);
			}
		}
	}

	/**
	 * Please don't use this for anything that commits files, use {@link #runWithScopedGit(GitFunction)} instead.
	 */
	Git getGitUnscoped() {
		return this.git;
	}

	private void finalizeScope(GitScope scope) {
		try {
			this.git.commit()
				.setAuthor(config.getUser(), config.getEmail())
				.setMessage(scope.commitMessage())
				.setAllowEmpty(false)
				.setSign(false)
				.call();
			this.scopeLock.unlock();
		} catch (EmptyCommitException ignored) {
			// If nothing changed we simply do not commit anything
		} catch (GitAPIException e) {
			throw new RuntimeException(e);
		}
	}

	public interface GitFunction {
		void accept(Git git) throws GitAPIException;
	}
}
