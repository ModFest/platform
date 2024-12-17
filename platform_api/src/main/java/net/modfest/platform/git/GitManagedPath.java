package net.modfest.platform.git;

import net.modfest.platform.IOConsumer;
import net.modfest.platform.IOFunction;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.EmptyCommitException;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;

public class GitManagedPath implements ManagedPath {
	protected final Git git;
	protected final Path path;
	protected final String subPath;

	public GitManagedPath(Git git, Path path, String subPath) {
		this.git = git;
		this.path = path;
		this.subPath = subPath;
	}

	@Override
	public void write(IOConsumer<Path> runner) throws IOException {
		runner.accept(this.path);
		try {
			// Stage all files in the repo including new files, excluding deleted files
			this.git.add().addFilepattern(subPath).call();
			// Stage all changed files, including deleted files, excluding new files
			this.git.add().addFilepattern(subPath).setUpdate(true).call();
			// Commit the changes
			this.git.commit()
				.setAuthor("Platform", "platform@example.org")
				.setMessage("PLATFORM CHANGE")
				.setAllowEmpty(false)
				.setSign(false)
				.call();
		} catch (EmptyCommitException ignored) {
			// If nothing changed we simply do not commit anything
		} catch (GitAPIException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void read(IOConsumer<Path> runner) throws IOException {
		runner.accept(this.path);
	}

	@Override
	public <R> R withRead(IOFunction<Path,R> runner) throws IOException {
		return runner.apply(this.path);
	}
}
