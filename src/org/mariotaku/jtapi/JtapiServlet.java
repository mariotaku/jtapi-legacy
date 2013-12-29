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
import java.util.regex.Matcher;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class JtapiServlet extends HttpServlet implements Constants {

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

	private static void copyStream(final InputStream is, final OutputStream os) throws IOException {
		final int buffer_size = 1024;
		final byte[] bytes = new byte[buffer_size];
		int count = is.read(bytes, 0, buffer_size);
		while (count != -1) {
			os.write(bytes, 0, count);
			count = is.read(bytes, 0, buffer_size);
		}
	}

	private static void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		final String serverName = req.getServerName();
		final String requestUri = req.getRequestURI();
		final Matcher subdomainMatcher = PATTERN_SUB_DOMAIN.matcher(serverName);
		final String subDomain = subdomainMatcher.matches() ? subdomainMatcher.group(ID_SUB_DOMAIN) : null;
		if (subDomain == null) {
			if ("/".equalsIgnoreCase(requestUri)) {
				handleWelcomePage(req, resp);
				return;
			}
			if ("dummy".equalsIgnoreCase(requestUri)) {
				resp.setContentLength(0);
				resp.setStatus(200);
				return;
			}
		}
		final String twitter_domain = subDomain != null ? subDomain + ".twitter.com" : "api.twitter.com";
		final String queryParam = req.getQueryString();
		final String requestUrlString = "https://" + twitter_domain + requestUri
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
			resp.getOutputStream().write(modifyAuthorizePage(req, content));
		} else {
			resp.getOutputStream().write(content);
		}
	}

	private static void handleWelcomePage(final HttpServletRequest req, final HttpServletResponse resp)
			throws IOException {
		final String serverName = req.getServerName();
		resp.setContentType("text/plain");
		final String scheme = req.getScheme();
		final PrintWriter writer = resp.getWriter();
		writer.println("JTAPI " + VERSION_NAME + " is running!");
		writer.println("--------------------------------");
		writer.println("Rest Base URL:		" + scheme + "://" + serverName + "/1.1/");
		writer.println("OAuth Base URL: 	" + scheme + "://" + serverName + "/oauth/");
		writer.println("--------------------------------");
		writer.println("How to use with Twidere:");
		writer.println("Enable \"Ignore SSL Error\", then set above URLs (It\'s better to use HTTPS.)");
		writer.println("--------------------------------");
	}

	private static byte[] modifyAuthorizePage(final HttpServletRequest req, final byte[] content)
			throws UnsupportedEncodingException {
		final String serverName = req.getServerName();
		final String contentString = new String(content, "UTF-8");
		final String replacedContentString = contentString.replace("api.twitter.com", serverName);
		return replacedContentString.getBytes("UTF-8");
	}
}
