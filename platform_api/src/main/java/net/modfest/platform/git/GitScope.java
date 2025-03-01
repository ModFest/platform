package net.modfest.platform.git;

import java.util.concurrent.TimeoutException;

public class GitScope {
	private final String commitMessage;
	private String commitSha;
	private boolean finalized = false;

	public GitScope(String commitMessage) {
		this.commitMessage = commitMessage;
	}

	public String commitMessage() {
		return commitMessage;
	}

	/**
	 * Note! This will cause a blocking wait until this scope is ended. The result
	 * may be null if nothing was committed!
	 * @return The sha of the commit made using this scope.
	 */
	public String commitSha() throws TimeoutException {
		int timeout = 0;
		while (!this.finalized) {
			if (timeout > 10_000) {
				throw new TimeoutException("Timed out waiting for git scope to get finalized");
			}
			try {
				Thread.sleep(50);
				timeout += 50;
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			Thread.onSpinWait();
		}
		return commitSha;
	}

	void setFinalized(String commitMessage) {
		commitSha = commitMessage;
		finalized = true;
	}

	void setFinalized() {
		finalized = true;
	}

	@Override
	public String toString() {
		return "GitScope["+commitMessage+"]("+finalized+", "+commitSha+")";
	}
}
