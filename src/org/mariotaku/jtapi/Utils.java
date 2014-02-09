package org.mariotaku.jtapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {

	public static boolean isEmpty(final CharSequence text) {
		return text == null || text.length() == 0;
	}

	public static String replaceLast(final String text, final String regex, final String replacement) {
		if (text == null || regex == null || replacement == null) return text;
		return text.replaceFirst("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
	}

	static void copyStream(final InputStream is, final OutputStream os) throws IOException {
		final int buffer_size = 1024;
		final byte[] bytes = new byte[buffer_size];
		int count = is.read(bytes, 0, buffer_size);
		while (count != -1) {
			os.write(bytes, 0, count);
			count = is.read(bytes, 0, buffer_size);
		}
	}

	static boolean isNormalPort(final int port) {
		return port == 0 || port == 80 || port == 443;
	}

	static boolean isRequestHTTPS(final String scheme) {
		return "https".equalsIgnoreCase(scheme);
	}

}
