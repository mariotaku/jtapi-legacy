package org.mariotaku.jtapi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class JtapiServlet extends HttpServlet implements Constants {

	private final Properties jtapiProperties = new Properties();

	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
		try {
			final InputStream inStream = getServletContext().getResourceAsStream("/WEB-INF/jtapi.properties");
			if (inStream != null) {
				jtapiProperties.load(inStream);
			}
		} catch (final IOException e) {
		}
	}

	@Override
	protected void doDelete(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}

	@Override
	protected void doHead(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}

	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}

	@Override
	protected void doPut(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}

	private String getAppHost() {
		final String appId = AppEngineAccessor.getAppIdWithoutPrefix();
		final String overrideAppHost = System.getProperty(KEY_OVERRIDE_APP_HOST, DEFAULT_OVERRIDE_APP_HOST);
		if (appId != null) return overrideAppHost.replace(KEYWORD_APPID, appId);
		return overrideAppHost;
	}

	private void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		final String serverName = req.getServerName(), appHost = getAppHost();
		final String requestUri = req.getRequestURI();
		final String subDomain = Utils.replaceLast(serverName, Pattern.quote(appHost), "");
		if (!serverName.endsWith(appHost)) {
			handleWrongHostPage(req, resp, appHost);
			return;
		}
		if (Utils.isEmpty(subDomain)) {
			if ("/".equalsIgnoreCase(requestUri)) {
				handleWelcomePage(req, resp);
				return;
			}
			if ("/dummy".equalsIgnoreCase(requestUri)) {
				resp.setContentLength(0);
				resp.setStatus(200);
				return;
			}
		}
		final String twitterHost = Utils.isEmpty(subDomain) ? TWITTER_HOST : subDomain + TWITTER_HOST;
		final String queryParam = req.getQueryString();
		final String requestScheme = isRequestHTTPS(req.getScheme()) ? "https" : "http";
		final String requestUrlString = requestScheme + "://" + twitterHost + requestUri
				+ (queryParam != null ? "?" + queryParam : "");
		final URL requestUrl = new URL(requestUrlString);
		final String requestMethod = req.getMethod();
		final HttpURLConnection conn = (HttpURLConnection) requestUrl.openConnection();
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestMethod(requestMethod);
		conn.setInstanceFollowRedirects(false);
		final Enumeration<?> headerNames = req.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			final String headerName = (String) headerNames.nextElement();
			conn.addRequestProperty(headerName, req.getHeader(headerName));
		}
		if ("POST".equals(requestMethod) || "PUT".equals(requestMethod)) {
			copyStream(req.getInputStream(), conn.getOutputStream());
		}
		resp.setStatus(conn.getResponseCode());
		resp.setContentType(conn.getContentType());
		final Map<String, List<String>> api_headers = conn.getHeaderFields();
		for (final String key : api_headers.keySet()) {
			for (final String value : api_headers.get(key)) {
				resp.addHeader(key, value);
			}
		}
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		copyStream(conn.getInputStream(), buffer);
		buffer.flush();
		final byte[] content = buffer.toByteArray();

		if (requestUri.equals("/oauth/authorize")) {
			resp.getOutputStream().write(modifyAuthorizePage(req, twitterHost, content));
		} else {
			resp.getOutputStream().write(content);
		}
	}

	private void handleWelcomePage(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		resp.setStatus(200);
		resp.setContentType("text/plain");
		final String serverName = req.getServerName();
		final String scheme = req.getScheme();
		final int port = req.getLocalPort();
		final PrintWriter writer = resp.getWriter();
		writer.printf("JTAPI %s is running!\n", VERSION_NAME);
		writer.println("--------------------------------");
		if (isNormalPort(port)) {
			writer.printf("Rest Base URL:\t\t%s://api.%s/1.1/\n", scheme, serverName);
			writer.printf("OAuth Base URL:\t\t%s://api.%s/oauth/\n", scheme, serverName);
		} else {
			writer.printf("Rest Base URL:\t\t%s://api.%s:%d/1.1/\n", scheme, serverName, port);
			writer.printf("OAuth Base URL:\t\t%s://api.%s:%d/oauth/\n", scheme, serverName, port);
		}
		writer.println("--------------------------------");
		writer.println("How to use with Twidere:");
		writer.println("Enable \"Ignore SSL Error\", then set above URLs (It\'s better to use HTTPS.)");
		writer.println("--------------------------------");
	}

	private void handleWrongHostPage(final HttpServletRequest req, final HttpServletResponse resp, final String appHost)
			throws IOException {
		resp.setStatus(500);
		resp.setContentType("text/plain");
		final PrintWriter writer = resp.getWriter();
		writer.printf("Wrong host config! %s\n", appHost);
	}

	private byte[] modifyAuthorizePage(final HttpServletRequest req, final String twitterHost, final byte[] content)
			throws UnsupportedEncodingException {
		final String serverName = req.getServerName();
		final String contentString = new String(content, "UTF-8");
		final String replacedContentString = contentString.replace(twitterHost, serverName);
		return replacedContentString.getBytes("UTF-8");
	}

	private static void copyStream(final InputStream is, final OutputStream os) throws IOException {
		final int buffer_size = 1024;
		final byte[] bytes = new byte[buffer_size];
		int count = is.read(bytes, 0, buffer_size);
		while (count != -1) {
			os.write(bytes, 0, count);
			count = is.read(bytes, 0, buffer_size);
		}
	}

	private static boolean isNormalPort(final int port) {
		return port == 0 || port == 80 || port == 443;
	}

	private static boolean isRequestHTTPS(final String scheme) {
		return "https".equalsIgnoreCase(scheme);
	}
}
