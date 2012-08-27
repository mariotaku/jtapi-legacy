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
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}

	private void copyStream(InputStream is, OutputStream os) throws IOException {
		final int buffer_size = 1024;
		final byte[] bytes = new byte[buffer_size];
		int count = is.read(bytes, 0, buffer_size);
		while (count != -1) {
			os.write(bytes, 0, count);
			count = is.read(bytes, 0, buffer_size);
		}
	}

	private void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		final String server_name = req.getServerName();
		final String request_uri = req.getRequestURI();
		final Matcher subdomain_matcher = PATTERN_SUB_DOMAIN.matcher(server_name);
		final String sub_domain = subdomain_matcher.matches() ? subdomain_matcher.group(ID_SUB_DOMAIN) : null;
		if (sub_domain != null) {
			final String twitter_domain = sub_domain + ".twitter.com";
			final String query_param = req.getQueryString();
			final String request_url_string = "https://" + twitter_domain + request_uri
					+ (query_param != null ? "?" + query_param : "");
			final URL request_url = new URL(request_url_string);
			final String request_method = req.getMethod();
			final HttpURLConnection conn = (HttpURLConnection) request_url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestMethod(request_method);
			conn.setInstanceFollowRedirects(false);
			final Enumeration<?> header_names = req.getHeaderNames();
			while (header_names.hasMoreElements()) {
				final String header_name = (String) header_names.nextElement();
				conn.addRequestProperty(header_name, req.getHeader(header_name));
			}
			if ("POST".equals(request_method) || "PUT".equals(request_method)) {
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

			if ("api".equals(sub_domain) && request_uri.startsWith("/oauth/authorize")) {
				resp.getOutputStream().write(modifyAuthorizePage(req, content));
			} else {
				resp.getOutputStream().write(content);
			}
		} else {
			handleWelcomePage(req, resp);
		}
	}

	private void handleWelcomePage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		final String server_name = req.getServerName();
		resp.setContentType("text/plain");
		final String scheme = req.getScheme();
		final PrintWriter writer = resp.getWriter();
		writer.println("JTAPI " + VERSION_NAME + " is running!");
		writer.println("--------------------------------");
		writer.println("Rest Base URL:		" + scheme + "://" + "api." + server_name + "/1/");
		writer.println("Oauth Base URL: 	" + scheme + "://" + "api." + server_name + "/oauth/");
		writer.println("Search Base URL:	" + scheme + "://" + "search." + server_name + "/");
		writer.println("Upload Base URL: 	" + scheme + "://" + "upload." + server_name + "/1/");
		writer.println("--------------------------------");
		writer.println("How to use with Twidere:");
		writer.println("Enable \"Ignore SSL Error\", then set above URLs (It\'s better to use HTTPS.)");
		writer.println("--------------------------------");
	}

	private byte[] modifyAuthorizePage(HttpServletRequest req, byte[] content) throws UnsupportedEncodingException {
		final String server_name = req.getServerName();
		final String request_uri = req.getRequestURI();
		final String content_string = new String(content, "UTF-8");
		final String replaced_content_string = content_string.replace("https://api.twitter.com/oauth/authorize",
				"https://" + server_name + request_uri);
		return replaced_content_string.getBytes("UTF-8");
	}
}
