package org.mariotaku.jtapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

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

	private void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		final String server_name = req.getServerName();
		final String request_uri = req.getRequestURI();
		final Matcher subdomain_matcher = PATTERN_SUB_DOMAIN.matcher(server_name);
		final String sub_domain = subdomain_matcher.matches() ? subdomain_matcher.group(ID_SUB_DOMAIN) : null;
		if (sub_domain != null) {
			final String twitter_domain = sub_domain + ".twitter.com";
			final String query_param = req.getQueryString();
			final String request_url = "https://" + twitter_domain + request_uri + (query_param != null ? "?" + query_param : "");
			final URLFetchService service = URLFetchServiceFactory.getURLFetchService();
			final BufferedReader reader = req.getReader();
			final StringBuilder builder = new StringBuilder();
			int c;
			while ((c = reader.read()) != -1) {
				if (c != -1) {
					builder.append((char)c);
				}
			}
			final HTTPRequest request = new HTTPRequest(new URL(request_url), HTTPMethod.valueOf(req.getMethod()));
			request.setPayload(builder.toString().getBytes());
			final Enumeration<?> header_names = req.getHeaderNames();
			while (header_names.hasMoreElements()) {
				final String headerName = (String) header_names.nextElement();
				final HTTPHeader header = new HTTPHeader(headerName, req.getHeader(headerName));
				request.addHeader(header);
			}
			final HTTPResponse api_response = service.fetch(request);
			final List<HTTPHeader> api_headers = api_response.getHeadersUncombined();
			for (final HTTPHeader api_header : api_headers) {
				resp.addHeader(api_header.getName(), api_header.getValue());
			}
			resp.setStatus(api_response.getResponseCode());
			final byte[] content = api_response.getContent();
			
			if ("api".equals(sub_domain) && request_uri.startsWith("/oauth/authorize")) {
				final String content_string = new String(content, "UTF-8");
				final String replaced_content_string = content_string.replace("https://api.twitter.com/oauth/authorize", "https://" + server_name + request_uri);
				final byte[] replaced_content = replaced_content_string.getBytes("UTF-8");
				resp.getOutputStream().write(replaced_content);
			} else {
				resp.getOutputStream().write(api_response.getContent());
			}
		} else {
			resp.setContentType("text/plain");
			final String scheme = req.getScheme();
			final PrintWriter writer = resp.getWriter();
			writer.println("JTAPI " + VERSION_NAME + " is running!");
			writer.println("--------------------------------");
			writer.println("Rest Base URL:		" + scheme + "://" + "api." + server_name + "/1/");
			writer.println("Oauth Base URL: 	" + scheme + "://" + "api." + server_name + "/oauth/");
			writer.println("Search Base URL:	" + scheme + "://" + "search." + server_name + "/");
			writer.println("Upload Base URL: 	" + scheme + "://" + "upload." + server_name + "/1/ (Not working)");
			writer.println("--------------------------------");
			writer.println("How to use with Twidere:");
			writer.println("Enable \"Ignore SSL Error\", then set above URLs (It\'s better to use HTTPS.)");
			writer.println("--------------------------------");
		}
	}
}
