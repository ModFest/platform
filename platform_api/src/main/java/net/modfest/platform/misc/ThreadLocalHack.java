package net.modfest.platform.misc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ThreadLocalHack {
	public static <T> T getValueOfOtherThread(ThreadLocal<T> threadLocal, Thread thread) {
		try {
			Method m = ThreadLocal.class.getDeclaredMethod("get", Thread.class);
			m.setAccessible(true);
			return (T)m.invoke(threadLocal, thread);
		} catch (NoSuchMethodException | InvocationTargetException |
				 IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
