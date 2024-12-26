package net.modfest.platform.git;

import net.modfest.platform.configuration.GitConfig;
import org.eclipse.jgit.api.Git;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GitManagedDirectory extends GitManagedPath implements ManagedDirectory{
	public GitManagedDirectory(GitConfig config, Git git, Path path, String subPath) {
		super(config, git, path, subPath);
	}

	@Override
	public @NonNull ManagedDirectory getSubDirectory(String name) {
		var subpath = this.subPath.equals(".") ? name : this.subPath+"/"+name;
		return new GitManagedDirectory(config, this.git, this.path.resolve(name), subpath);
	}

	@Override
	public @NonNull ManagedFile getSubFile(String name) {
		var subpath = this.subPath.equals(".") ? name : this.subPath+"/"+name;
		return new GitManagedFile(config, this.git, this.path.resolve(name), subpath);
	}

	@Override
	public void createDirectories() throws IOException {
		this.write(Files::createDirectories);
	}

	private static final class GitManagedFile extends GitManagedPath implements ManagedFile{
		public GitManagedFile(GitConfig config, Git git, Path path, String subPath) {
			super(config, git, path, subPath);
		}

		@Override
		public void createParentDirectories() throws IOException {
			this.write(p -> Files.createDirectories(p.getParent()));
		}
	}
}
