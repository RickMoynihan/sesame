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
package org.openrdf.rio.n3;

import java.io.OutputStream;
import java.io.Writer;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;

/**
 * An {@link RDFWriterFactory} for N3 writers.
 * 
 * @author Arjohn Kampman
 */
public class N3WriterFactory implements RDFWriterFactory {

	/**
	 * Returns {@link RDFFormat#N3}.
	 */
	public RDFFormat getRDFFormat() {
		return RDFFormat.N3;
	}

	/**
	 * Returns a new instance of {@link N3Writer}.
	 */
	public RDFWriter getWriter(OutputStream out) {
		return new N3Writer(out);
	}

	/**
	 * Returns a new instance of {@link N3Writer}.
	 */
	public RDFWriter getWriter(Writer writer) {
		return new N3Writer(writer);
	}
}
