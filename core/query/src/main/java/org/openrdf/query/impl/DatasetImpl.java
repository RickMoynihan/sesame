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
package org.openrdf.query.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.query.Dataset;

/**
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class DatasetImpl implements Dataset, Serializable {

	private static final long serialVersionUID = 7841576172053060417L;

	private Set<URI> defaultRemoveGraphs = new LinkedHashSet<URI>();

	private URI defaultInsertGraph;

	private Set<URI> defaultGraphs = new LinkedHashSet<URI>();

	private Set<URI> namedGraphs = new LinkedHashSet<URI>();

	public DatasetImpl() {
	}

	public Set<URI> getDefaultRemoveGraphs() {
		return Collections.unmodifiableSet(defaultRemoveGraphs);
	}

	/**
	 * Adds a graph URI to the set of default remove graph URIs.
	 */
	public void addDefaultRemoveGraph(URI graphURI) {
		defaultRemoveGraphs.add(graphURI);
	}

	/**
	 * Removes a graph URI from the set of default remove graph URIs.
	 * 
	 * @return <tt>true</tt> if the URI was removed from the set, <tt>false</tt>
	 *         if the set did not contain the URI.
	 */
	public boolean removeDefaultRemoveGraph(URI graphURI) {
		return defaultRemoveGraphs.remove(graphURI);
	}

	/**
	 * @return Returns the default insert graph.
	 */
	public URI getDefaultInsertGraph() {
		return defaultInsertGraph;
	}

	/**
	 * @param defaultInsertGraph
	 *        The default insert graph to used.
	 */
	public void setDefaultInsertGraph(URI defaultInsertGraph) {
		this.defaultInsertGraph = defaultInsertGraph;
	}

	public Set<URI> getDefaultGraphs() {
		return Collections.unmodifiableSet(defaultGraphs);
	}

	/**
	 * Adds a graph URI to the set of default graph URIs.
	 */
	public void addDefaultGraph(URI graphURI) {
		defaultGraphs.add(graphURI);
	}

	/**
	 * Removes a graph URI from the set of default graph URIs.
	 * 
	 * @return <tt>true</tt> if the URI was removed from the set, <tt>false</tt>
	 *         if the set did not contain the URI.
	 */
	public boolean removeDefaultGraph(URI graphURI) {
		return defaultGraphs.remove(graphURI);
	}

	/**
	 * Gets the (unmodifiable) set of named graph URIs.
	 */
	public Set<URI> getNamedGraphs() {
		return Collections.unmodifiableSet(namedGraphs);
	}

	/**
	 * Adds a graph URI to the set of named graph URIs.
	 */
	public void addNamedGraph(URI graphURI) {
		namedGraphs.add(graphURI);
	}

	/**
	 * Removes a graph URI from the set of named graph URIs.
	 * 
	 * @return <tt>true</tt> if the URI was removed from the set, <tt>false</tt>
	 *         if the set did not contain the URI.
	 */
	public boolean removeNamedGraph(URI graphURI) {
		return namedGraphs.remove(graphURI);
	}

	/**
	 * Removes all graph URIs (both default and named) from this dataset.
	 */
	public void clear() {
		defaultRemoveGraphs.clear();
		defaultInsertGraph = null;
		defaultGraphs.clear();
		namedGraphs.clear();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (URI uri : getDefaultRemoveGraphs()) {
			sb.append("DELETE FROM ");
			appendURI(sb, uri);
		}
		if (getDefaultInsertGraph() != null) {
			sb.append("INSERT INTO ");
			appendURI(sb, getDefaultInsertGraph());
		}
		for (URI uri : getDefaultGraphs()) {
			sb.append("USING ");
			appendURI(sb, uri);
		}
		for (URI uri : getNamedGraphs()) {
			sb.append("USING NAMED ");
			appendURI(sb, uri);
		}
		if (getDefaultGraphs().isEmpty() && getNamedGraphs().isEmpty()) {
			sb.append("## empty dataset ##");
		}
		return sb.toString();
	}

	private void appendURI(StringBuilder sb, URI uri) {
		String str = uri.toString();
		if (str.length() > 50) {
			sb.append("<").append(str, 0, 19).append("..");
			sb.append(str, str.length() - 29, str.length()).append(">\n");
		}
		else {
			sb.append("<").append(uri).append(">\n");
		}
	}
}
