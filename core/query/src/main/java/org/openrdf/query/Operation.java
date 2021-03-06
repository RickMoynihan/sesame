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
package org.openrdf.query;

import org.openrdf.model.Value;

/**
 * An operation (e.g. a query or an update) on a repository that can be
 * formulated in one of the supported query languages (for example SeRQL or
 * SPARQL). It allows one to predefine bindings in the operation to be able to
 * reuse the same operation with different bindings.
 * 
 * @author Jeen
 */
public interface Operation {

	/**
	 * Binds the specified variable to the supplied value. Any value that was
	 * previously bound to the specified value will be overwritten.
	 * 
	 * @param name
	 *        The name of the variable that should be bound.
	 * @param value
	 *        The (new) value for the specified variable.
	 */
	public void setBinding(String name, Value value);

	/**
	 * Removes a previously set binding on the supplied variable. Calling this
	 * method with an unbound variable name has no effect.
	 * 
	 * @param name
	 *        The name of the variable from which the binding is to be removed.
	 */
	public void removeBinding(String name);

	/**
	 * Removes all previously set bindings.
	 */
	public void clearBindings();

	/**
	 * Retrieves the bindings that have been set on this operation.
	 * 
	 * @return A (possibly empty) set of operation variable bindings.
	 * @see #setBinding(String, Value)
	 */
	public BindingSet getBindings();

	/**
	 * Specifies the dataset against which to execute an operation, overriding
	 * any dataset that is specified in the operation itself.
	 */
	public void setDataset(Dataset dataset);

	/**
	 * Gets the dataset that has been set using {@link #setDataset(Dataset)}, if
	 * any.
	 */
	public Dataset getDataset();

	/**
	 * Determine whether evaluation results of this operation should include
	 * inferred statements (if any inferred statements are present in the
	 * repository). The default setting is 'true'.
	 * 
	 * @param includeInferred
	 *        indicates whether inferred statements should be included in the
	 *        result.
	 */
	public void setIncludeInferred(boolean includeInferred);

	/**
	 * Returns whether or not this operation will return inferred statements (if
	 * any are present in the repository).
	 * 
	 * @return <tt>true</tt> if inferred statements will be returned,
	 *         <tt>false</tt> otherwise.
	 */
	public boolean getIncludeInferred();

	/**
	 * Specifies the maximum time that an operation is allowed to run. The
	 * operation will be interrupted when it exceeds the time limit. Any
	 * consecutive requests to fetch query results will result in
	 * {@link QueryInterruptedException}s or {@link UpdateInterruptedException}s
	 * (depending on whether the operation is a query or an update).
	 * 
	 * @param maxQueryTime
	 *        The maximum query time, measured in seconds. A negative or zero
	 *        value indicates an unlimited execution time (which is the default).
	 * @since 2.8.0
	 */
	public void setMaxExecutionTime(int maxExecTime);

	/**
	 * Returns the maximum operation execution time.
	 * 
	 * @return The maximum operation execution time, measured in seconds.
	 * @see #setMaxExecutionTime(int)
	 * @since 2.8.0
	 */
	public int getMaxExecutionTime();

}
