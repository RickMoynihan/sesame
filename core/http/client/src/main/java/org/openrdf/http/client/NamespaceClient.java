/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2002-2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.http.client;

import org.openrdf.http.client.connections.HTTPConnectionPool;
import org.openrdf.http.client.helpers.StoreClient;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.store.StoreException;

/**
 * @author Herko ter Horst
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class NamespaceClient {
	private StoreClient client;

	public NamespaceClient(HTTPConnectionPool namespaces) {
		this.client = new StoreClient(namespaces);
	}

	public TupleQueryResult list()
		throws StoreException
	{
		return client.list();
	}

	public void list(TupleQueryResultHandler handler)
		throws TupleQueryResultHandlerException, StoreException
	{
		client.list(handler);
	}

	public String get(String prefix)
		throws StoreException
	{
		return client.get(prefix, String.class);
	}

	public void put(String prefix, String name)
		throws StoreException
	{
		client.put(prefix, name);
	}

	public void delete(String prefix)
		throws StoreException
	{
		client.delete(prefix);
	}

	public void delete()
		throws StoreException
	{
		client.delete();
	}

}
