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
package org.openrdf.sail.derived;

import org.openrdf.IsolationLevel;
import org.openrdf.sail.SailException;

/**
 * @author James Leigh
 */
public class DelegatingRdfSource implements RdfSource {

	private final RdfSource delegate;

	private final boolean releasing;

	private boolean released;

	/**
	 * @param delegate
	 * @param releasing
	 *        if {@link #release()} should be delegated
	 */
	public DelegatingRdfSource(RdfSource delegate, boolean releasing) {
		super();
		this.delegate = delegate;
		this.releasing = releasing;
	}

	public boolean isActive() {
		return !released && delegate.isActive();
	}

	public void release() {
		released = true;
		if (releasing) {
			delegate.release();
		}
	}

	public RdfSource fork()
		throws SailException
	{
		return delegate.fork();
	}

	public void prepare()
		throws SailException
	{
		delegate.prepare();
	}

	public void flush()
		throws SailException
	{
		delegate.flush();
	}

	public RdfSink sink(IsolationLevel level)
		throws SailException
	{
		return delegate.sink(level);
	}

	public RdfDataset snapshot(IsolationLevel level)
		throws SailException
	{
		return delegate.snapshot(level);
	}
}
