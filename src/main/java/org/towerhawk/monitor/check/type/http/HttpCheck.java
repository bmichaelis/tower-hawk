package org.towerhawk.monitor.check.type.http;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.towerhawk.monitor.app.App;
import org.towerhawk.monitor.check.Check;
import org.towerhawk.monitor.check.impl.AbstractCheck;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.monitor.check.threshold.Threshold;
import org.towerhawk.serde.resolver.CheckType;
import org.towerhawk.spring.config.Configuration;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@CheckType("http")
@Setter
public class HttpCheck extends AbstractCheck {
	protected transient HttpClient client;
	protected transient RequestParamCache requestParamCache = new RequestParamCache();
	protected String endpoint;
	protected String method = "GET";
	protected String body;
	protected Map<String, String> headers = new LinkedHashMap<>();
	protected Auth auth = new Auth();
	protected boolean includeResponseInResult = true;

	protected CloseableHttpClient configureHttpClient() {
		HttpClientBuilder builder = HttpClients.custom();

		if (StringUtils.hasText(auth.getUsername())) {
			Credentials creds = new UsernamePasswordCredentials(auth.getUsername(), auth.getPassword());
			CredentialsProvider provider = new BasicCredentialsProvider();
			provider.setCredentials(AuthScope.ANY, creds);
			builder.setDefaultCredentialsProvider(provider);
		}

		return builder.build();
	}

	@Override
	protected void doRun(CheckRun.Builder builder, RunContext context) throws Exception {
		HttpResponse response = null;
		try {
			HttpUriRequest request = createRequest();
			response = client.execute(request);
			Threshold t = getThreshold();
			String asString = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
			if (includeResponseInResult) {
				builder.addContext("httpResponse", asString);
				builder.addContext("connection", String.format("Connection to %s successful", endpoint));
			}
			Object processed = applyTransforms(asString);
			t.evaluate(builder, processed);
		} catch (Exception e) {
			log.error("Unable to complete request for check {}", getFullName(), e);
			builder.critical().error(e);
		} finally {
			if (response != null) {
				try {
					EntityUtils.consume(response.getEntity());
					if (response instanceof Closeable) {
						((Closeable) response).close();
					}
				} catch (IOException e) {
					log.warn("Caught exception while closing response for {}", getFullName(), e);
				}
			}
		}
	}

	@Override
	public void init(Check check, Configuration configuration, App app, String id) {
		if (!StringUtils.hasText(endpoint)) {
			endpoint = configuration.getDefaultHost();
		}
		if (!endpoint.contains("://")) {
			endpoint = "http://" + endpoint;
		}
		requestParamCache.setUri(endpoint);
		requestParamCache.setHttpMethod(method);
		requestParamCache.setHeaderArray(headers);

		client = configureHttpClient();

		super.init(check, configuration, app, id);
	}

	protected HttpUriRequest createRequest() {
		HttpUriRequest request = createHttpUriRequest(requestParamCache.getUri(), requestParamCache.getHttpMethod());
		request.setHeaders(requestParamCache.getHeaderArray());
		if (request instanceof HttpEntityEnclosingRequest) {
			((HttpEntityEnclosingRequest) request).setEntity(new StringEntity(body, StandardCharsets.UTF_8));
		}
		return request;
	}

	protected HttpUriRequest createHttpUriRequest(URI uri, HttpMethod httpMethod) {
		switch (httpMethod) {
			case GET:
				return new HttpGet(uri);
			case HEAD:
				return new HttpHead(uri);
			case POST:
				return new HttpPost(uri);
			case PUT:
				return new HttpPut(uri);
			case PATCH:
				return new HttpPatch(uri);
			case DELETE:
				return new HttpDelete(uri);
			case OPTIONS:
				return new HttpOptions(uri);
			case TRACE:
				return new HttpTrace(uri);
			default:
				throw new IllegalArgumentException("Invalid HTTP method: " + httpMethod);
		}
	}

	@Getter
	protected class RequestParamCache {
		private URI uri;
		private HttpMethod httpMethod;
		private Header[] headerArray;

		public void setUri(String endpoint) {
			try {
				uri = new URI(endpoint);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("endpoint '" + endpoint + "' cannot be converted to URI", e);
			}
		}

		public void setHttpMethod(String method) {
			httpMethod = HttpMethod.resolve(method.toUpperCase());
		}

		public void setHeaderArray(Map<String, String> headers) {
			headerArray = headers.entrySet().stream().map(
					e -> new BasicHeader(e.getKey(), e.getValue()))
					.collect(Collectors.toList())
					.toArray(new Header[headers.size()]);
		}
	}

	@Override
	public void close() throws IOException {
		super.close();
		try {
			if (client instanceof Closeable) {
				((Closeable) client).close();
			}
		} catch (Exception e) {
			log.error("Caught exception while closing " + getFullName(), e);
		}
	}

	@Getter
	@Setter
	public class Auth {
		protected String username;
		protected String password;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers.putAll(headers);
	}
}
