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
package org.openrdf.sail.base;

import java.util.HashMap;
import java.util.Map;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.DistinctIteration;

import org.openrdf.IsolationLevel;
import org.openrdf.IsolationLevels;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.QueryRoot;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.EvaluationStrategy;
import org.openrdf.query.algebra.evaluation.TripleSource;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.openrdf.query.algebra.evaluation.impl.BindingAssigner;
import org.openrdf.query.algebra.evaluation.impl.CompareOptimizer;
import org.openrdf.query.algebra.evaluation.impl.ConjunctiveConstraintSplitter;
import org.openrdf.query.algebra.evaluation.impl.ConstantOptimizer;
import org.openrdf.query.algebra.evaluation.impl.DisjunctiveConstraintOptimizer;
import org.openrdf.query.algebra.evaluation.impl.EvaluationStrategyImpl;
import org.openrdf.query.algebra.evaluation.impl.FilterOptimizer;
import org.openrdf.query.algebra.evaluation.impl.IterativeEvaluationOptimizer;
import org.openrdf.query.algebra.evaluation.impl.OrderLimitOptimizer;
import org.openrdf.query.algebra.evaluation.impl.QueryJoinOptimizer;
import org.openrdf.query.algebra.evaluation.impl.QueryModelNormalizer;
import org.openrdf.query.algebra.evaluation.impl.SameTermFilterOptimizer;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.UnknownSailTransactionStateException;
import org.openrdf.sail.UpdateContext;
import org.openrdf.sail.helpers.NotifyingSailConnectionBase;
import org.openrdf.sail.helpers.SailBase;
import org.openrdf.sail.inferencer.InferencerConnection;

/**
 * A {@link SailConnection} implementation that is based on an {@link SailStore}
 * .
 * 
 * @author James Leigh
 */
public abstract class SailSourceConnection extends NotifyingSailConnectionBase
		implements InferencerConnection, FederatedServiceResolverClient
{

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The state of store for outstanding operations.
	 */
	private final Map<UpdateContext, SailDataset> datasets = new HashMap<UpdateContext, SailDataset>();

	/**
	 * Outstanding changes that are underway, but not yet realized, by an active
	 * operation.
	 */
	private final Map<UpdateContext, SailSink> explicitSinks = new HashMap<UpdateContext, SailSink>();

	/**
	 * Set of explicit statements that must not be inferred.
	 */
	private SailDataset explicitOnlyDataset;

	/**
	 * Set of inferred statements that have already been inferred earlier.
	 */
	private SailDataset inferredDataset;

	/**
	 * Outstanding inferred statements that are not yet flushed by a read
	 * operation.
	 */
	private SailSink inferredSink;

	/**
	 * {@link ValueFactory} used by this connection.
	 */
	private final ValueFactory vf;

	/**
	 * The backing {@link SailStore} used to manage the state.
	 */
	private final SailStore store;

	/**
	 * The default {@link IsolationLevel} when not otherwise specified.
	 */
	private final IsolationLevel defaultIsolationLevel;

	/**
	 * An {@link SailSource} of only explicit statements when in an isolated
	 * transaction.
	 */
	private SailSource explicitOnlyBranch;

	/**
	 * An {@link SailSource} of only inferred statements when in an isolated
	 * transaction.
	 */
	private SailSource inferredOnlyBranch;

	/**
	 * An {@link SailSource} of all statements when in an isolated transaction.
	 */
	private SailSource includeInferredBranch;

	/**
	 * Connection specific resolver.
	 */
	private FederatedServiceResolver federatedServiceResolver;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new {@link SailConnection}, using the given {@link SailStore}
	 * to manage the state.
	 * 
	 * @param sail
	 * @param store
	 * @param resolver
	 */
	protected SailSourceConnection(SailBase sail, SailStore store, FederatedServiceResolver resolver) {
		super(sail);
		this.vf = sail.getValueFactory();
		this.store = store;
		this.defaultIsolationLevel = sail.getDefaultIsolationLevel();
		this.federatedServiceResolver = resolver;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public FederatedServiceResolver getFederatedServiceResolver() {
		return federatedServiceResolver;
	}

	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		this.federatedServiceResolver = resolver;
	}

	protected EvaluationStrategy getEvaluationStrategy(Dataset dataset, TripleSource tripleSource) {
		return new EvaluationStrategyImpl(tripleSource, dataset, getFederatedServiceResolver());
	}

	@Override
	protected CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(
			TupleExpr tupleExpr, Dataset dataset, BindingSet bindings, boolean includeInferred)
				throws SailException
	{
		flush();
		logger.trace("Incoming query model:\n{}", tupleExpr);

		// Clone the tuple expression to allow for more aggresive optimizations
		tupleExpr = tupleExpr.clone();

		if (!(tupleExpr instanceof QueryRoot)) {
			// Add a dummy root node to the tuple expressions to allow the
			// optimizers to modify the actual root node
			tupleExpr = new QueryRoot(tupleExpr);
		}

		SailSource branch = branch(includeInferred);
		SailDataset rdfDataset = branch.dataset(getIsolationLevel());
		boolean releaseLock = true;

		try {

			TripleSource tripleSource = new SailDatasetTripleSource(vf, rdfDataset);
			EvaluationStrategy strategy = getEvaluationStrategy(dataset, tripleSource);

			new BindingAssigner().optimize(tupleExpr, dataset, bindings);
			new ConstantOptimizer(strategy).optimize(tupleExpr, dataset, bindings);
			new CompareOptimizer().optimize(tupleExpr, dataset, bindings);
			new ConjunctiveConstraintSplitter().optimize(tupleExpr, dataset, bindings);
			new DisjunctiveConstraintOptimizer().optimize(tupleExpr, dataset, bindings);
			new SameTermFilterOptimizer().optimize(tupleExpr, dataset, bindings);
			new QueryModelNormalizer().optimize(tupleExpr, dataset, bindings);
			new QueryJoinOptimizer(store.getEvaluationStatistics()).optimize(tupleExpr, dataset, bindings);
			// new SubSelectJoinOptimizer().optimize(tupleExpr, dataset,
			// bindings);
			new IterativeEvaluationOptimizer().optimize(tupleExpr, dataset, bindings);
			new FilterOptimizer().optimize(tupleExpr, dataset, bindings);
			new OrderLimitOptimizer().optimize(tupleExpr, dataset, bindings);

			logger.trace("Optimized query model:\n{}", tupleExpr);

			CloseableIteration<BindingSet, QueryEvaluationException> iter;
			iter = strategy.evaluate(tupleExpr, EmptyBindingSet.getInstance());
			iter = interlock(iter, rdfDataset, branch);
			releaseLock = false;
			return iter;
		}
		catch (QueryEvaluationException e) {
			throw new SailException(e);
		}
		finally {
			if (releaseLock) {
				rdfDataset.close();
				branch.close();
			}
		}
	}

	@Override
	protected void closeInternal()
		throws SailException
	{
		// no-op
	}

	@Override
	protected CloseableIteration<? extends Resource, SailException> getContextIDsInternal()
		throws SailException
	{
		flush();
		SailSource branch = branch(false);
		SailDataset snapshot = branch.dataset(getIsolationLevel());
		return SailClosingIteration.makeClosable(snapshot.getContextIDs(), snapshot, branch);
	}

	@Override
	protected CloseableIteration<? extends Statement, SailException> getStatementsInternal(Resource subj,
			URI pred, Value obj, boolean includeInferred, Resource... contexts)
				throws SailException
	{
		flush();
		SailSource branch = branch(includeInferred);
		SailDataset snapshot = branch.dataset(getIsolationLevel());
		return SailClosingIteration.makeClosable(snapshot.getStatements(subj, pred, obj, contexts), snapshot,
				branch);
	}

	@Override
	protected long sizeInternal(Resource... contexts)
		throws SailException
	{
		flush();
		CloseableIteration<? extends Statement, SailException> iter = new DistinctIteration<Statement, SailException>(
				getStatementsInternal(null, null, null, false, contexts));

		try {
			long size = 0L;

			while (iter.hasNext()) {
				iter.next();
				size++;
			}

			return size;
		}
		finally {
			iter.close();
		}
	}

	@Override
	protected CloseableIteration<? extends Namespace, SailException> getNamespacesInternal()
		throws SailException
	{
		SailSource branch = branch(false);
		SailDataset snapshot = branch.dataset(getIsolationLevel());
		return SailClosingIteration.makeClosable(snapshot.getNamespaces(), snapshot, branch);
	}

	@Override
	protected String getNamespaceInternal(String prefix)
		throws SailException
	{
		SailSource branch = branch(false);
		SailDataset snapshot = branch.dataset(getIsolationLevel());
		try {
			return snapshot.getNamespace(prefix);
		}
		finally {
			snapshot.close();
			branch.close();
		}
	}

	@Override
	protected void startTransactionInternal()
		throws SailException
	{
		assert explicitOnlyBranch == null;
		assert inferredOnlyBranch == null;
		assert includeInferredBranch == null;
		IsolationLevel level = getTransactionIsolation();
		if (!IsolationLevels.NONE.isCompatibleWith(level)) {
			// only create transaction branches if transaction is isolated
			explicitOnlyBranch = store.getExplicitSailSource().fork();
			inferredOnlyBranch = store.getInferredSailSource().fork();
			includeInferredBranch = new UnionSailSource(inferredOnlyBranch, explicitOnlyBranch);
		}
	}

	@Override
	protected void prepareInternal()
		throws SailException
	{
		if (includeInferredBranch != null) {
			includeInferredBranch.prepare();
		}
	}

	@Override
	protected void commitInternal()
		throws SailException
	{
		try {
			if (includeInferredBranch != null) {
				includeInferredBranch.flush();
			}
		}
		finally {
			if (includeInferredBranch != null) {
				includeInferredBranch.close();
				includeInferredBranch = null;
				explicitOnlyBranch = null;
				inferredOnlyBranch = null;
			}
		}
	}

	@Override
	protected void rollbackInternal()
		throws SailException
	{
		synchronized (datasets) {
			if (datasets.containsKey(null)) {
				datasets.remove(null).close();
			}
			if (explicitSinks.containsKey(null)) {
				explicitSinks.remove(null).close();
			}
			if (explicitOnlyDataset != null) {
				explicitOnlyDataset.close();
				explicitOnlyDataset = null;
			}
			if (inferredDataset != null) {
				inferredDataset.close();
				inferredDataset = null;
			}
			if (inferredSink != null) {
				inferredSink.close();
				inferredSink = null;
			}
		}
		if (includeInferredBranch != null) {
			includeInferredBranch.close();
			includeInferredBranch = null;
			explicitOnlyBranch = null;
			inferredOnlyBranch = null;
		}
	}

	@Override
	public void startUpdate(UpdateContext op)
		throws SailException
	{
		if (op != null) {
			IsolationLevel level = getIsolationLevel();
			flush();
			synchronized (datasets) {
				assert !datasets.containsKey(op);
				SailSource source;
				if (op.isIncludeInferred() && inferredOnlyBranch == null) {
					// IsolationLevels.NONE
					SailSource explicit = store.getExplicitSailSource();
					SailSource inferred = store.getInferredSailSource();
					source = new UnionSailSource(explicit, inferred);
				}
				else if (op.isIncludeInferred()) {
					source = new UnionSailSource(explicitOnlyBranch, inferredOnlyBranch);
				}
				else {
					source = branch(false);
				}
				datasets.put(op, source.dataset(level));
				explicitSinks.put(op, source.sink(level));
			}
		}
	}

	@Override
	public void addStatement(UpdateContext op, Resource subj, URI pred, Value obj, Resource... contexts)
		throws SailException
	{
		verifyIsOpen();
		verifyIsActive();
		synchronized (datasets) {
			if (op == null && !datasets.containsKey(null)) {
				SailSource source = branch(false);
				datasets.put(null, source.dataset(getIsolationLevel()));
				explicitSinks.put(null, source.sink(getIsolationLevel()));
			}
			assert explicitSinks.containsKey(op);
			add(subj, pred, obj, datasets.get(op), explicitSinks.get(op), contexts);
		}
		addStatementInternal(subj, pred, obj, contexts);
	}

	@Override
	public void removeStatement(UpdateContext op, Resource subj, URI pred, Value obj, Resource... contexts)
		throws SailException
	{
		verifyIsOpen();
		verifyIsActive();
		flush();
		synchronized (datasets) {
			if (op == null && !datasets.containsKey(null)) {
				SailSource source = branch(false);
				datasets.put(null, source.dataset(getIsolationLevel()));
				explicitSinks.put(null, source.sink(getIsolationLevel()));
			}
			assert explicitSinks.containsKey(op);
			remove(subj, pred, obj, datasets.get(op), explicitSinks.get(op), contexts);
		}
		removeStatementsInternal(subj, pred, obj, contexts);
	}

	@Override
	protected void endUpdateInternal(UpdateContext op)
		throws SailException
	{
		synchronized (datasets) {
			if (inferredSink != null) {
				try {
					inferredSink.flush();
				}
				finally {
					inferredSink.close();
					inferredSink = null;
				}
			}
			if (explicitOnlyDataset != null) {
				explicitOnlyDataset.close();
				explicitOnlyDataset = null;
			}
			if (inferredDataset != null) {
				inferredDataset.close();
				inferredDataset = null;
			}
			SailSink explicit = explicitSinks.remove(op);
			if (explicit != null) {
				try {
					explicit.flush();
				}
				finally {
					explicit.close();
					datasets.remove(op).close();
				}
			}
		}
	}

	public boolean addInferredStatement(Resource subj, URI pred, Value obj, Resource... contexts)
		throws SailException
	{
		verifyIsOpen();
		verifyIsActive();
		IsolationLevel level = getIsolationLevel();
		synchronized (datasets) {
			if (inferredSink == null) {
				SailSource branch = branch(true);
				inferredDataset = branch.dataset(level);
				inferredSink = branch.sink(level);
				explicitOnlyDataset = branch(false).dataset(level);
			}
			addStatementInternal(subj, pred, obj, contexts);
			boolean modified = false;
			if (contexts.length == 0) {
				if (!hasStatement(explicitOnlyDataset, subj, pred, obj, null)) {
					// only add inferred statements that aren't already explicit
					if (!hasStatement(inferredDataset, subj, pred, obj, null)) {
						// only report inferred statements that don't already
						// exist
						notifyStatementAdded(vf.createStatement(subj, pred, obj));
						modified = true;
					}
					inferredSink.approve(subj, pred, obj, null);
				}
			}
			else {
				for (Resource ctx : contexts) {
					if (!hasStatement(explicitOnlyDataset, subj, pred, obj, ctx)) {
						// only add inferred statements that aren't already
						// explicit
						if (!hasStatement(inferredDataset, subj, pred, obj, ctx)) {
							// only report inferred statements that don't
							// already exist
							notifyStatementAdded(vf.createStatement(subj, pred, obj, ctx));
							modified = true;
						}
						inferredSink.approve(subj, pred, obj, ctx);
					}
				}
			}
			return modified;
		}
	}

	private boolean add(Resource subj, URI pred, Value obj, SailDataset dataset, SailSink sink,
			Resource... contexts)
				throws SailException
	{
		boolean modified = false;
		if (contexts.length == 0) {
			if (!hasStatement(dataset, subj, pred, obj, null)) {
				notifyStatementAdded(vf.createStatement(subj, pred, obj));
				modified = true;
			}
			sink.approve(subj, pred, obj, null);
		}
		else {
			for (Resource ctx : contexts) {
				if (!hasStatement(dataset, subj, pred, obj, ctx)) {
					notifyStatementAdded(vf.createStatement(subj, pred, obj, ctx));
					modified = true;
				}
				sink.approve(subj, pred, obj, ctx);
			}
		}
		return modified;
	}

	public boolean removeInferredStatement(Resource subj, URI pred, Value obj, Resource... contexts)
		throws SailException
	{
		verifyIsOpen();
		verifyIsActive();
		synchronized (datasets) {
			IsolationLevel level = getIsolationLevel();
			if (inferredSink == null) {
				SailSource branch = branch(true);
				inferredDataset = branch.dataset(level);
				inferredSink = branch.sink(level);
				explicitOnlyDataset = branch(false).dataset(level);
			}
			removeStatementsInternal(subj, pred, obj, contexts);
			return remove(subj, pred, obj, inferredDataset, inferredSink, contexts);
		}
	}

	private boolean remove(Resource subj, URI pred, Value obj, SailDataset dataset, SailSink sink,
			Resource... contexts)
				throws SailException
	{
		boolean statementsRemoved = false;
		CloseableIteration<? extends Statement, SailException> iter;
		iter = dataset.getStatements(subj, pred, obj, contexts);
		try {
			while (iter.hasNext()) {
				Statement st = iter.next();
				sink.deprecate(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
				statementsRemoved = true;
				notifyStatementRemoved(st);
			}
		}
		finally {
			iter.close();
		}
		return statementsRemoved;
	}

	@Override
	protected void clearInternal(Resource... contexts)
		throws SailException
	{
		verifyIsOpen();
		verifyIsActive();
		synchronized (datasets) {
			if (!datasets.containsKey(null)) {
				SailSource source = branch(false);
				datasets.put(null, source.dataset(getIsolationLevel()));
				explicitSinks.put(null, source.sink(getIsolationLevel()));
			}
			assert explicitSinks.containsKey(null);
			if (this.hasConnectionListeners()) {
				remove(null, null, null, datasets.get(null), explicitSinks.get(null), contexts);
			}
			explicitSinks.get(null).clear(contexts);
		}
	}

	public void clearInferred(Resource... contexts)
		throws SailException
	{
		verifyIsOpen();
		verifyIsActive();
		synchronized (datasets) {
			if (inferredSink == null) {
				IsolationLevel level = getIsolationLevel();
				SailSource branch = branch(true);
				inferredDataset = branch.dataset(level);
				inferredSink = branch.sink(level);
				explicitOnlyDataset = branch(false).dataset(level);
			}
			if (this.hasConnectionListeners()) {
				remove(null, null, null, inferredDataset, inferredSink, contexts);
			}
			inferredSink.clear(contexts);
		}
	}

	public void flushUpdates()
		throws SailException
	{
		flush();
	}

	@Override
	protected void setNamespaceInternal(String prefix, String name)
		throws SailException
	{
		SailSource branch = branch(false);
		SailSink sink = branch.sink(getTransactionIsolation());
		try {
			sink.setNamespace(prefix, name);
			sink.flush();
		}
		finally {
			sink.close();
			branch.close();
		}
	}

	@Override
	protected void removeNamespaceInternal(String prefix)
		throws SailException
	{
		SailSource branch = branch(false);
		SailSink sink = branch.sink(getTransactionIsolation());
		try {
			sink.removeNamespace(prefix);
			sink.flush();
		}
		finally {
			sink.close();
			branch.close();
		}
	}

	@Override
	protected void clearNamespacesInternal()
		throws SailException
	{
		SailSource branch = branch(false);
		SailSink sink = branch.sink(getTransactionIsolation());
		try {
			sink.clearNamespaces();
			sink.flush();
		}
		finally {
			sink.close();
			branch.close();
		}
	}

	/*-------------------------------------*
	 * Inner class MemEvaluationStatistics *
	 *-------------------------------------*/

	private IsolationLevel getIsolationLevel()
		throws UnknownSailTransactionStateException
	{
		if (isActive()) {
			return super.getTransactionIsolation();
		}
		else {
			return defaultIsolationLevel;
		}
	}

	/**
	 * @return read operation {@link SailSource}
	 * @throws SailException
	 */
	private SailSource branch(boolean includeinferred)
		throws SailException
	{
		boolean active = isActive();
		IsolationLevel level = getIsolationLevel();
		boolean isolated = !IsolationLevels.NONE.isCompatibleWith(level);
		if (includeinferred && active && isolated) {
			// use the transaction branch
			return new DelegatingSailSource(includeInferredBranch, false);
		}
		else if (active && isolated) {
			// use the transaction branch
			return new DelegatingSailSource(explicitOnlyBranch, false);
		}
		else if (includeinferred && active) {
			// don't actually branch source
			return new UnionSailSource(store.getInferredSailSource(), store.getExplicitSailSource());
		}
		else if (active) {
			// don't actually branch source
			return store.getExplicitSailSource();
		}
		else if (includeinferred) {
			// create a new branch for read operation
			return new UnionSailSource(store.getInferredSailSource().fork(),
					store.getExplicitSailSource().fork());
		}
		else {
			// create a new branch for read operation
			return store.getExplicitSailSource().fork();
		}
	}

	private <T, X extends Exception> CloseableIteration<T, QueryEvaluationException> interlock(
			CloseableIteration<T, QueryEvaluationException> iter, SailClosable... closes)
	{
		return new SailClosingIteration<T, QueryEvaluationException>(iter, closes) {

			protected void handleSailException(SailException e)
				throws QueryEvaluationException
			{
				throw new QueryEvaluationException(e);
			}
		};
	}

	private boolean hasStatement(SailDataset dataset, Resource subj, URI pred, Value obj, Resource ctx)
		throws SailException
	{
		CloseableIteration<? extends Statement, SailException> iter;
		iter = dataset.getStatements(subj, pred, obj, ctx);
		try {
			return iter.hasNext();
		}
		finally {
			iter.close();
		}
	}
}
