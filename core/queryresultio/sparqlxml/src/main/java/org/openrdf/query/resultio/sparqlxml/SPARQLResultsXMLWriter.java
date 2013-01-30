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
package org.openrdf.query.resultio.sparqlxml;

import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BINDING_NAME_ATT;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BINDING_TAG;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BNODE_TAG;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.HEAD_TAG;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.HREF_ATT;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.LINK_TAG;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.LITERAL_DATATYPE_ATT;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.LITERAL_LANG_ATT;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.LITERAL_TAG;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.NAMESPACE;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.RESULT_SET_TAG;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.RESULT_TAG;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.ROOT_TAG;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.URI_TAG;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.VAR_NAME_ATT;
import static org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLConstants.VAR_TAG;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import info.aduna.xml.XMLWriter;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;

/**
 * A {@link TupleQueryResultWriter} that writes tuple query results in the <a
 * href="http://www.w3.org/TR/rdf-sparql-XMLres/">SPARQL Query Results XML
 * Format</a>.
 */
public class SPARQLResultsXMLWriter extends SPARQLXMLWriterBase<TupleQueryResultFormat> implements
		TupleQueryResultWriter
{

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SPARQLResultsXMLWriter(OutputStream out) {
		super(out);
	}

	public SPARQLResultsXMLWriter(XMLWriter xmlWriter) {
		super(xmlWriter);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public final TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.SPARQL;
	}

	@Override
	public final TupleQueryResultFormat getQueryResultFormat() {
		return getTupleQueryResultFormat();
	}

	@Override
	public void startDocument()
		throws TupleQueryResultHandlerException
	{
		documentOpen = true;
		headerComplete = false;

		try {
			xmlWriter.startDocument();

			xmlWriter.setAttribute("xmlns", NAMESPACE);
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void handleStylesheet(String url)
		throws TupleQueryResultHandlerException
	{
		try {
			xmlWriter.writeStylesheet(url);
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void startHeader()
		throws TupleQueryResultHandlerException
	{
		try {
			xmlWriter.startTag(ROOT_TAG);

			xmlWriter.startTag(HEAD_TAG);
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void handleLinks(List<String> linkUrls)
		throws TupleQueryResultHandlerException
	{
		try {
			// Write link URLs
			for (String name : linkUrls) {
				xmlWriter.setAttribute(HREF_ATT, name);
				xmlWriter.emptyElement(LINK_TAG);
			}
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void endHeader()
		throws TupleQueryResultHandlerException
	{
		try {
			xmlWriter.endTag(HEAD_TAG);

			// Write start of results, which must always exist, even if there are
			// no result bindings
			xmlWriter.startTag(RESULT_SET_TAG);

			headerComplete = true;
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void startQueryResult(List<String> bindingNames)
		throws TupleQueryResultHandlerException
	{
		if (!documentOpen) {
			startDocument();
			startHeader();
		}
		try {
			// Write binding names
			for (String name : bindingNames) {
				xmlWriter.setAttribute(VAR_NAME_ATT, name);
				xmlWriter.emptyElement(VAR_TAG);
			}
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void endQueryResult()
		throws TupleQueryResultHandlerException
	{
		if (!headerComplete) {
			endHeader();
		}
		try {
			xmlWriter.endTag(RESULT_SET_TAG);
			endDocument();
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void handleSolution(BindingSet bindingSet)
		throws TupleQueryResultHandlerException
	{
		if (!headerComplete) {
			endHeader();
		}
		try {
			xmlWriter.startTag(RESULT_TAG);

			for (Binding binding : bindingSet) {
				xmlWriter.setAttribute(BINDING_NAME_ATT, binding.getName());
				xmlWriter.startTag(BINDING_TAG);

				writeValue(binding.getValue());

				xmlWriter.endTag(BINDING_TAG);
			}

			xmlWriter.endTag(RESULT_TAG);
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	private void writeValue(Value value)
		throws IOException
	{
		if (value instanceof URI) {
			writeURI((URI)value);
		}
		else if (value instanceof BNode) {
			writeBNode((BNode)value);
		}
		else if (value instanceof Literal) {
			writeLiteral((Literal)value);
		}
	}

	private void writeURI(URI uri)
		throws IOException
	{
		xmlWriter.textElement(URI_TAG, uri.toString());
	}

	private void writeBNode(BNode bNode)
		throws IOException
	{
		xmlWriter.textElement(BNODE_TAG, bNode.getID());
	}

	private void writeLiteral(Literal literal)
		throws IOException
	{
		if (literal.getLanguage() != null) {
			xmlWriter.setAttribute(LITERAL_LANG_ATT, literal.getLanguage());
		}
		else if (literal.getDatatype() != null) {
			URI datatype = literal.getDatatype();
			xmlWriter.setAttribute(LITERAL_DATATYPE_ATT, datatype.toString());
		}

		xmlWriter.textElement(LITERAL_TAG, literal.getLabel());
	}

}
