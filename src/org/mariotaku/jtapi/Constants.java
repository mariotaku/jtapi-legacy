package org.mariotaku.jtapi;

import java.util.regex.Pattern;

public interface Constants {

	public static final String VERSION_NAME = "0.3";

	public static final Pattern PATTERN_SUB_DOMAIN = Pattern.compile("([\\d\\w\\-]+)\\.([\\d\\w\\-]+)\\.appspot\\.com");
	public static final int ID_SUB_DOMAIN = 1;
}
