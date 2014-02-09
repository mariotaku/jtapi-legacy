package org.mariotaku.jtapi;

public class Utils {

	public static boolean isEmpty(final CharSequence text) {
		return text == null || text.length() == 0;
	}

	public static String replaceLast(final String text, final String regex, final String replacement) {
		if (text == null || regex == null || replacement == null) return text;
		return text.replaceFirst("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
	}

}
