package tornadofx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicHeader;

import javax.json.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "unused"})
public class Rest extends Controller {
	@Getter private String baseURI;
	@Getter @Setter private HttpHost host;
	@Getter @Setter private CloseableHttpClient client;
	@Getter @Setter private HttpClientContext clientContext;

	private CredentialsProvider credentialsProvider;

	public Rest() {
		clientContext = HttpClientContext.create();
		configure();
	}

	public void setBaseURI(String baseURI) throws URISyntaxException {
		URI uri = new URI(baseURI);

		this.baseURI = uri.getPath();

		if (uri.getHost() != null) {
			String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
			int port = uri.getPort() > -1 ? uri.getPort() : scheme.equals("http") ? 80 : 443;
			this.host = new HttpHost(uri.getHost(), port, scheme);
		}
	}

	public void configure(Consumer<HttpClientBuilder> builderConfigurator) {
		HttpClientBuilder builder = getClientBuilder();
		builderConfigurator.accept(builder);
		client = builder.build();
	}

	public void configure() {
		client = getClientBuilder().build();
	}

	private HttpClientBuilder getClientBuilder() {
		return HttpClients.custom()
			.setDefaultRequestConfig(getDefaultRequestConfig())
			.setDefaultCredentialsProvider(credentialsProvider);
	}

	private RequestConfig getDefaultRequestConfig() {
		return RequestConfig.custom()
			.setSocketTimeout(5000)
			.setConnectTimeout(5000)
			.build();
	}

	public void configureBasicAuth(String username, String password) {
		BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

		credsProvider.setCredentials(
			new AuthScope(host),
			new UsernamePasswordCredentials(username, password));

		AuthCache authCache = new BasicAuthCache();
		authCache.put(host, new BasicScheme());
		clientContext.setAuthCache(authCache);

		setCredentialsProvider(credsProvider);

		configure();
	}

	public <JsonType extends JsonStructure> JsonCall<JsonType> get(String path, Object... params) {
		return get(String.format(path, params));
	}

	public <JsonType extends JsonStructure> JsonCall<JsonType> get(String path) {
		return new JsonCall(new HttpGet(getURI(path)));
	}

	public <JsonType extends JsonStructure> JsonCall<JsonType> put(String path, Object... params) {
		return put(String.format(path, params));
	}

	public <JsonType extends JsonStructure> JsonCall<JsonType> put(String path) {
		return new JsonCall(new HttpPut(getURI(path)));
	}

	public <JsonType extends JsonStructure> JsonCall<JsonType> post(String path, Object... params) {
		return post(String.format(path, params));
	}

	public <JsonType extends JsonStructure> JsonCall<JsonType> post(String path) {
		return new JsonCall(new HttpPost(getURI(path)));
	}

	public <JsonType extends JsonStructure> JsonCall<JsonType> delete(String path, Object... params) {
		return delete(String.format(path, params));
	}

	public <JsonType extends JsonStructure> JsonCall<JsonType> delete(String path) {
		return new JsonCall(new HttpDelete(getURI(path)));
	}

	public URI getURI(String path) {
		try {
			StringBuilder uri = new StringBuilder();

			if (baseURI != null)
				uri.append(baseURI);

			if (uri.toString().endsWith("/") && path.startsWith("/"))
				uri.append(path.substring(1));
			else
				uri.append(path);

			return new URI(uri.toString().replace(" ", "%20"));
		} catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
	}

	@SuppressWarnings("unused")
	public interface JsonResult<T, J> {
		HttpResponse getResponse();
		Exception getError();
		J getData();

		default boolean ok() {
			return getStatusCode() == 200;
		}

		default int getStatusCode() {
			HttpResponse response = getResponse();
			return response == null ? -1 : response.getStatusLine().getStatusCode();
		}

		default Boolean hasError() {
			//noinspection ThrowableResultOfMethodCallIgnored
			return getError() != null;
		}

		default JsonResult<T, J> ifPresent(Consumer<JsonResult<T, J>> ifPresentConsumer) {
			if (getData() != null)
				ifPresentConsumer.accept(this);

			return this;
		}

		default JsonResult<T, J> ifError(BiConsumer<HttpResponse, Exception> ifErrorConsumer) {
			if (hasError())
				ifErrorConsumer.accept(getResponse(), getError());

			return this;
		}
	}

	@SuppressWarnings("unused")
	public static class JsonArrayResult implements JsonResult<JsonArrayResult, JsonArray> {
		@Getter private final HttpResponse response;
		@Getter private JsonArray data;
		@Getter private Exception error;

		public JsonArrayResult(HttpResponse response, Exception error) {
			this.response = response;
			this.error = error;
		}

		public JsonArrayResult(HttpResponse response, JsonArray data) {
			this.response = response;
			this.data = data;
		}

		public Stream<JsonObject> stream() {
			return data.stream().map(value -> (JsonObject) value);
		}

		public <Model extends JsonModel> ObservableList<Model> as(Class<Model> objectClass) {
			if (data == null)
				return FXCollections.emptyObservableList();

			Stream<Model> stream = stream().map(json -> {
				Model model = ReflectionTools.create(objectClass);
				model.updateModel(json);
				return model;
			});

			return FXCollections.observableArrayList(stream.collect(Collectors.toList()));
		}

	}

	public static class JsonEmptyResult implements JsonResult<JsonEmptyResult, Void> {
		@Getter private final HttpResponse response;
		@Getter private Exception error;

		public JsonEmptyResult(HttpResponse response) {
			this.response = response;
		}

		public JsonEmptyResult(HttpResponse response, Exception error) {
			this.response = response;
			this.error = error;
		}

		public Void getData() {
			return null;
		}

	}

	@SuppressWarnings("unused")
	public static class JsonObjectResult implements JsonResult<JsonObjectResult, JsonObject> {
		@Getter private final HttpResponse response;
		@Getter private JsonObject data;
		@Getter private Exception error;

		public JsonObjectResult(HttpResponse response, JsonObject data) {
			this.response = response;
			this.data = data;
		}

		public JsonObjectResult(HttpResponse response, Exception error) {
			this.response = response;
			this.error = error;
		}

		public <Model extends JsonModel> Model as(Class<Model> objectClass) {
			if (data == null)
				return null;

			Model model = ReflectionTools.create(objectClass);
			model.updateModel(data);
			return model;
		}

	}

	@SuppressWarnings("unused")
	public class JsonCall<JsonType extends JsonStructure> {
		private final HttpRequestBase request;
		private JsonStructure data;

		public JsonCall(HttpRequestBase request) {
			this.request = request;
		}

		public JsonCall data(JsonStructure data) {
			this.data = data;
			return this;
		}

		public JsonCall data(JsonObjectBuilder data) {
			this.data = data.build();
			return this;
		}

		public JsonCall data(JsonArrayBuilder data) {
			this.data = data.build();
			return this;
		}

		public JsonCall data(JsonModel data) {
			JsonObjectBuilder builder = Json.createObjectBuilder();
			data.toJSON(builder);
			this.data = builder.build();
			return this;
		}

		public JsonObjectResult one() {
			return (JsonObjectResult) execute((Class<? extends JsonType>) JsonObject.class);
		}

		public JsonArrayResult list() {
			return (JsonArrayResult) execute((Class<? extends JsonType>) JsonArray.class);
		}

		public JsonEmptyResult execute() {
			return (JsonEmptyResult) execute(null);
		}

		private JsonResult execute(Class<? extends JsonType> returnType) {
			HttpResponse response = null;

			try {
				if (data != null && request instanceof HttpEntityEnclosingRequestBase) {
					HttpEntityEnclosingRequestBase heer = (HttpEntityEnclosingRequestBase) request;
					heer.setHeader(new BasicHeader("Content-Type", "application/json"));
					heer.setEntity(new StringEntity(data.toString()));
				}

				response = client.execute(host, request, clientContext);

				if (returnType != null) {
					try (JsonReader reader = Json.createReader(response.getEntity().getContent())) {
						if (returnType.equals(JsonArray.class)) {
							return new JsonArrayResult(response, reader.readArray());
						} else {
							return new JsonObjectResult(response, reader.readObject());
						}
					}
				}

				return new JsonEmptyResult(response);

			} catch (Exception ex) {
				if (returnType == null)
					return new JsonEmptyResult(response, ex);
				else if (returnType.equals(JsonArray.class)) {
					return new JsonArrayResult(response, ex);
				} else {
					return new JsonObjectResult(response, ex);
				}
			}
		}

	}

	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}
}