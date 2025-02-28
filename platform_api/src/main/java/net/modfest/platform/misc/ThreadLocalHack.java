package net.modfest.platform.misc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ThreadLocalHack {
	public static <T> T getValueOfOtherThread(ThreadLocal<T> threadLocal, Thread thread) {
		try {
			Method m = ThreadLocal.class.getDeclaredMethod("get", Thread.class);
			m.setAccessible(true);
			return (T)m.invoke(threadLocal, thread);
//			// https://stackoverflow.com/questions/5180114/threadlocal-value-access-across-different-threads
//			Field field = Thread.class.getDeclaredField("threadLocals");
//			field.setAccessible(true);
//			Object map = field.get(thread);
//
//			Method method = null;
//			method = Class.forName("java.lang.ThreadLocal$ThreadLocalMap").getDeclaredMethod("getEntry", ThreadLocal.class);
//
//			method.setAccessible(true);
//			WeakReference<?> entry = (WeakReference<?>)method.invoke(map, threadLocal);
//
//			Field valueField = Class.forName("java.lang.ThreadLocal$ThreadLocalMap$Entry").getDeclaredField("value");
//			valueField.setAccessible(true);
//			Object value = valueField.get(entry);
//
//			return (T)value;
		} catch (NoSuchMethodException | InvocationTargetException |
				 IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
