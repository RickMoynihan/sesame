package org.openrdf.sail.federation;

import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.store.StoreException;

/**
 * Echos all write operations to all members.
 * 
 * @author James Leigh
 */
class WriteToAllConnection extends FederationConnection {

	private boolean closed;

	public WriteToAllConnection(Federation federation, List<RepositoryConnection> members) {
		super(federation, members);
	}

	public boolean isOpen()
		throws StoreException
	{
		return !closed;
	}

	public void close()
		throws StoreException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection member)
				throws StoreException
			{
				member.close();
			}
		});
		closed = true;
	}

	public void begin()
		throws StoreException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection member)
				throws StoreException
			{
				member.setAutoCommit(false);
			}
		});
	}

	public void rollback()
		throws StoreException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection member)
				throws StoreException
			{
				member.rollback();
				member.setAutoCommit(true);
			}
		});
	}

	public void commit()
		throws StoreException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection member)
				throws StoreException
			{
				member.commit();
				member.setAutoCommit(false);
			}
		});
	}

	public void setNamespace(final String prefix, final String name)
		throws StoreException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection member)
				throws StoreException
			{
				member.setNamespace(prefix, name);
			}
		});
	}

	public void clearNamespaces()
		throws StoreException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection member)
				throws StoreException
			{
				member.clearNamespaces();
			}
		});
	}

	public void removeNamespace(final String prefix)
		throws StoreException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection member)
				throws StoreException
			{
				member.removeNamespace(prefix);
			}
		});
	}

	public void addStatement(final Resource subj, final URI pred, final Value obj, final Resource... contexts)
		throws StoreException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection member)
				throws StoreException
			{
				member.add(subj, pred, obj, contexts);
			}
		});
	}

	public void removeStatements(final Resource subj, final URI pred, final Value obj,
			final Resource... contexts)
		throws StoreException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection member)
				throws StoreException
			{
				member.removeMatch(subj, pred, obj, contexts);
			}
		});
	}

}
