package net.modfest.platform.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

import java.io.IOException;
import java.nio.file.Path;

public class GitRootPath extends GitManagedDirectory implements ManagedDirectory, AutoCloseable {

	public GitRootPath(Path path) throws IOException {
		super(createGit(path), path, ".");
	}

	private static Git createGit(Path path) throws IOException {
		// Try opening the directory, if it's not a git directory, initialize it as one
		try {
			return Git.open(path.toFile());
		} catch (RepositoryNotFoundException e) {
			try {
				var git = Git.init().setDirectory(path.toFile()).setInitialBranch("main").call();
				git.add().addFilepattern(".").call();
				git.commit()
					.setAuthor("platform", "platform@example.com")
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

	@Override
	public void close() throws Exception {
		git.close();
	}
}
