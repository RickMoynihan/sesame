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
package org.openrdf.query.algebra;

import org.openrdf.query.algebra.StatementPattern.Scope;

/**
 * @author jeen
 */
public class Clear extends QueryModelNodeBase implements UpdateExpr {

	private ValueConstant graph;

	private boolean silent;
	
	private Scope scope;
	
	public Clear() {
		super();
	}
	
	public Clear(ValueConstant graph) {
		super();
		setGraph(graph);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
		throws X
	{
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
		throws X
	{
		if (graph != null) {
			graph.visit(visitor);
		}
		super.visitChildren(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (graph == current) {
			setGraph((ValueConstant)replacement);
		}
		else {
			super.replaceChildNode(current, replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof Clear) {
			Clear o = (Clear)other;
			return silent == o.silent
					&& nullEquals(graph, o.graph)
					&& nullEquals(scope, o.scope);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = silent ? 1 :0;
		if(graph != null) {
			result ^= graph.hashCode();
		}
		if(scope != null) {
			result ^= scope.hashCode();
		}
		return result;
	}

	@Override
	public Clear clone() {
		Clear clone = new Clear();
		clone.setSilent(isSilent());
		if (getGraph() != null) {
			clone.setGraph(getGraph().clone());
		}
		return clone;
	}

	/**
	 * @param graph
	 *        The graph to set.
	 */
	public void setGraph(ValueConstant graph) {
		this.graph = graph;
	}

	/**
	 * @return Returns the graph.
	 */
	public ValueConstant getGraph() {
		return graph;
	}

	/**
	 * @param silent The silent to set.
	 */
	public void setSilent(boolean silent) {
		this.silent = silent;
	}

	/**
	 * @return Returns the silent.
	 */
	@Override
	public boolean isSilent() {
		return silent;
	}

	/**
	 * @param scope The scope to set.
	 */
	public void setScope(Scope scope) {
		this.scope = scope;
	}

	/**
	 * @return Returns the scope.
	 */
	public Scope getScope() {
		return scope;
	}

}
