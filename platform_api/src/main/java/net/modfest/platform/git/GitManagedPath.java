package net.modfest.platform.git;

import net.modfest.platform.configuration.GitConfig;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

public class GitManagedPath implements ManagedPath {
	@NonNull
	protected final GitConfig config;
	@NonNull
	protected final GitScopeManager gitScope;
	@NonNull
	protected final Path path;
	@NonNull
	protected final String subPath;

	public GitManagedPath(GitConfig config, GitScopeManager git, Path path, String subPath) {
		this.config = config;
		this.gitScope = git;
		this.path = path;
		this.subPath = subPath;
	}

	@Override
	public void write(Consumer<Path> runner) {
		this.gitScope.runWithScopedGit(git -> {
			runner.accept(this.path);
			// Stage all files in the repo including new files, excluding deleted files
			git.add().addFilepattern(subPath).call();
			// Stage all changed files, including deleted files, excluding new files
			git.add().addFilepattern(subPath).setUpdate(true).call();
		});
	}

	@Override
	public void read(Consumer<Path> runner) {
		runner.accept(this.path);
	}

	@Override
	public <R> R withRead(Function<Path,R> runner) {
		return runner.apply(this.path);
	}

	@Override
	public void runWithScope(GitScope scope, Runnable r) {
		this.gitScope.runWithScope(scope, r);
	}
}
