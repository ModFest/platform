package net.modfest.platform.git;

import net.modfest.platform.configuration.GitConfig;
import net.modfest.platform.misc.PfReentrantLock;
import net.modfest.platform.misc.ThreadLocalHack;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.EmptyCommitException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class GitScopeManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(GitScopeManager.class);
	private final Git git;
	private final GitConfig config;
	private final ThreadLocal<GitScope> gitScopes = new ThreadLocal<>();
	/**
	 * Helps manage which scope is the current "write" scope. Many threads can set
	 * a scope, but once the scope is actually used¹ the thread that owns the scope will
	 * retrieve this lock. The thread that owns the locks owns the writing scope.
	 *
	 * ¹ Often a scope will be set but nothing will actually be written and no locking is needed.
	 *   For example, each request sets a scope.
	 */
	private final PfReentrantLock scopeLock = new PfReentrantLock();

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

	/**
	 * Sets the git commit message for any writes that may or may not be
	 * done during the provided runnable. All writes will be batched into a
	 * single commit.
	 */
	public void runWithScope(GitScope scope, Runnable r) {
		setScope(scope);
		r.run();
		closeScope();
	}

	/**
	 * Activates the current scope to be used for writing
	 */
	void runWithScopedGit(GitFunction r) {
		var threadScope = gitScopes.get();
		try {
			if (!scopeLock.tryLock(10, TimeUnit.SECONDS)) {
				// This is a pretty serious error condition which should not be happening
				// so I'm okay with doing some dirty hacks as long as we get the debug information to fix this
				var owner = scopeLock.getOwner();
				var scope = ThreadLocalHack.getValueOfOtherThread(gitScopes, owner);
				LOGGER.error("Platform failed to obtain git lock. We're blocked on {} and waiting for it to commit: {}", owner, scope);
				throw new RuntimeException("Major timeout error. Waiting on a change to be committed to git before we can start writing.");
			}
			// The lock is now owned by this thread, which means that our
			// thread scope is the active scope.
			r.accept(this.git);
		} catch (GitAPIException | InterruptedException e) {
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
		// We only need to commit if the scope was locked for use in writing
		if (this.scopeLock.isHeldByCurrentThread()) {
			try {
				this.git.commit()
					.setAuthor(config.getUser(), config.getEmail())
					.setMessage(scope.commitMessage())
					.setAllowEmpty(false)
					.setSign(false)
					.call();
			} catch (EmptyCommitException ignored) {
				// If nothing changed we simply do not commit anything
			} catch (GitAPIException e) {
				throw new RuntimeException(e);
			} finally {
				this.scopeLock.unlock();
			}
		}
	}

	public interface GitFunction {
		void accept(Git git) throws GitAPIException;
	}
}
