package net.modfest.platform.modrinth.project.version;

public class FilesItem {

	public String filename;
	public Hashes hashes;
	public String url;
	public boolean primary;

	public String getFilename() {
		return filename;
	}

	public Hashes getHashes() {
		return hashes;
	}

	public String getUrl() {
		return url;
	}

	public boolean isPrimary() {
		return primary;
	}
}