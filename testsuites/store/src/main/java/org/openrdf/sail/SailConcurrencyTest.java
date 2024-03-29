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
package org.openrdf.sail;

import static org.junit.Assert.assertNotNull;

import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import info.aduna.iteration.CloseableIteration;

import org.openrdf.model.Resource;
import org.openrdf.model.impl.URIImpl;

/**
 * Tests concurrent read and write access to a Sail implementation.
 * 
 * @author Arjohn Kampman
 */
public abstract class SailConcurrencyTest {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final int MAX_STATEMENT_IDX = 1000;

	private static final long MAX_TEST_TIME = 30 * 1000;

	/*-----------*
	 * Variables *
	 *-----------*/

	private Sail store;

	private boolean m_failed;

	private boolean continueRunning;

	/*---------*
	 * Methods *
	 *---------*/

	@Before
	public void setUp()
		throws Exception
	{
		store = createSail();
		store.initialize();
	}

	protected abstract Sail createSail()
		throws SailException;

	@After
	public void tearDown()
		throws Exception
	{
		store.shutDown();
	}

	@Test
	public void testGetContextIDs()
		throws Exception
	{
		// Create one thread which writes statements to the repository, on a
		// number of named graphs.
		final Random insertRandomizer = new Random(12345L);
		final Random removeRandomizer = new Random(System.currentTimeMillis());

		Runnable writer = new Runnable() {

			public void run() {
				try {
					SailConnection connection = store.getConnection();
					try {
						while (continueRunning) {
							connection.begin();
							for (int i = 0; i < 10; i++) {
								insertTestStatement(connection, insertRandomizer.nextInt() % MAX_STATEMENT_IDX);
								removeTestStatement(connection, removeRandomizer.nextInt() % MAX_STATEMENT_IDX);
							}
							// System.out.print("*");
							connection.commit();
						}
					}
					finally {
						connection.close();
					}
				}
				catch (Throwable t) {
					continueRunning = false;
					fail("Writer failed", t);
				}
			}
		};

		// Create another which constantly calls getContextIDs() on the
		// connection.
		Runnable reader = new Runnable() {

			public void run() {
				try {
					SailConnection connection = store.getConnection();
					try {
						while (continueRunning) {
							CloseableIteration<? extends Resource, SailException> contextIter = connection.getContextIDs();
							try {
								int contextCount = 0;
								while (contextIter.hasNext()) {
									Resource context = contextIter.next();
									assertNotNull(context);
									contextCount++;
								}
//								 System.out.println("Found " + contextCount + " contexts");
							}
							finally {
								contextIter.close();
							}
						}
					}
					finally {
						connection.close();
					}
				}
				catch (Throwable t) {
					continueRunning = false;
					fail("Reader failed", t);
				}
			}
		};

		Thread readerThread1 = new Thread(reader);
		Thread readerThread2 = new Thread(reader);
		Thread writerThread1 = new Thread(writer);
		Thread writerThread2 = new Thread(writer);

		System.out.println("Running concurrency test...");

		continueRunning = true;
		readerThread1.start();
		readerThread2.start();
		writerThread1.start();
		writerThread2.start();

		readerThread1.join(MAX_TEST_TIME);

		continueRunning = false;

		readerThread1.join(1000);
		readerThread2.join(1000);
		writerThread1.join(1000);
		writerThread2.join(1000);

		if (hasFailed()) {
			Assert.fail("Test Failed");
		}
		else {
			System.out.println("Test succeeded");
		}
	}

	protected synchronized void fail(String message, Throwable t) {
		System.err.println(message);
		t.printStackTrace();
		m_failed = true;
	}

	protected synchronized boolean hasFailed() {
		return m_failed;
	}

	protected void insertTestStatement(SailConnection connection, int i)
		throws SailException
	{
		// System.out.print("+");
		connection.addStatement(new URIImpl("http://test#s" + i), new URIImpl("http://test#p" + i),
				new URIImpl("http://test#o" + i), new URIImpl("http://test#context_" + i));
	}

	protected void removeTestStatement(SailConnection connection, int i)
		throws SailException
	{
		// System.out.print("-");
		connection.removeStatements(new URIImpl("http://test#s" + i), new URIImpl("http://test#p" + i),
				new URIImpl("http://test#o" + i), new URIImpl("http://test#context_" + i));
	}
}
