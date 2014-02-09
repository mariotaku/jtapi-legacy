package org.mariotaku.jtapi;

import com.google.apphosting.api.ApiProxy;

public class AppEngineAccessor {

	public static String getAppId() {
		try {
			return ApiProxy.getCurrentEnvironment().getAppId();
		} catch (final Throwable t) {
			return null;
		}
	}

	public static String getAppIdWithoutPrefix() {
		final String appId = getAppId();
		if (appId == null) return null;
		if (appId.startsWith("s~")) return appId.replaceFirst("s~", "");
		return appId;
	}

}
