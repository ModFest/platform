package net.modfest.platform.misc;

import java.util.HashSet;
import java.util.Set;

public class EventSource<T> {
	private final Set<Subscriber<T>> subscribers = new HashSet<>();

	public void unsubscribe(Subscriber<T> subscriber) {
		subscribers.remove(subscriber);
	}

	public void subscribe(Subscriber<T> subscriber) {
		subscribers.add(subscriber);
	}

	/**
	 * Emit a new event
	 */
	public void emit(T event) {
		var iterator = subscribers.iterator();
		while (iterator.hasNext()) {
			var next = iterator.next();
			try {
				next.accept(event);
			} catch (CancelSubscriptionException e) {
				iterator.remove();
			}
		}
	}

	public interface Subscriber<T> {
		/**
		 * Called when an event is submitted. If {@link CancelSubscriptionException} is thrown, this
		 * subscriber will cease to receive events.
		 *
		 * @implNote
		 * This callback will be called inside critical locations. It's expected
		 * that implementors gracefully handle exceptions themselves. If exceptions are not gracefully handled,
		 * this can cause a cascade of errors
		 */
		void accept(T event) throws CancelSubscriptionException;
	}

	public static class CancelSubscriptionException extends Exception {
	}
}
