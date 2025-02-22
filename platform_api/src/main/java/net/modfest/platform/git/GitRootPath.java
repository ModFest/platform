package net.modfest.platform.git;

import net.modfest.platform.configuration.GitConfig;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.transport.URIish;
import org.jspecify.annotations.NonNull;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GitRootPath extends GitManagedDirectory implements ManagedDirectory, AutoCloseable {
	private final List<URIish> remotes = new ArrayList<>();

	public GitRootPath(Path path, GitConfig config) throws IOException {
		super(config, createScopeMngr(path, config), path, ".");
	}

	private static GitScopeManager createScopeMngr(Path path, GitConfig conf) throws IOException {
		return new GitScopeManager(createGit(path, conf), conf);
	}

	private static Git createGit(Path path, GitConfig conf) throws IOException {
		// Try opening the directory, if it's not a git directory, initialize it as one
		try {
			return Git.open(path.toFile());
		} catch (RepositoryNotFoundException e) {
			try {
				var git = Git.init().setDirectory(path.toFile()).setInitialBranch("main").call();
				git.add().addFilepattern(".").call();
				git.commit()
					.setAuthor(conf.getUser(), conf.getEmail())
					.setMessage("Initial commit")
					.setSign(false)
					.call();
				return git;
			} catch (InvalidRefNameException e2) {
				throw new RuntimeException(e);
			} catch (GitAPIException e2) {
				throw new IOException(e);
			}
		}
	}

	/**
	 * Adds a remote to which this git repository will be synchronized
	 */
	public void addRemote(@NonNull String uri) throws URISyntaxException, GitAPIException {
		addRemote(new URIish(uri));
	}

	public void addRemote(@NonNull URIish uri) throws GitAPIException {
		this.remotes.add(uri);
		this.setGitRemotes();
	}

	/**
	 * Iterates all the remotes in this git repository. The first parameter represents
	 * the name of the remote, and the second represents the uri it's configured for.
	 */
	private void forAllRemotes(@NonNull RemoteConsumer consumer) throws GitAPIException {
		for (int i = 0; i < remotes.size(); i++) {
			var name = i == 0 ? "platform" : "platform-"+i;
			consumer.accept(name, remotes.get(i));
		}
	}

	/**
	 * Sets the git remotes of the git repositories to the urls configured for this git
	 */
	private void setGitRemotes() throws GitAPIException {
		forAllRemotes((name, uri) -> {
			this.gitScope.getGitUnscoped().remoteSetUrl()
				.setRemoteName(name)
				.setRemoteUri(uri)
				.call();
		});
	}

	@Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
	private void pushGitRepo() throws GitAPIException {
		forAllRemotes((name, _uri) -> {
			this.gitScope.getGitUnscoped().push()
				.setRemote(name)
				.setForce(true)
				.call();
		});
	}

	@Override
	public void close() throws Exception {
		gitScope.getGitUnscoped().close();
	}

	private interface RemoteConsumer {
		void accept(String name, URIish uri) throws GitAPIException;
	}
}
