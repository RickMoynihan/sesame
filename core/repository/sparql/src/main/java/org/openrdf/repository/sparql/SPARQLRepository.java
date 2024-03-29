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
package org.openrdf.repository.sparql;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.apache.http.client.HttpClient;

import org.openrdf.http.client.HttpClientDependent;
import org.openrdf.http.client.SesameClient;
import org.openrdf.http.client.SesameClientDependent;
import org.openrdf.http.client.SesameClientImpl;
import org.openrdf.http.client.SparqlSession;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryBase;

/**
 * A proxy class to access any SPARQL endpoint. The instance must be initialized
 * prior to using it.
 * 
 * @author James Leigh
 */
public class SPARQLRepository extends RepositoryBase implements HttpClientDependent, SesameClientDependent {
	
	/**
	 * Flag indicating if quad mode is enabled in newly created
	 * {@link SPARQLConnection}s.
	 * @see #enableQuadMode(boolean) 
	 */
	private boolean quadMode = false;

	/**
	 * The HTTP client that takes care of the client-server communication.
	 */
	private SesameClient client;

	/** dependent life cycle */
	private SesameClientImpl dependentClient;

	private String username;

	private String password;

	private String queryEndpointUrl;

	private String updateEndpointUrl;

	private Map<String, String> additionalHttpHeaders = Collections.emptyMap();

	/**
	 * Create a new SPARQLRepository using the supplied endpoint URL for queries
	 * and updates.
	 * 
	 * @param endpointUrl
	 *        a SPARQL endpoint URL. May not be null.
	 */
	public SPARQLRepository(String endpointUrl) {
		this(endpointUrl, endpointUrl);
	}

	/**
	 * Create a new SPARQLREpository using the supplied query endpoint URL for
	 * queries, and the supplied update endpoint URL for updates.
	 * 
	 * @param queryEndpointUrl
	 *        a SPARQL endpoint URL for queries. May not be null.
	 * @param updateEndpointUrl
	 *        a SPARQL endpoint URL for updates. May not be null.
	 * @throws IllegalArgumentException
	 *         if one of the supplied endpoint URLs is null.
	 */
	public SPARQLRepository(String queryEndpointUrl, String updateEndpointUrl) {
		if (queryEndpointUrl == null || updateEndpointUrl == null) {
			throw new IllegalArgumentException("endpoint URL may not be null.");
		}
		this.queryEndpointUrl = queryEndpointUrl;
		this.updateEndpointUrl = updateEndpointUrl;
	}

	public synchronized SesameClient getSesameClient() {
		if (client == null) {
			client = dependentClient = new SesameClientImpl();
		}
		return client;
	}

	public synchronized void setSesameClient(SesameClient client) {
		this.client = client;
	}

	public final HttpClient getHttpClient() {
		return getSesameClient().getHttpClient();
	}

	public void setHttpClient(HttpClient httpClient) {
		if (dependentClient == null) {
			client = dependentClient = new SesameClientImpl();
		}
		dependentClient.setHttpClient(httpClient);
	}

	/**
	 * Creates a new HTTPClient object. Subclasses may override to return a more
	 * specific HTTPClient subtype.
	 * 
	 * @return a HTTPClient object.
	 */
	protected SparqlSession createHTTPClient() {
		// initialize HTTP client
		SparqlSession httpClient = getSesameClient().createSparqlSession(queryEndpointUrl, updateEndpointUrl);
		httpClient.setValueFactory(ValueFactoryImpl.getInstance());
		httpClient.setPreferredTupleQueryResultFormat(TupleQueryResultFormat.SPARQL);
		httpClient.setAdditionalHttpHeaders(additionalHttpHeaders);
		if (username != null) {
			httpClient.setUsernameAndPassword(username, password);
		}
		return httpClient;
	}

	public RepositoryConnection getConnection()
		throws RepositoryException
	{
		if (!isInitialized()) {
			throw new RepositoryException("SPARQLRepository not initialized.");
		}
		return new SPARQLConnection(this, createHTTPClient(), quadMode);
	}

	public File getDataDir() {
		return null;
	}

	public ValueFactory getValueFactory() {
		return ValueFactoryImpl.getInstance();
	}

	@Override
	protected void initializeInternal()
		throws RepositoryException
	{
		// no-op
	}

	public boolean isWritable()
		throws RepositoryException
	{
		return false;
	}

	public void setDataDir(File dataDir) {
		// no-op
	}

	/**
	 * Set the username and password to use for authenticating with the remote
	 * repository.
	 * 
	 * @param username
	 *        the username. Setting this to null will disable authentication.
	 * @param password
	 *        the password. Setting this to null will disable authentication.
	 */
	public void setUsernameAndPassword(final String username, final String password) {
		this.username = username;
		this.password = password;
	}

	@Override
	protected void shutDownInternal()
		throws RepositoryException
	{
		if (dependentClient != null) {
			dependentClient.shutDown();
			dependentClient = null;
		}
		// remove reference but do not shut down, client may be shared by other
		// repos.
		client = null;
	}

	@Override
	public String toString() {
		return queryEndpointUrl;
	}

	/**
	 * Get the additional HTTP headers which will be used
	 * 
	 * @return a read-only view of the additional HTTP headers which will be
	 *         included in every request to the server.
	 */
	public Map<String, String> getAdditionalHttpHeaders() {
		return Collections.unmodifiableMap(additionalHttpHeaders);
	}

	/**
	 * Set additional HTTP headers to be included in every request to the server,
	 * which may be required for certain unusual server configurations. This will
	 * only take effect on connections subsequently returned by
	 * {@link #getConnection()}.
	 * 
	 * @param additionalHttpHeaders
	 *        a map containing pairs of header names and values. May be null
	 */
	public void setAdditionalHttpHeaders(Map<String, String> additionalHttpHeaders) {
		if (additionalHttpHeaders == null) {
			this.additionalHttpHeaders = Collections.emptyMap();
		}
		else {
			this.additionalHttpHeaders = additionalHttpHeaders;
		}
	}
	
	/**
	 * Activate quad mode for this {@link SPARQLRepository}, i.e. for 
	 * retrieval of statements also retrieve the graph.<p>
	 * 
	 * Note: the setting is only applied in newly created {@link SPARQLConnection}s
	 * as the setting is an immutable configuration of a connection instance.
	 * 
	 * @param flag flag to enable or disable the quad mode
	 * @see SPARQLConnection#getStatements(org.openrdf.model.Resource, org.openrdf.model.URI, org.openrdf.model.Value, boolean, org.openrdf.model.Resource...)
	 */
	public void enableQuadMode(boolean flag) {
		this.quadMode = flag;
	}
}
