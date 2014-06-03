package org.mariotaku.jtapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
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
		if (!loadPropertiesFromServletResources(jtapiProperties)) {
			loadPropertiesFromResource(jtapiProperties);
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
		final String overrideAppHost = jtapiProperties.getProperty(KEY_OVERRIDE_APP_HOST, DEFAULT_OVERRIDE_APP_HOST);
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
			} else if ("/dummy".equalsIgnoreCase(requestUri)) {
				resp.setContentLength(0);
				resp.setStatus(200);
				return;
			} else if (requestUri.startsWith("/domain.")) {
				final String domainPart = requestUri.substring("/domain.".length(), requestUri.indexOf("/", 1));
				final String realRequestUri = requestUri.substring(requestUri.indexOf("/", 1));
				final String realSubDomain = Utils.isEmpty(domainPart) ? domainPart : domainPart + ".";
				handleTwitterRequest(req, resp, realSubDomain, realRequestUri);
			} else {
				handleTwitterRequest(req, resp, null, requestUri);
			}
		} else {
			handleTwitterRequest(req, resp, subDomain, requestUri);
		}
	}

	private void handleTwitterRequest(final HttpServletRequest req, final HttpServletResponse resp,
			final String subDomain, final String requestUri) throws IOException {
		final String twitterHost = Utils.isEmpty(subDomain) ? TWITTER_HOST : subDomain + TWITTER_HOST;
		final String queryParam = req.getQueryString();
		final boolean forceSSL = Boolean.parseBoolean(jtapiProperties.getProperty(KEY_FORCE_SSL));
		final String requestScheme = forceSSL || Utils.isRequestHTTPS(req.getScheme()) ? "https" : "http";
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
		for (final Object headerNameObject : Collections.list(headerNames)) {
			final String headerName = (String) headerNameObject;
			final Enumeration<?> headers = req.getHeaders(headerName);
			for (final Object headerObject : Collections.list(headers)) {
				conn.addRequestProperty(headerName, (String) headerObject);
			}
		}
		if ("POST".equals(requestMethod) || "PUT".equals(requestMethod)) {
			Utils.copyStream(req.getInputStream(), conn.getOutputStream());
		}
		resp.setStatus(conn.getResponseCode());
		final Map<String, List<String>> apiHeaders = conn.getHeaderFields();
		for (final String key : apiHeaders.keySet()) {
			if (key == null) {
				continue;
			}
			for (final String value : apiHeaders.get(key)) {
				if (value == null) {
					continue;
				}
				resp.addHeader(key, value);
			}
		}
		resp.addHeader("X-JTAPI-Request-URL", requestUrlString);
		try {
			final InputStream errorStream = conn.getErrorStream();
			if (errorStream != null) {
				Utils.copyStream(errorStream, resp.getOutputStream());
			} else {
				Utils.copyStream(conn.getInputStream(), resp.getOutputStream());
			}
		} catch (final IOException e) {
			// Suppress exceptions thrown
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
		if (Utils.isNormalPort(port)) {
			writer.printf("Rest Base URL:\t\t%s://api.%s/1.1/\n", scheme, serverName);
			writer.printf("OAuth Base URL:\t\t%s://api.%s/oauth/\n", scheme, serverName);
			writer.println("OR");
			writer.printf("Rest Base URL:\t\t%s://%s/domain.api/1.1/\n", scheme, serverName);
			writer.printf("OAuth Base URL:\t\t%s://%s/domain.api/oauth/\n", scheme, serverName);
		} else {
			writer.printf("Rest Base URL:\t\t%s://api.%s:%d/1.1/\n", scheme, serverName, port);
			writer.printf("OAuth Base URL:\t\t%s://api.%s:%d/oauth/\n", scheme, serverName, port);
			writer.println("OR");
			writer.printf("Rest Base URL:\t\t%s://%s:%d/domain.api/1.1/\n", scheme, serverName, port);
			writer.printf("OAuth Base URL:\t\t%s://%s:%d/domain.api/oauth/\n", scheme, serverName, port);
		}
		writer.println("--------------------------------");
		writer.println("How to use with Twidere:");
		writer.println("Enable \"Ignore SSL Error\", then set above URLs (It\'s better to use HTTPS.)");
		writer.println("--------------------------------");
		for (final Object key : jtapiProperties.keySet()) {
			writer.printf("%s: %s\n", key, jtapiProperties.get(key));
		}
		writer.println("--------------------------------");
	}

	private void handleWrongHostPage(final HttpServletRequest req, final HttpServletResponse resp, final String appHost)
			throws IOException {
		resp.setStatus(500);
		resp.setContentType("text/plain");
		final PrintWriter writer = resp.getWriter();
		writer.printf("Wrong host config! %s\n", appHost);
		writer.println("--------------------------------");
		for (final Object key : jtapiProperties.keySet()) {
			writer.printf("%s: %s\n", key, jtapiProperties.get(key));
		}
		writer.println("--------------------------------");
	}

	private boolean loadPropertiesFromResource(final Properties props) {
		try {
			final InputStream inStream = getClass().getResourceAsStream("/config/jtapi.properties");
			if (inStream != null) {
				props.load(inStream);
				return true;
			}
		} catch (final IOException e) {
		}
		return false;
	}

	private boolean loadPropertiesFromServletResources(final Properties props) {
		try {
			final InputStream inStream = getServletContext().getResourceAsStream("/WEB-INF/jtapi.properties");
			if (inStream != null) {
				props.load(inStream);
				return true;
			}
		} catch (final IOException e) {
		}
		return false;
	}
}
