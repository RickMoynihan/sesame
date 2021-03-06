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
package org.openrdf.model.vocabulary;

import org.openrdf.model.Namespace;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.model.impl.ValueFactoryImpl;

/**
 * Namespace Sparql-service-description. Prefix:
 * {@code <http://www.w3.org/ns/sparql-service-description#>}
 * 
 * @see <a href="http://www.w3.org/TR/sparql11-service-description/">SPARQL 1.1
 *      Service Description</a>
 * @author Peter Ansell
 */
public class SD {

	/**
	 * {@code http://www.w3.org/ns/sparql-service-description#}
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/sparql-service-description#";

	/**
	 * {@code sd}
	 */
	public static final String PREFIX = "sd";

	/**
	 * An immutable {@link Namespace} constant that represents the SPARQL Service
	 * Description namespace.
	 */
	public static final Namespace NS = new NamespaceImpl(PREFIX, NAMESPACE);

	/**
	 * Aggregate
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#Aggregate}.
	 * <p>
	 * An instance of sd:Aggregate represents an aggregate that may be used in a
	 * SPARQL aggregate query (for instance in a HAVING clause or SELECT
	 * expression) besides the standard list of supported aggregates COUNT, SUM,
	 * MIN, MAX, AVG, GROUP_CONCAT, and SAMPLE.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#Aggregate">Aggregate</a>
	 */
	public static final URI AGGREGATE;

	/**
	 * available graph descriptions
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#availableGraphs}.
	 * <p>
	 * Relates an instance of sd:Service to a description of the graphs which are
	 * allowed in the construction of a dataset either via the SPARQL Protocol,
	 * with FROM/FROM NAMED clauses in a query, or with USING/USING NAMED in an
	 * update request, if the service limits the scope of dataset construction.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#availableGraphs">availableGraphs</a>
	 */
	public static final URI AVAILBLE_GRAPHS;

	/**
	 * Basic Federated Query
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#BasicFederatedQuery}.
	 * <p>
	 * sd:BasicFederatedQuery, when used as the object of the sd:feature
	 * property, indicates that the SPARQL service supports basic federated query
	 * using the SERVICE keyword as defined by SPARQL 1.1 Federation Extensions.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#BasicFederatedQuery">BasicFederatedQuery</a>
	 */
	public static final URI BASIC_FEDERATED_QUERY;

	/**
	 * Dataset
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#Dataset}.
	 * <p>
	 * An instance of sd:Dataset represents a RDF Dataset comprised of a default
	 * graph and zero or more named graphs.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#Dataset">Dataset</a>
	 */
	public static final URI DATASET;

	/**
	 * default dataset description
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#defaultDataset}.
	 * <p>
	 * Relates an instance of sd:Service to a description of the default dataset
	 * available when no explicit dataset is specified in the query, update
	 * request or via protocol parameters.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#defaultDataset">defaultDataset</a>
	 */
	public static final URI DEFAULT_DATASET;

	/**
	 * default entailment regime
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#defaultEntailmentRegime}.
	 * <p>
	 * Relates an instance of sd:Service with a resource representing an
	 * entailment regime used for basic graph pattern matching. This property is
	 * intended for use when a single entailment regime by default applies to all
	 * graphs in the default dataset of the service. In situations where a
	 * different entailment regime applies to a specific graph in the dataset,
	 * the sd:entailmentRegime property should be used to indicate this fact in
	 * the description of that graph.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#defaultEntailmentRegime">defaultEntailmentRegime</a>
	 */
	public static final URI DEFAULT_ENTAILMENT_REGIME;

	/**
	 * default graph
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#defaultGraph}.
	 * <p>
	 * Relates an instance of sd:Dataset to the description of its default graph.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#defaultGraph">defaultGraph</a>
	 */
	public static final URI DEFAULT_GRAPH;

	/**
	 * default supported entailment profile
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#defaultSupportedEntailmentProfile}.
	 * <p>
	 * Relates an instance of sd:Service with a resource representing a supported
	 * profile of the default entailment regime (as declared by
	 * sd:defaultEntailmentRegime).
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#defaultSupportedEntailmentProfile">defaultSupportedEntailmentProfile</a>
	 */
	public static final URI DEFAULT_SUPPORTED_ENTAILMENT_PROFILE;

	/**
	 * Dereferences URIs
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#DereferencesURIs}.
	 * <p>
	 * sd:DereferencesURIs, when used as the object of the sd:feature property,
	 * indicates that a SPARQL service will dereference URIs used in FROM/FROM
	 * NAMED and USING/USING NAMED clauses and use the resulting RDF in the
	 * dataset during query evaluation.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#DereferencesURIs">DereferencesURIs</a>
	 */
	public static final URI DEREFERENCES_URIS;

	/**
	 * Empty Graphs
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#EmptyGraphs}.
	 * <p>
	 * sd:EmptyGraphs, when used as the object of the sd:feature property,
	 * indicates that the underlying graph store supports empty graphs. A graph
	 * store that supports empty graphs MUST NOT remove graphs that are left
	 * empty after triples are removed from them.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#EmptyGraphs">EmptyGraphs</a>
	 */
	public static final URI EMPTY_GRAPHS;

	/**
	 * endpoint
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#endpoint}.
	 * <p>
	 * The SPARQL endpoint of an sd:Service that implements the SPARQL Protocol
	 * service. The object of the sd:endpoint property is an IRI.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#endpoint">endpoint</a>
	 */
	public static final URI ENDPOINT;

	/**
	 * Entailment Profile
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#EntailmentProfile}.
	 * <p>
	 * An instance of sd:EntailmentProfile represents a profile of an entailment
	 * regime. An entailment profile MAY impose restrictions on what constitutes
	 * valid RDF with respect to entailment.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#EntailmentProfile">EntailmentProfile</a>
	 */
	public static final URI ENTAILMENT_PROFILE;

	/**
	 * Entailment Regime
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#EntailmentRegime}.
	 * <p>
	 * An instance of sd:EntailmentRegime represents an entailment regime used in
	 * basic graph pattern matching (as described by SPARQL 1.1 Query Language).
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#EntailmentRegime">EntailmentRegime</a>
	 */
	public static final URI ENTAILMENT_REGIME_CLASS;

	/**
	 * entailment regime
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#entailmentRegime}.
	 * <p>
	 * Relates a named graph description with a resource representing an
	 * entailment regime used for basic graph pattern matching over that graph.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#entailmentRegime">entailmentRegime</a>
	 */
	public static final URI ENTAILMENT_REGIME_PROPERTY;

	/**
	 * extension aggregate
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#extensionAggregate}.
	 * <p>
	 * Relates an instance of sd:Service to an aggregate that may be used in a
	 * SPARQL aggregate query (for instance in a HAVING clause or SELECT
	 * expression) besides the standard list of supported aggregates COUNT, SUM,
	 * MIN, MAX, AVG, GROUP_CONCAT, and SAMPLE
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#extensionAggregate">extensionAggregate</a>
	 */
	public static final URI EXTENSION_AGGREGATE;

	/**
	 * extension function
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#extensionFunction}.
	 * <p>
	 * Relates an instance of sd:Service to a function that may be used in a
	 * SPARQL SELECT expression or a FILTER, HAVING, GROUP BY, ORDER BY, or BIND
	 * clause.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#extensionFunction">extensionFunction</a>
	 */
	public static final URI EXTENSION_FUNCTION;

	/**
	 * Feature
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#Feature}.
	 * <p>
	 * An instance of sd:Feature represents a feature of a SPARQL service.
	 * Specific types of features include functions, aggregates, languages, and
	 * entailment regimes and profiles. This document defines five instances of
	 * sd:Feature: sd:DereferencesURIs, sd:UnionDefaultGraph, sd:RequiresDataset,
	 * sd:EmptyGraphs, and sd:BasicFederatedQuery.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#Feature">Feature</a>
	 */
	public static final URI FEATURE_CLASS;

	/**
	 * feature
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#feature}.
	 * <p>
	 * Relates an instance of sd:Service with a resource representing a supported
	 * feature.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#feature">feature</a>
	 */
	public static final URI FEATURE_PROPERTY;

	/**
	 * Function
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#Function}.
	 * <p>
	 * An instance of sd:Function represents a function that may be used in a
	 * SPARQL SELECT expression or a FILTER, HAVING, GROUP BY, ORDER BY, or BIND
	 * clause.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#Function">Function</a>
	 */
	public static final URI FUNCTION;

	/**
	 * graph
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#graph}.
	 * <p>
	 * Relates a named graph to its graph description.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#graph">graph</a>
	 */
	public static final URI GRAPH_PROPERTY;

	/**
	 * Graph
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#Graph}.
	 * <p>
	 * An instance of sd:Graph represents the description of an RDF graph.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#Graph">Graph</a>
	 */
	public static final URI GRAPH_CLASS;

	/**
	 * Graph Collection
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#GraphCollection}.
	 * <p>
	 * An instance of sd:GraphCollection represents a collection of zero or more
	 * named graph descriptions. Each named graph description belonging to an
	 * sd:GraphCollection MUST be linked with the sd:namedGraph predicate.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#GraphCollection">GraphCollection</a>
	 */
	public static final URI GRAPH_COLLECTION;

	/**
	 * input format
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#inputFormat}.
	 * <p>
	 * Relates an instance of sd:Service to a format that is supported for
	 * parsing RDF input; for example, via a SPARQL 1.1 Update LOAD statement, or
	 * when URIs are dereferenced in FROM/FROM NAMED/USING/USING NAMED clauses.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#inputFormat">inputFormat</a>
	 */
	public static final URI INPUT_FORMAT;

	/**
	 * Language
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#Language}.
	 * <p>
	 * An instance of sd:Language represents one of the SPARQL languages,
	 * including specific configurations providing particular features or
	 * extensions. This document defines three instances of sd:Language:
	 * sd:SPARQL10Query, sd:SPARQL11Query, and sd:SPARQL11Update.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#Language">Language</a>
	 */
	public static final URI LANGUAGE;

	/**
	 * language extension
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#languageExtension}.
	 * <p>
	 * Relates an instance of sd:Service to a resource representing an
	 * implemented extension to the SPARQL Query or Update language.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#languageExtension">languageExtension</a>
	 */
	public static final URI LANGUAGE_EXTENSION;

	/**
	 * name
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#name}.
	 * <p>
	 * Relates a named graph to the name by which it may be referenced in a
	 * FROM/FROM NAMED clause. The object of the sd:name property is an IRI.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#name">name</a>
	 */
	public static final URI NAME;

	/**
	 * named graph
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#namedGraph}.
	 * <p>
	 * Relates an instance of sd:GraphCollection (or its subclass sd:Dataset) to
	 * the description of one of its named graphs. The description of such a
	 * named graph MUST include the sd:name property and MAY include the sd:graph
	 * property.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#namedGraph">namedGraph</a>
	 */
	public static final URI NAMED_GRAPH_PROPERTY;

	/**
	 * Named Graph
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#NamedGraph}.
	 * <p>
	 * An instance of sd:NamedGraph represents a named graph having a name (via
	 * sd:name) and an optional graph description (via sd:graph).
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#NamedGraph">NamedGraph</a>
	 */
	public static final URI NAMED_GRAPH_CLASS;

	/**
	 * property feature
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#propertyFeature}.
	 * <p>
	 * Relates an instance of sd:Service to a resource representing an
	 * implemented feature that extends the SPARQL Query or Update language and
	 * that is accessed by using the named property.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#propertyFeature">propertyFeature</a>
	 */
	public static final URI PROPERTY_FEATURE;

	/**
	 * Requires Dataset
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#RequiresDataset}.
	 * <p>
	 * sd:RequiresDataset, when used as the object of the sd:feature property,
	 * indicates that the SPARQL service requires an explicit dataset declaration
	 * (based on either FROM/FROM NAMED clauses in a query, USING/USING NAMED
	 * clauses in an update, or the appropriate SPARQL Protocol parameters).
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#RequiresDataset">RequiresDataset</a>
	 */
	public static final URI REQUIRES_DATASET;

	/**
	 * result format
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#resultFormat}.
	 * <p>
	 * Relates an instance of sd:Service to a format that is supported for
	 * serializing query results.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#resultFormat">resultFormat</a>
	 */
	public static final URI RESULT_FORMAT;

	/**
	 * Service
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#Service}.
	 * <p>
	 * An instance of sd:Service represents a SPARQL service made available via
	 * the SPARQL Protocol.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#Service">Service</a>
	 */
	public static final URI SERVICE;

	/**
	 * SPARQL 1.0 Query
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#SPARQL10Query}.
	 * <p>
	 * sd:SPARQL10Query is an sd:Language representing the SPARQL 1.0 Query
	 * language.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#SPARQL10Query">SPARQL10Query</a>
	 */
	public static final URI SPARQL_10_QUERY;

	/**
	 * SPARQL 1.1 Query
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#SPARQL11Query}.
	 * <p>
	 * sd:SPARQL11Query is an sd:Language representing the SPARQL 1.1 Query
	 * language.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#SPARQL11Query">SPARQL11Query</a>
	 */
	public static final URI SPARQL_11_QUERY;

	/**
	 * SPARQL 1.1 Update
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#SPARQL11Update}.
	 * <p>
	 * sd:SPARQLUpdate is an sd:Language representing the SPARQL 1.1 Update
	 * language.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#SPARQL11Update">SPARQL11Update</a>
	 */
	public static final URI SPARQL_11_UPDATE;

	/**
	 * supported entailment profile
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#supportedEntailmentProfile}.
	 * <p>
	 * Relates a named graph description with a resource representing a supported
	 * profile of the entailment regime (as declared by sd:entailmentRegime) used
	 * for basic graph pattern matching over that graph.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#supportedEntailmentProfile">supportedEntailmentProfile</a>
	 */
	public static final URI SUPPORTED_ENTAILMENT_PROFILE;

	/**
	 * supported language
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#supportedLanguage}.
	 * <p>
	 * Relates an instance of sd:Service to a SPARQL language (e.g. Query and
	 * Update) that it implements.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#supportedLanguage">supportedLanguage</a>
	 */
	public static final URI SUPPORTED_LANGUAGE;

	/**
	 * Union Default Graph
	 * <p>
	 * {@code http://www.w3.org/ns/sparql-service-description#UnionDefaultGraph}.
	 * <p>
	 * sd:UnionDefaultGraph, when used as the object of the sd:feature property,
	 * indicates that the default graph of the dataset used during query and
	 * update evaluation (when an explicit dataset is not specified) is comprised
	 * of the union of all the named graphs in that dataset.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/ns/sparql-service-description#UnionDefaultGraph">UnionDefaultGraph</a>
	 */
	public static final URI UNION_DEFAULT_GRAPH;

	static {
		ValueFactory factory = ValueFactoryImpl.getInstance();

		AGGREGATE = factory.createURI(SD.NAMESPACE, "Aggregate");
		AVAILBLE_GRAPHS = factory.createURI(SD.NAMESPACE, "availableGraphs");
		BASIC_FEDERATED_QUERY = factory.createURI(SD.NAMESPACE, "BasicFederatedQuery");
		DATASET = factory.createURI(SD.NAMESPACE, "Dataset");
		DEFAULT_DATASET = factory.createURI(SD.NAMESPACE, "defaultDataset");
		DEFAULT_ENTAILMENT_REGIME = factory.createURI(SD.NAMESPACE, "defaultEntailmentRegime");
		DEFAULT_GRAPH = factory.createURI(SD.NAMESPACE, "defaultGraph");
		DEFAULT_SUPPORTED_ENTAILMENT_PROFILE = factory.createURI(SD.NAMESPACE,
				"defaultSupportedEntailmentProfile");
		DEREFERENCES_URIS = factory.createURI(SD.NAMESPACE, "DereferencesURIs");
		EMPTY_GRAPHS = factory.createURI(SD.NAMESPACE, "EmptyGraphs");
		ENDPOINT = factory.createURI(SD.NAMESPACE, "endpoint");
		ENTAILMENT_PROFILE = factory.createURI(SD.NAMESPACE, "EntailmentProfile");
		ENTAILMENT_REGIME_CLASS = factory.createURI(SD.NAMESPACE, "EntailmentRegime");
		ENTAILMENT_REGIME_PROPERTY = factory.createURI(SD.NAMESPACE, "entailmentRegime");
		EXTENSION_AGGREGATE = factory.createURI(SD.NAMESPACE, "extensionAggregate");
		EXTENSION_FUNCTION = factory.createURI(SD.NAMESPACE, "extensionFunction");
		FEATURE_CLASS = factory.createURI(SD.NAMESPACE, "Feature");
		FEATURE_PROPERTY = factory.createURI(SD.NAMESPACE, "feature");
		FUNCTION = factory.createURI(SD.NAMESPACE, "Function");
		GRAPH_PROPERTY = factory.createURI(SD.NAMESPACE, "graph");
		GRAPH_CLASS = factory.createURI(SD.NAMESPACE, "Graph");
		GRAPH_COLLECTION = factory.createURI(SD.NAMESPACE, "GraphCollection");
		INPUT_FORMAT = factory.createURI(SD.NAMESPACE, "inputFormat");
		LANGUAGE = factory.createURI(SD.NAMESPACE, "Language");
		LANGUAGE_EXTENSION = factory.createURI(SD.NAMESPACE, "languageExtension");
		NAME = factory.createURI(SD.NAMESPACE, "name");
		NAMED_GRAPH_PROPERTY = factory.createURI(SD.NAMESPACE, "namedGraph");
		NAMED_GRAPH_CLASS = factory.createURI(SD.NAMESPACE, "NamedGraph");
		PROPERTY_FEATURE = factory.createURI(SD.NAMESPACE, "propertyFeature");
		REQUIRES_DATASET = factory.createURI(SD.NAMESPACE, "RequiresDataset");
		RESULT_FORMAT = factory.createURI(SD.NAMESPACE, "resultFormat");
		SERVICE = factory.createURI(SD.NAMESPACE, "Service");
		SPARQL_10_QUERY = factory.createURI(SD.NAMESPACE, "SPARQL10Query");
		SPARQL_11_QUERY = factory.createURI(SD.NAMESPACE, "SPARQL11Query");
		SPARQL_11_UPDATE = factory.createURI(SD.NAMESPACE, "SPARQL11Update");
		SUPPORTED_ENTAILMENT_PROFILE = factory.createURI(SD.NAMESPACE, "supportedEntailmentProfile");
		SUPPORTED_LANGUAGE = factory.createURI(SD.NAMESPACE, "supportedLanguage");
		UNION_DEFAULT_GRAPH = factory.createURI(SD.NAMESPACE, "UnionDefaultGraph");
	}

	private SD() {
		// static access only
	}
}
