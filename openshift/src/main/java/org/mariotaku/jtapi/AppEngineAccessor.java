package org.mariotaku.jtapi;

import java.lang.reflect.Method;

public class AppEngineAccessor {

	public static String getAppId() {
		try {
			Class.forName("com.google.apphosting.api.ApiProxy");
		} catch (final ClassNotFoundException e) {
			return null;
		}
		return AppEngineAccessorImpl.getAppId();
	}

	public static String getAppIdWithoutPrefix() {
		final String appId = getAppId();
		if (appId == null) return null;
		if (appId.startsWith("s~")) return appId.replaceFirst("s~", "");
		return appId;
	}

	private static class AppEngineAccessorImpl {
		public static String getAppId() {
			try {
				final Class<?> apiProxyClass = Class.forName("com.google.apphosting.api.ApiProxy");
				final Method getCurrentEnvironment = apiProxyClass.getMethod("getCurrentEnvironment");
				final Object environment = getCurrentEnvironment.invoke(null);
				final Class<?> environmentClass = environment.getClass();
				final Method getAppId = environmentClass.getMethod("getAppId");
				return (String) getAppId.invoke(environment);
			} catch (final Throwable t) {
				return null;
			}
		}
	}
}
