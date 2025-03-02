package net.modfest.platform.misc;

import java.util.concurrent.locks.ReentrantLock;

public class PfReentrantLock extends ReentrantLock {
	/**
	 * Exposed for debugging purposes
	 */
	public Thread getOwner() {
		return super.getOwner();
	}
}
