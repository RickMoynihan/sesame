/* 
 * Licensed to Aduna under one or more contributor license agreements.  
 * See the NOTICE.txt file distributed with this work for additional 
 * information regarding copyright ownership. 
 *
 * Aduna licenses this file to you under the terms of the Aduna BSD 
 * License (the "License"); you may not use this file except in compliance 
 * with the License. See the LICENSE.txt file distributed with this work 
 * for the full License.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.openrdf.http.client;

import static org.openrdf.http.protocol.Protocol.ACCEPT_PARAM_NAME;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrdf.http.protocol.Protocol;
import org.openrdf.http.protocol.UnauthorizedException;
import org.openrdf.http.protocol.error.ErrorInfo;
import org.openrdf.http.protocol.error.ErrorType;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.Binding;
import org.openrdf.query.Dataset;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryInterruptedException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.UnsupportedQueryLanguageException;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.query.resultio.BooleanQueryResultFormat;
import org.openrdf.query.resultio.BooleanQueryResultParser;
import org.openrdf.query.resultio.BooleanQueryResultParserRegistry;
import org.openrdf.query.resultio.QueryResultIO;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultParser;
import org.openrdf.query.resultio.TupleQueryResultParserRegistry;
import org.openrdf.query.resultio.UnsupportedQueryResultFormatException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.openrdf.rio.helpers.ParseErrorLogger;
import org.openrdf.rio.helpers.StatementCollector;

/**
 * Low-level HTTP client for Sesame's HTTP protocol. Methods correspond directly
 * to the functionality offered by the protocol.
 * 
 * @author Herko ter Horst
 * @author Arjohn Kampman
 */
public class HTTPClient {

	/*-----------*
	 * Constants *
	 *-----------*/

	final Logger logger = LoggerFactory.getLogger(this.getClass());

	/*-----------*
	 * Variables *
	 *-----------*/

	private ValueFactory valueFactory;
	
	private String queryURL;
	private String updateURL;
	
	private final MultiThreadedHttpConnectionManager manager;

	protected final HttpClient httpClient;
	
	private final ExecutorService executor = Executors.newCachedThreadPool();

	private AuthScope authScope;

	private ParserConfig parserConfig = new ParserConfig();

	private TupleQueryResultFormat preferredTQRFormat = TupleQueryResultFormat.BINARY;

	private BooleanQueryResultFormat preferredBQRFormat = BooleanQueryResultFormat.TEXT;

	private RDFFormat preferredRDFFormat = RDFFormat.TURTLE;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public HTTPClient() {
		valueFactory = ValueFactoryImpl.getInstance();

		// Use MultiThreadedHttpConnectionManager to allow concurrent access on
		// HttpClient
		manager = new MultiThreadedHttpConnectionManager();

		// Allow 20 concurrent connections to the same host (default is 2)
		HttpConnectionManagerParams params = new HttpConnectionManagerParams();
		params.setDefaultMaxConnectionsPerHost(20);
		manager.setParams(params);

		
		httpClient = new HttpClient(manager);

		configureProxySettings(httpClient);
	}

	/*-----------------*
	 * Get/set methods *
	 *-----------------*/

	public void shutDown() {
		manager.shutdown();
		executor.shutdown();
	}

	protected final HttpClient getHttpClient() {
		return httpClient;
	}

	public void setValueFactory(ValueFactory valueFactory) {
		this.valueFactory = valueFactory;
	}

	public ValueFactory getValueFactory() {
		return valueFactory;
	}
	
	public void setQueryURL(String queryURL) {
		if (queryURL == null) {
			throw new IllegalArgumentException("queryURL must not be null");
		}
		this.queryURL = queryURL;
	}
	
	// TODO set updateURL

	/**
	 * Sets the preferred format for encoding tuple query results. The
	 * {@link TupleQueryResultFormat#BINARY binary} format is preferred by
	 * default.
	 * 
	 * @param format
	 *        The preferred {@link TupleQueryResultFormat}, or <tt>null</tt> to
	 *        indicate no specific format is preferred.
	 */
	public void setPreferredTupleQueryResultFormat(TupleQueryResultFormat format) {
		preferredTQRFormat = format;
	}

	/**
	 * Gets the preferred {@link TupleQueryResultFormat} for encoding tuple query
	 * results.
	 * 
	 * @return The preferred format, of <tt>null</tt> if no specific format is
	 *         preferred.
	 */
	public TupleQueryResultFormat getPreferredTupleQueryResultFormat() {
		return preferredTQRFormat;
	}

	/**
	 * Sets the preferred format for encoding RDF documents. The
	 * {@link RDFFormat#TURTLE Turtle} format is preferred by default.
	 * 
	 * @param format
	 *        The preferred {@link RDFFormat}, or <tt>null</tt> to indicate no
	 *        specific format is preferred.
	 */
	public void setPreferredRDFFormat(RDFFormat format) {
		preferredRDFFormat = format;
	}

	/**
	 * Gets the preferred {@link RDFFormat} for encoding RDF documents.
	 * 
	 * @return The preferred format, of <tt>null</tt> if no specific format is
	 *         preferred.
	 */
	public RDFFormat getPreferredRDFFormat() {
		return preferredRDFFormat;
	}

	/**
	 * Sets the preferred format for encoding boolean query results. The
	 * {@link BooleanQueryResultFormat#TEXT binary} format is preferred by
	 * default.
	 * 
	 * @param format
	 *        The preferred {@link BooleanQueryResultFormat}, or <tt>null</tt> to
	 *        indicate no specific format is preferred.
	 */
	public void setPreferredBooleanQueryResultFormat(BooleanQueryResultFormat format) {
		preferredBQRFormat = format;
	}

	/**
	 * Gets the preferred {@link BooleanQueryResultFormat} for encoding boolean
	 * query results.
	 * 
	 * @return The preferred format, of <tt>null</tt> if no specific format is
	 *         preferred.
	 */
	public BooleanQueryResultFormat getPreferredBooleanQueryResultFormat() {
		return preferredBQRFormat;
	}

	/**
	 * Set the username and password for authentication with the remote server.
	 * 
	 * @param username
	 *        the username
	 * @param password
	 *        the password
	 */
	public void setUsernameAndPassword(String username, String password) {
//		checkServerURL();

		if (username != null && password != null) {
			logger.debug("Setting username '{}' and password for server at {}.", username, queryURL);
			try {
				URL server = new URL(getQueryURL());
				authScope = new AuthScope(server.getHost(), AuthScope.ANY_PORT);
				httpClient.getState().setCredentials(authScope,
						new UsernamePasswordCredentials(username, password));
				httpClient.getParams().setAuthenticationPreemptive(true);
			}
			catch (MalformedURLException e) {
				logger.warn("Unable to set username and password for malformed URL {}", queryURL);
			}
		}
		else {
			authScope = null;
			httpClient.getState().clearCredentials();
			httpClient.getParams().setAuthenticationPreemptive(false);
		}
	}
	
	
	protected void execute(Runnable command) {
		executor.execute(command);
	}
	
	protected String getQueryURL() {
		return queryURL;
	}

	protected String getUpdateURL() {
		return updateURL;
	}

	/*------------------*
	 * Query evaluation *
	 *------------------*/

	public TupleQueryResult sendTupleQuery(QueryLanguage ql, String query, Dataset dataset,
			boolean includeInferred, Binding... bindings)
		throws IOException, RepositoryException, MalformedQueryException, UnauthorizedException,
		QueryInterruptedException
	{
		return sendTupleQuery(ql, query, null, dataset, includeInferred, 0, bindings);
	}

	public TupleQueryResult sendTupleQuery(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, Binding... bindings)
		throws IOException, RepositoryException, MalformedQueryException, UnauthorizedException,
		QueryInterruptedException
	{
		HttpMethod method = getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime, bindings);
		return getBackgroundTupleQueryResult(method);
	}


	public void sendTupleQuery(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, TupleQueryResultHandler handler, Binding... bindings)
		throws IOException, TupleQueryResultHandlerException, RepositoryException, MalformedQueryException,
		UnauthorizedException, QueryInterruptedException
	{
		HttpMethod method = getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime, bindings);
		getTupleQueryResult(method, handler);
	}

	public void sendUpdate(QueryLanguage ql, String update, String baseURI, Dataset dataset,
			boolean includeInferred, Binding... bindings)
		throws IOException, RepositoryException, MalformedQueryException, UnauthorizedException,
		QueryInterruptedException
	{
		HttpMethod method = getUpdateMethod(ql, update, baseURI, dataset, includeInferred, bindings);

		try {
			int httpCode = httpClient.executeMethod(method);

			if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new UnauthorizedException();
			}
			else if (!is2xx(httpCode)) {
				ErrorInfo errInfo = ErrorInfo.parse(method.getResponseBodyAsString());

				if (errInfo.getErrorType() == ErrorType.MALFORMED_QUERY) {
					throw new MalformedQueryException(errInfo.getErrorMessage());
				}
				else {
					throw new RepositoryException("Failed to execute update: " + errInfo);
				}
			}
		}
		finally {
			releaseConnection(method);
		}
	}

	public GraphQueryResult sendGraphQuery(QueryLanguage ql, String query, Dataset dataset,
			boolean includeInferred, Binding... bindings)
		throws IOException, RepositoryException, MalformedQueryException, UnauthorizedException,
		QueryInterruptedException
	{
		return sendGraphQuery(ql, query, null, dataset, includeInferred, 0, bindings);
	}

	public GraphQueryResult sendGraphQuery(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, Binding... bindings)
		throws IOException, RepositoryException, MalformedQueryException, UnauthorizedException,
		QueryInterruptedException
	{
		try {
			StatementCollector collector = new StatementCollector();
			sendGraphQuery(ql, query, baseURI, dataset, includeInferred, maxQueryTime, collector, bindings);
			return new GraphQueryResultImpl(collector.getNamespaces(), collector.getStatements());
		}
		catch (RDFHandlerException e) {
			// Found a bug in TupleQueryResultBuilder?
			throw new RuntimeException(e);
		}
	}

	public void sendGraphQuery(QueryLanguage ql, String query, Dataset dataset, boolean includeInferred,
			RDFHandler handler, Binding... bindings)
		throws IOException, RDFHandlerException, RepositoryException, MalformedQueryException,
		UnauthorizedException, QueryInterruptedException
	{
		sendGraphQuery(ql, query, null, dataset, includeInferred, 0, handler, bindings);
	}

	public void sendGraphQuery(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, RDFHandler handler, Binding... bindings)
		throws IOException, RDFHandlerException, RepositoryException, MalformedQueryException,
		UnauthorizedException, QueryInterruptedException
	{
		HttpMethod method = getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime, bindings);

		try {
			getRDF(method, handler, false);
		}
		finally {
			releaseConnection(method);
		}
	}

	public boolean sendBooleanQuery(QueryLanguage ql, String query, Dataset dataset, boolean includeInferred,
			Binding... bindings)
		throws IOException, RepositoryException, MalformedQueryException, UnauthorizedException,
		QueryInterruptedException
	{
		return sendBooleanQuery(ql, query, null, dataset, includeInferred, 0, bindings);
	}

	public boolean sendBooleanQuery(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, Binding... bindings)
		throws IOException, RepositoryException, MalformedQueryException, UnauthorizedException,
		QueryInterruptedException
	{
		HttpMethod method = getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime, bindings);

		try {
			return getBoolean(method);
		}
		finally {
			releaseConnection(method);
		}
	}

	protected HttpMethod getQueryMethod(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, Binding... bindings)
	{
		PostMethod method = new PostMethod(getQueryURL());
		setDoAuthentication(method);

		method.setRequestHeader("Content-Type", Protocol.FORM_MIME_TYPE + "; charset=utf-8");

		List<NameValuePair> queryParams = getQueryMethodParameters(ql, query, baseURI, dataset,
				includeInferred, maxQueryTime, bindings);

		method.setRequestBody(queryParams.toArray(new NameValuePair[queryParams.size()]));

		return method;
	}

	protected HttpMethod getUpdateMethod(QueryLanguage ql, String update, String baseURI, Dataset dataset,
			boolean includeInferred, Binding... bindings)
	{
		PostMethod method = new PostMethod(getUpdateURL());
		setDoAuthentication(method);

		method.setRequestHeader("Content-Type", Protocol.FORM_MIME_TYPE + "; charset=utf-8");

		List<NameValuePair> queryParams = getUpdateMethodParameters(ql, update, baseURI, dataset,
				includeInferred, bindings);

		method.setRequestBody(queryParams.toArray(new NameValuePair[queryParams.size()]));

		return method;
	}

	protected List<NameValuePair> getQueryMethodParameters(QueryLanguage ql, String query, String baseURI,
			Dataset dataset, boolean includeInferred, int maxQueryTime, Binding... bindings)
	{
		List<NameValuePair> queryParams = new ArrayList<NameValuePair>(bindings.length + 10);

		queryParams.add(new NameValuePair(Protocol.QUERY_LANGUAGE_PARAM_NAME, ql.getName()));
		queryParams.add(new NameValuePair(Protocol.QUERY_PARAM_NAME, query));
		if (baseURI != null) {
			queryParams.add(new NameValuePair(Protocol.BASEURI_PARAM_NAME, baseURI));
		}
		queryParams.add(new NameValuePair(Protocol.INCLUDE_INFERRED_PARAM_NAME,
				Boolean.toString(includeInferred)));
		if (maxQueryTime > 0) {
			queryParams.add(new NameValuePair(Protocol.TIMEOUT_PARAM_NAME, Integer.toString(maxQueryTime)));
		}

		if (dataset != null) {
			for (URI defaultGraphURI : dataset.getDefaultGraphs()) {
				queryParams.add(new NameValuePair(Protocol.DEFAULT_GRAPH_PARAM_NAME,
						String.valueOf(defaultGraphURI)));
			}
			for (URI namedGraphURI : dataset.getNamedGraphs()) {
				queryParams.add(new NameValuePair(Protocol.NAMED_GRAPH_PARAM_NAME, String.valueOf(namedGraphURI)));
			}
		}

		for (int i = 0; i < bindings.length; i++) {
			String paramName = Protocol.BINDING_PREFIX + bindings[i].getName();
			String paramValue = Protocol.encodeValue(bindings[i].getValue());
			queryParams.add(new NameValuePair(paramName, paramValue));
		}

		return queryParams;
	}

	protected List<NameValuePair> getUpdateMethodParameters(QueryLanguage ql, String update, String baseURI,
			Dataset dataset, boolean includeInferred, Binding... bindings)
	{
		List<NameValuePair> queryParams = new ArrayList<NameValuePair>(bindings.length + 10);

		queryParams.add(new NameValuePair(Protocol.QUERY_LANGUAGE_PARAM_NAME, ql.getName()));
		queryParams.add(new NameValuePair(Protocol.UPDATE_PARAM_NAME, update));
		if (baseURI != null) {
			queryParams.add(new NameValuePair(Protocol.BASEURI_PARAM_NAME, baseURI));
		}
		queryParams.add(new NameValuePair(Protocol.INCLUDE_INFERRED_PARAM_NAME,
				Boolean.toString(includeInferred)));

		if (dataset != null) {
			for (URI graphURI : dataset.getDefaultRemoveGraphs()) {
				queryParams.add(new NameValuePair(Protocol.REMOVE_GRAPH_PARAM_NAME, String.valueOf(graphURI)));
			}
			if (dataset.getDefaultInsertGraph() != null) {
				queryParams.add(new NameValuePair(Protocol.INSERT_GRAPH_PARAM_NAME,
						String.valueOf(dataset.getDefaultInsertGraph())));
			}
			for (URI defaultGraphURI : dataset.getDefaultGraphs()) {
				queryParams.add(new NameValuePair(Protocol.USING_GRAPH_PARAM_NAME,
						String.valueOf(defaultGraphURI)));
			}
			for (URI namedGraphURI : dataset.getNamedGraphs()) {
				queryParams.add(new NameValuePair(Protocol.USING_NAMED_GRAPH_PARAM_NAME,
						String.valueOf(namedGraphURI)));
			}
		}

		for (int i = 0; i < bindings.length; i++) {
			String paramName = Protocol.BINDING_PREFIX + bindings[i].getName();
			String paramValue = Protocol.encodeValue(bindings[i].getValue());
			queryParams.add(new NameValuePair(paramName, paramValue));
		}

		return queryParams;
	}

	/*------------------*
	 * Response parsing *
	 *------------------*/

	/**
	 * Parse the response in a background thread. HTTP connections are dealt with
	 * in the {@link BackgroundTupleResult} or (in the error-case) in this method.
	 */
	protected BackgroundTupleResult getBackgroundTupleQueryResult(HttpMethod method)
		throws RepositoryException, QueryInterruptedException, HttpException, MalformedQueryException,
		IOException
	{
	
		boolean submitted = false;
		try {
			
			// Specify which formats we support
			Set<TupleQueryResultFormat> tqrFormats = TupleQueryResultParserRegistry.getInstance().getKeys();
			if (tqrFormats.isEmpty()) {
				throw new RepositoryException("No tuple query result parsers have been registered");
			}
			
			// send the tuple query
			sendTupleQueryViaHttp(method, tqrFormats);
			
			// if we get here, HTTP code is 200
			String mimeType = getResponseMIMEType(method);
			try {
				TupleQueryResultFormat format = TupleQueryResultFormat.matchMIMEType(mimeType, tqrFormats);
				TupleQueryResultParser parser = QueryResultIO.createParser(format, getValueFactory());
				BackgroundTupleResult tRes = new BackgroundTupleResult(parser, method.getResponseBodyAsStream(), method);
				execute(tRes);
				submitted = true;
				return tRes;
			}
			catch (UnsupportedQueryResultFormatException e) {
				throw new RepositoryException("Server responded with an unsupported file format: " + mimeType);
			}
		} finally {
			if (!submitted)
				releaseConnection(method);
		}
	}
			
	
	/**
	 * Parse the response in this thread using the provided {@link TupleQueryResultHandler}.
	 * All HTTP connections are closed and released in this method
	 */
	protected void getTupleQueryResult(HttpMethod method, TupleQueryResultHandler handler)
		throws IOException, TupleQueryResultHandlerException, RepositoryException, MalformedQueryException,
		UnauthorizedException, QueryInterruptedException
	{		
		try {
			
			// Specify which formats we support
			Set<TupleQueryResultFormat> tqrFormats = TupleQueryResultParserRegistry.getInstance().getKeys();
			if (tqrFormats.isEmpty()) {
				throw new RepositoryException("No tuple query result parsers have been registered");
			}
			
			// send the tuple query
			sendTupleQueryViaHttp(method, tqrFormats);
			
			// if we get here, HTTP code is 200
			String mimeType = getResponseMIMEType(method);
			try {
				TupleQueryResultFormat format = TupleQueryResultFormat.matchMIMEType(mimeType, tqrFormats);
				TupleQueryResultParser parser = QueryResultIO.createParser(format, getValueFactory());
				parser.setQueryResultHandler(handler);
				parser.parseQueryResult(method.getResponseBodyAsStream());
			}
			catch (UnsupportedQueryResultFormatException e) {
				throw new RepositoryException("Server responded with an unsupported file format: " + mimeType);
			}
			catch (QueryResultParseException e) {
				throw new RepositoryException("Malformed query result from server", e);
			}
			catch (QueryResultHandlerException e) {
				if (e instanceof TupleQueryResultHandlerException) {
					throw (TupleQueryResultHandlerException)e;
				}
				else {
					throw new TupleQueryResultHandlerException(e);
				}
			}			
		} finally {
			releaseConnection(method);
		}
	}
	
	/**
	 * Send the tuple query via HTTP and throws an exception in case anything
	 * goes wrong, i.e. only for HTTP 200 the method returns without
	 * exception.
	 * 
	 * If HTTP status code is not equal to 200, the request is aborted,
	 * however pooled connections are not released.
	 * 
	 * @param method
	 * @throws RepositoryException
	 * @throws HttpException
	 * @throws IOException
	 * @throws QueryInterruptedException
	 * @throws MalformedQueryException
	 */
	private void sendTupleQueryViaHttp(HttpMethod method, Set<TupleQueryResultFormat> tqrFormats)
		throws RepositoryException, HttpException, IOException, QueryInterruptedException,
		MalformedQueryException
	{

		for (TupleQueryResultFormat format : tqrFormats) {
			// Determine a q-value that reflects the user specified preference
			int qValue = 10;

			if (preferredTQRFormat != null && !preferredTQRFormat.equals(format)) {
				// Prefer specified format over other formats
				qValue -= 2;
			}

			for (String mimeType : format.getMIMETypes()) {
				String acceptParam = mimeType;

				if (qValue < 10) {
					acceptParam += ";q=0." + qValue;
				}

				method.addRequestHeader(ACCEPT_PARAM_NAME, acceptParam);
			}
		}

		int httpCode = httpClient.executeMethod(method);

		if (httpCode == HttpURLConnection.HTTP_OK) {			
			return; // everything OK, control flow can continue
		} else {
			// in any other case abort the http execution
			method.abort();
		}
		
		if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
			throw new UnauthorizedException();
		}
		else if (httpCode == HttpURLConnection.HTTP_UNAVAILABLE) {
			throw new QueryInterruptedException();
		}
		else {
			ErrorInfo errInfo = getErrorInfo(method);

			// Throw appropriate exception
			if (errInfo.getErrorType() == ErrorType.MALFORMED_QUERY) {
				throw new MalformedQueryException(errInfo.getErrorMessage());
			}
			else if (errInfo.getErrorType() == ErrorType.UNSUPPORTED_QUERY_LANGUAGE) {
				throw new UnsupportedQueryLanguageException(errInfo.getErrorMessage());
			}
			else {
				throw new RepositoryException(errInfo.toString());
			}
		}
	}

	protected void getRDF(HttpMethod method, RDFHandler handler, boolean requireContext)
		throws IOException, RDFHandlerException, RepositoryException, MalformedQueryException,
		UnauthorizedException, QueryInterruptedException
	{
		// Specify which formats we support using Accept headers
		Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
		if (rdfFormats.isEmpty()) {
			throw new RepositoryException("No tuple RDF parsers have been registered");
		}

		List<String> acceptParams = RDFFormat.getAcceptParams(rdfFormats, requireContext, preferredRDFFormat);
		for (String acceptParam : acceptParams) {
			method.addRequestHeader(ACCEPT_PARAM_NAME, acceptParam);
		}

		int httpCode = httpClient.executeMethod(method);

		if (httpCode == HttpURLConnection.HTTP_OK) {
			String mimeType = getResponseMIMEType(method);
			try {
				RDFFormat format = RDFFormat.matchMIMEType(mimeType, rdfFormats);
				RDFParser parser = Rio.createParser(format, getValueFactory());
				parser.setParserConfig(getParserConfig());
				parser.setParseErrorListener(new ParseErrorLogger());
				parser.setRDFHandler(handler);
				parser.parse(method.getResponseBodyAsStream(), method.getURI().getURI());
			}
			catch (UnsupportedRDFormatException e) {
				throw new RepositoryException("Server responded with an unsupported file format: " + mimeType);
			}
			catch (RDFParseException e) {
				throw new RepositoryException("Malformed query result from server", e);
			}
		}
		else if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
			throw new UnauthorizedException();
		}
		else if (httpCode == HttpURLConnection.HTTP_UNAVAILABLE) {
			throw new QueryInterruptedException();
		}
		else {
			ErrorInfo errInfo = getErrorInfo(method);

			// Throw appropriate exception
			if (errInfo.getErrorType() == ErrorType.MALFORMED_QUERY) {
				throw new MalformedQueryException(errInfo.getErrorMessage());
			}
			else if (errInfo.getErrorType() == ErrorType.UNSUPPORTED_QUERY_LANGUAGE) {
				throw new UnsupportedQueryLanguageException(errInfo.getErrorMessage());
			}
			else {
				throw new RepositoryException(errInfo.toString());
			}
		}
	}

	protected boolean getBoolean(HttpMethod method)
		throws IOException, RepositoryException, MalformedQueryException, UnauthorizedException,
		QueryInterruptedException
	{
		// Specify which formats we support using Accept headers
		Set<BooleanQueryResultFormat> booleanFormats = BooleanQueryResultParserRegistry.getInstance().getKeys();
		if (booleanFormats.isEmpty()) {
			throw new RepositoryException("No boolean query result parsers have been registered");
		}

		for (BooleanQueryResultFormat format : booleanFormats) {
			// Determine a q-value that reflects the user specified preference
			int qValue = 10;

			if (preferredBQRFormat != null && !preferredBQRFormat.equals(format)) {
				// Prefer specified format over other formats
				qValue -= 2;
			}

			for (String mimeType : format.getMIMETypes()) {
				String acceptParam = mimeType;

				if (qValue < 10) {
					acceptParam += ";q=0." + qValue;
				}

				method.addRequestHeader(ACCEPT_PARAM_NAME, acceptParam);
			}
		}

		int httpCode = httpClient.executeMethod(method);

		if (httpCode == HttpURLConnection.HTTP_OK) {
			String mimeType = getResponseMIMEType(method);
			try {
				BooleanQueryResultFormat format = BooleanQueryResultFormat.matchMIMEType(mimeType, booleanFormats);
				BooleanQueryResultParser parser = QueryResultIO.createParser(format);
				return parser.parse(method.getResponseBodyAsStream());
			}
			catch (UnsupportedQueryResultFormatException e) {
				throw new RepositoryException("Server responded with an unsupported file format: " + mimeType);
			}
			catch (QueryResultParseException e) {
				throw new RepositoryException("Malformed query result from server", e);
			}
		}
		else if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
			throw new UnauthorizedException();
		}
		else if (httpCode == HttpURLConnection.HTTP_UNAVAILABLE) {
			throw new QueryInterruptedException();
		}
		else {
			ErrorInfo errInfo = getErrorInfo(method);

			// Throw appropriate exception
			if (errInfo.getErrorType() == ErrorType.MALFORMED_QUERY) {
				throw new MalformedQueryException(errInfo.getErrorMessage());
			}
			else if (errInfo.getErrorType() == ErrorType.UNSUPPORTED_QUERY_LANGUAGE) {
				throw new UnsupportedQueryLanguageException(errInfo.getErrorMessage());
			}
			else {
				throw new RepositoryException(method.getStatusText());
			}
		}
	}

	/*-------------------------*
	 * General utility methods *
	 *-------------------------*/

	/**
	 * Checks whether the specified status code is in the 2xx-range, indicating a
	 * successfull request.
	 * 
	 * @return <tt>true</tt> if the status code is in the 2xx range.
	 */
	protected boolean is2xx(int statusCode) {
		return statusCode >= 200 && statusCode < 300;
	}

	/**
	 * Gets the MIME type specified in the response headers of the supplied
	 * method, if any. For example, if the response headers contain
	 * <tt>Content-Type: application/xml;charset=UTF-8</tt>, this method will
	 * return <tt>application/xml</tt> as the MIME type.
	 * 
	 * @param method
	 *        The method to get the reponse MIME type from.
	 * @return The response MIME type, or <tt>null</tt> if not available.
	 */
	protected String getResponseMIMEType(HttpMethod method)
		throws IOException
	{
		Header[] headers = method.getResponseHeaders("Content-Type");

		for (Header header : headers) {
			HeaderElement[] headerElements = header.getElements();

			for (HeaderElement headerEl : headerElements) {
				String mimeType = headerEl.getName();
				if (mimeType != null) {
					logger.debug("reponse MIME type is {}", mimeType);
					return mimeType;
				}
			}
		}

		return null;
	}

	protected ErrorInfo getErrorInfo(HttpMethod method)
		throws RepositoryException
	{
		try {
			ErrorInfo errInfo = ErrorInfo.parse(method.getResponseBodyAsString());
			logger.warn("Server reports problem: {}", errInfo.getErrorMessage());
			return errInfo;
		}
		catch (IOException e) {
			logger.warn("Unable to retrieve error info from server");
			throw new RepositoryException("Unable to retrieve error info from server", e);
		}
	}

	protected final void setDoAuthentication(HttpMethod method) {
		if (authScope != null && httpClient.getState().getCredentials(authScope) != null) {
			method.setDoAuthentication(true);
		}
		else {
			method.setDoAuthentication(false);
		}
	}

	protected final void releaseConnection(HttpMethod method) {
		if (Thread.currentThread().isInterrupted()) {
			method.abort();
		}
		method.releaseConnection();
	}

	/**
	 * @param parserConfig
	 *        The parserConfig to set.
	 */
	public void setParserConfig(ParserConfig parserConfig) {
		this.parserConfig = parserConfig;
	}

	/**
	 * @return Returns the parserConfig.
	 */
	public ParserConfig getParserConfig() {
		return parserConfig;
	}

	/**
	 * Gets the http connection read timeout in milliseconds.
	 */
	public long getConnectionTimeout() {
		return this.httpClient.getParams().getConnectionManagerTimeout();
	}

	/**
	 * Sets the http connection read timeout.
	 * 
	 * @param timeout
	 *        timeout in milliseconds. Zero sets to infinity.
	 */
	public void setConnectionTimeout(long timeout) {
		this.httpClient.getParams().setConnectionManagerTimeout(timeout);
	}

	private static void configureProxySettings(HttpClient httpClient) {
		String proxyHostName = System.getProperty("http.proxyHost");
		if (proxyHostName != null && proxyHostName.length() > 0) {
			int proxyPort = 80; // default
			try {
				proxyPort = Integer.parseInt(System.getProperty("http.proxyPort"));
			}
			catch (NumberFormatException e) {
				// do nothing, revert to default
			}
			ProxyHost proxyHost = new ProxyHost(proxyHostName, proxyPort);
			httpClient.getHostConfiguration().setProxyHost(proxyHost);

			String proxyUser = System.getProperty("http.proxyUser");
			if (proxyUser != null) {
				String proxyPassword = System.getProperty("http.proxyPassword");
				httpClient.getState().setProxyCredentials(
						new AuthScope(proxyHost.getHostName(), proxyHost.getPort()),
						new UsernamePasswordCredentials(proxyUser, proxyPassword));
				httpClient.getParams().setAuthenticationPreemptive(true);
			}
		}
	}
}
