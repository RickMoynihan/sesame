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
package org.openrdf.repository.event.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.event.NotifyingRepositoryConnection;
import org.openrdf.repository.event.RepositoryConnectionListener;

/**
 * This broadcaster is used by the RepositoryBroadcaster to wrap the delegate
 * repository connection. Listeners are notified of changes after they have
 * occurred.
 * 
 * @author James Leigh
 * @author Herko ter Horst
 */
public class NotifyingRepositoryConnectionWrapper extends RepositoryConnectionWrapper implements
		NotifyingRepositoryConnection
{

	/*-----------*
	 * Variables *
	 *-----------*/

	private boolean activated = false;

	private boolean reportDeltas = false;

	private Set<RepositoryConnectionListener> listeners = new CopyOnWriteArraySet<RepositoryConnectionListener>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public NotifyingRepositoryConnectionWrapper(Repository repository, RepositoryConnection connection) {
		super(repository, connection);
	}

	public NotifyingRepositoryConnectionWrapper(Repository repository, RepositoryConnection connection,
			boolean reportDeltas)
	{
		this(repository, connection);
		setReportDeltas(reportDeltas);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public boolean reportDeltas() {
		return reportDeltas;
	}

	public void setReportDeltas(boolean reportDeltas) {
		this.reportDeltas = reportDeltas;
	}

	/**
	 * Registers a <tt>RepositoryConnectionListener</tt> that will receive
	 * notifications of operations that are performed on this connection.
	 */
	public void addRepositoryConnectionListener(RepositoryConnectionListener listener) {
		listeners.add(listener);
		activated = true;
	}

	/**
	 * Removes a registered <tt>RepositoryConnectionListener</tt> from this
	 * connection.
	 */
	public void removeRepositoryConnectionListener(RepositoryConnectionListener listener) {
		listeners.remove(listener);
		activated = !listeners.isEmpty();
	}

	@Override
	protected boolean isDelegatingAdd() {
		return !activated;
	}

	@Override
	protected boolean isDelegatingRemove() {
		return !activated;
	}

	@Override
	public void addWithoutCommit(Resource subject, URI predicate, Value object, Resource... contexts)
		throws RepositoryException
	{
		boolean reportEvent = activated;

		if (reportEvent && reportDeltas()) {
			// Only report if the statement is not present yet
			reportEvent = !getDelegate().hasStatement(subject, predicate, object, false, contexts);
		}

		getDelegate().add(subject, predicate, object, contexts);

		if (reportEvent) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.add(getDelegate(), subject, predicate, object, contexts);
			}
		}
	}

	@Override
	public void clear(Resource... contexts)
		throws RepositoryException
	{
		if (activated && reportDeltas()) {
			removeWithoutCommit(null, null, null, contexts);
		}
		else if (activated) {
			getDelegate().clear(contexts);
			for (RepositoryConnectionListener listener : listeners) {
				listener.clear(getDelegate(), contexts);
			}
		}
		else {
			getDelegate().clear(contexts);
		}
	}

	@Override
	public void close()
		throws RepositoryException
	{
		getDelegate().close();

		if (activated) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.close(getDelegate());
			}
		}
	}

	@Override
	public void commit()
		throws RepositoryException
	{
		getDelegate().commit();

		if (activated) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.commit(getDelegate());
			}
		}
	}

	@Override
	public void removeWithoutCommit(Resource subj, URI pred, Value obj, Resource... ctx)
		throws RepositoryException
	{
		if (activated && reportDeltas()) {
			RepositoryResult<Statement> stmts;
			stmts = getDelegate().getStatements(subj, pred, obj, false, ctx);
			List<Statement> list = new ArrayList<Statement>();
			try {
				while (stmts.hasNext()) {
					list.add(stmts.next());
				}
			}
			finally {
				stmts.close();
			}
			getDelegate().remove(subj, pred, obj, ctx);
			for (RepositoryConnectionListener listener : listeners) {
				for (Statement stmt : list) {
					Resource s = stmt.getSubject();
					URI p = stmt.getPredicate();
					Value o = stmt.getObject();
					Resource c = stmt.getContext();
					listener.remove(getDelegate(), s, p, o, c);
				}
			}
		}
		else if (activated) {
			getDelegate().remove(subj, pred, obj, ctx);
			for (RepositoryConnectionListener listener : listeners) {
				listener.remove(getDelegate(), subj, pred, obj, ctx);
			}
		}
		else {
			getDelegate().remove(subj, pred, obj, ctx);
		}
	}

	@Override
	public void removeNamespace(String prefix)
		throws RepositoryException
	{
		getDelegate().removeNamespace(prefix);

		if (activated) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.removeNamespace(getDelegate(), prefix);
			}
		}
	}

	@Override
	public void clearNamespaces()
		throws RepositoryException
	{
		if (activated && reportDeltas()) {
			RepositoryResult<Namespace> namespaces;
			namespaces = getDelegate().getNamespaces();
			List<String> prefix = new ArrayList<String>();
			try {
				while (namespaces.hasNext()) {
					Namespace ns = namespaces.next();
					prefix.add(ns.getPrefix());
				}
			}
			finally {
				namespaces.close();
			}
			getDelegate().clearNamespaces();
			for (String p : prefix) {
				removeNamespace(p);
			}
		}
		else if (activated) {
			getDelegate().clearNamespaces();
			for (RepositoryConnectionListener listener : listeners) {
				listener.clearNamespaces(getDelegate());
			}
		}
		else {
			getDelegate().clearNamespaces();
		}
	}

	@Override
	public void begin()
		throws RepositoryException
	{
		getDelegate().begin();

		if (activated) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.begin(getDelegate());
			}
		}
	}

	@Override
	public void rollback()
		throws RepositoryException
	{
		getDelegate().rollback();

		if (activated) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.rollback(getDelegate());
			}
		}
	}

	@Override
	@Deprecated
	public void setAutoCommit(boolean autoCommit)
		throws RepositoryException
	{
		boolean wasAutoCommit = isAutoCommit();
		getDelegate().setAutoCommit(autoCommit);

		if (activated && wasAutoCommit != autoCommit) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.setAutoCommit(getDelegate(), autoCommit);
			}
			if (autoCommit) {
				for (RepositoryConnectionListener listener : listeners) {
					listener.commit(getDelegate());
				}
			}
		}
	}

	@Override
	public void setNamespace(String prefix, String name)
		throws RepositoryException
	{
		getDelegate().setNamespace(prefix, name);

		if (activated) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.setNamespace(getDelegate(), prefix, name);
			}
		}
	}

	@Override
	public Update prepareUpdate(final QueryLanguage ql, final String update, final String baseURI)
		throws MalformedQueryException, RepositoryException
	{
		if (activated) {
			return new Update() {

				private final RepositoryConnection conn = getDelegate();

				private final Update delegate = conn.prepareUpdate(ql, update, baseURI);

				public void execute()
					throws UpdateExecutionException
				{
					delegate.execute();
					if (activated) {
						for (RepositoryConnectionListener listener : listeners) {
							listener.execute(conn, ql, update, baseURI, delegate);
						}
					}
				}

				public void setBinding(String name, Value value) {
					delegate.setBinding(name, value);
				}

				public void removeBinding(String name) {
					delegate.removeBinding(name);
				}

				public void clearBindings() {
					delegate.clearBindings();
				}

				public BindingSet getBindings() {
					return delegate.getBindings();
				}

				public void setDataset(Dataset dataset) {
					delegate.setDataset(dataset);
				}

				public Dataset getDataset() {
					return delegate.getDataset();
				}

				public void setIncludeInferred(boolean includeInferred) {
					delegate.setIncludeInferred(includeInferred);
				}

				public boolean getIncludeInferred() {
					return delegate.getIncludeInferred();
				}

				@Override
				public void setMaxExecutionTime(int maxExecTime) {
					delegate.setMaxExecutionTime(maxExecTime);
				}

				@Override
				public int getMaxExecutionTime() {
					return delegate.getMaxExecutionTime();
				}
			};
		}
		else {
			return getDelegate().prepareUpdate(ql, update, baseURI);
		}
	}
}
