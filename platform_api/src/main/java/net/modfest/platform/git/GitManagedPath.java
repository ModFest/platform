package net.modfest.platform.git;

import net.modfest.platform.IOConsumer;
import net.modfest.platform.IOFunction;
import net.modfest.platform.configuration.GitConfig;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.EmptyCommitException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Path;

public class GitManagedPath implements ManagedPath {
	@NonNull
	protected final GitConfig config;
	@NonNull
	protected final Git git;
	@NonNull
	protected final Path path;
	@NonNull
	protected final String subPath;

	public GitManagedPath(GitConfig config, Git git, Path path, String subPath) {
		this.config = config;
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
				.setAuthor(config.getUser(), config.getEmail())
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
