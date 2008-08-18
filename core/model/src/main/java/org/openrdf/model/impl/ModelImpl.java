/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.model.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import info.aduna.collections.iterators.FilterIterator;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 * @author James Leigh
 */
@SuppressWarnings("unchecked")
public class ModelImpl extends AbstractSet<Statement> implements Model {

	private static final long serialVersionUID = -9161104123818983614L;

	private static final Resource[] NULL_CTX = new Resource[] { null };

	transient Map<Value, ModelNode> values;

	transient Set<ModelStatement> statements;

	public ModelImpl() {
		super();
		values = new HashMap<Value, ModelNode>();
		statements = new HashSet<ModelStatement>();
	}

	public ModelImpl(Collection<? extends Statement> c) {
		super();
		values = new HashMap<Value, ModelNode>(c.size() * 2);
		statements = new HashSet<ModelStatement>(c.size());
		addAll(c);
	}

	public int size() {
		return statements.size();
	}

	@Override
	public boolean add(Statement st) {
		return add(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
	}

	public boolean add(Resource subj, URI pred, Value obj, Resource... contexts) {
		Resource[] ctxs = contexts;
		if (ctxs == null || ctxs.length == 0) {
			ctxs = NULL_CTX;
		}
		boolean changed = false;
		for (Resource ctx : ctxs) {
			ModelNode<Resource> s = asGraphNode(subj);
			ModelNode<URI> p = asGraphNode(pred);
			ModelNode<Value> o = asGraphNode(obj);
			ModelNode<Resource> c = asGraphNode(ctx);
			ModelStatement st = new ModelStatement(s, p, o, c);
			changed |= addModelStatement(st);
		}
		return changed;
	}

	@Override
	public void clear() {
		values.clear();
		statements.clear();
	}

	@Override
	public boolean remove(Object o) {
		if (o instanceof Statement) {
			Iterator iter = find((Statement)o);
			if (iter.hasNext()) {
				iter.next();
				iter.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean contains(Object o) {
		if (o instanceof Statement) {
			return find((Statement)o).hasNext();
		}
		return false;
	}

	public Iterator iterator() {
		return match(null, null, null);
	}

	public boolean contains(Resource subj, URI pred, Value obj, Resource... contexts) {
		return match(subj, pred, obj, contexts).hasNext();
	}

	public boolean remove(Resource subj, URI pred, Value obj, Resource... contexts) {
		Iterator iter = match(subj, pred, obj, contexts);
		if (!iter.hasNext())
			return false;
		while (iter.hasNext()) {
			iter.next();
			iter.remove();
		}
		return true;
	}

	public boolean clear(Resource context) {
		return remove(null, null, null, context);
	}

	public Set<Statement> filter(final Resource subj, final URI pred, final Value obj,
			final Resource... contexts)
	{
		return new FilteredSet() {

			@Override
			public void clear() {
				ModelImpl.this.remove(subj, pred, obj, contexts);
			}

			@Override
			protected ModelIterator statementIterator() {
				return match(subj, pred, obj, contexts);
			}

			@Override
			protected boolean accept(Statement st) {
				return matches(st, subj, pred, obj, contexts);
			}
		};
	}

	public Set<Resource> contexts(final Resource subj, final URI pred, final Value obj) {
		return new ValueSet<Resource>() {

			@Override
			public boolean contains(Object o) {
				if (o instanceof Resource || o == null) {
					return ModelImpl.this.contains(subj, pred, obj, (Resource)o);
				}
				return false;
			}

			@Override
			public boolean remove(Object o) {
				if (o instanceof Resource || o == null)
					return ModelImpl.this.remove(subj, pred, obj, (Resource)o);
				return false;
			}

			@Override
			public boolean add(Resource ctx) {
				if (subj == null || pred == null || obj == null)
					throw new UnsupportedOperationException("Incomplete statement");
				if (contains(ctx))
					return false;
				return ModelImpl.this.add(subj, pred, obj, ctx);
			}

			@Override
			public void clear() {
				ModelImpl.this.remove(subj, pred, obj);
			}

			@Override
			protected ModelIterator statementIterator() {
				return match(subj, pred, obj);
			}

			@Override
			protected ModelNode<Resource> node(ModelStatement st) {
				return st.ctx;
			}

			@Override
			protected Set<ModelStatement> set(ModelNode<Resource> node) {
				return node.contexts;
			}
		};
	}

	public Set<Value> objects(final Resource subj, final URI pred, final Resource... contexts) {
		return new ValueSet<Value>() {

			@Override
			public boolean contains(Object o) {
				if (o instanceof Value) {
					return ModelImpl.this.contains(subj, pred, (Value)o, contexts);
				}
				return false;
			}

			@Override
			public boolean remove(Object o) {
				if (o instanceof Value)
					return ModelImpl.this.remove(subj, pred, (Value)o, contexts);
				return false;
			}

			@Override
			public boolean add(Value obj) {
				if (subj == null || pred == null)
					throw new UnsupportedOperationException("Incomplete statement");
				if (contains(obj))
					return false;
				return ModelImpl.this.add(subj, pred, obj, contexts);
			}

			@Override
			public void clear() {
				ModelImpl.this.remove(subj, pred, null, contexts);
			}

			@Override
			protected ModelIterator statementIterator() {
				return match(subj, pred, null, contexts);
			}

			@Override
			protected ModelNode<Value> node(ModelStatement st) {
				return st.obj;
			}

			@Override
			protected Set<ModelStatement> set(ModelNode<Value> node) {
				return node.objects;
			}
		};
	}

	public Set<URI> predicates(final Resource subj, final Value obj, final Resource... contexts) {
		return new ValueSet<URI>() {

			@Override
			public boolean contains(Object o) {
				if (o instanceof URI) {
					return ModelImpl.this.contains(subj, (URI)o, obj, contexts);
				}
				return false;
			}

			@Override
			public boolean remove(Object o) {
				if (o instanceof URI)
					return ModelImpl.this.remove(subj, (URI)o, obj, contexts);
				return false;
			}

			@Override
			public boolean add(URI pred) {
				if (subj == null || obj == null)
					throw new UnsupportedOperationException("Incomplete statement");
				if (contains(pred))
					return false;
				return ModelImpl.this.add(subj, pred, obj, contexts);
			}

			@Override
			public void clear() {
				ModelImpl.this.remove(subj, null, obj, contexts);
			}

			@Override
			protected ModelIterator statementIterator() {
				return match(subj, null, obj, contexts);
			}

			@Override
			protected ModelNode<URI> node(ModelStatement st) {
				return st.pred;
			}

			@Override
			protected Set<ModelStatement> set(ModelNode<URI> node) {
				return node.predicates;
			}
		};
	}

	public Set<Resource> subjects(final URI pred, final Value obj, final Resource... contexts) {
		return new ValueSet<Resource>() {

			@Override
			public boolean contains(Object o) {
				if (o instanceof Resource) {
					return ModelImpl.this.contains((Resource)o, pred, obj, contexts);
				}
				return false;
			}

			@Override
			public boolean remove(Object o) {
				if (o instanceof Resource)
					return ModelImpl.this.remove((Resource)o, pred, obj, contexts);
				return false;
			}

			@Override
			public boolean add(Resource subj) {
				if (pred == null || obj == null)
					throw new UnsupportedOperationException("Incomplete statement");
				if (contains(subj))
					return false;
				return ModelImpl.this.add(subj, pred, obj, contexts);
			}

			@Override
			public void clear() {
				ModelImpl.this.remove(null, pred, obj, contexts);
			}

			@Override
			protected ModelIterator statementIterator() {
				return match(null, pred, obj, contexts);
			}

			@Override
			protected ModelNode<Resource> node(ModelStatement st) {
				return st.subj;
			}

			@Override
			protected Set<ModelStatement> set(ModelNode<Resource> node) {
				return node.subjects;
			}
		};
	}

	ModelIterator match(Resource subj, URI pred, Value obj, Resource... contexts) {
		assert contexts != null;
		Set<ModelStatement> s = null;
		Set<ModelStatement> p = null;
		Set<ModelStatement> o = null;
		if (subj != null) {
			s = asGraphNode(subj).subjects;
		}
		if (pred != null) {
			p = asGraphNode(pred).predicates;
		}
		if (obj != null) {
			o = asGraphNode(obj).objects;
		}
		Set<ModelStatement> set;
		if (contexts != null && contexts.length == 1) {
			Set<ModelStatement> c = asGraphNode(contexts[0]).contexts;
			set = smallest(statements, s, p, o, c);
		}
		else {
			set = smallest(statements, s, p, o);
		}
		Iterator<ModelStatement> it = set.iterator();
		Iterator<ModelStatement> iter;
		iter = new PatternIterator(it, subj, pred, obj, contexts);
		return new ModelIterator(iter, set);
	}

	boolean matches(Statement st, Resource subj, URI pred, Value obj, Resource... contexts) {
		if (subj != null && !subj.equals(st.getSubject())) {
			return false;
		}
		if (pred != null && !pred.equals(st.getPredicate())) {
			return false;
		}
		if (obj != null && !obj.equals(st.getObject())) {
			return false;
		}

		if (contexts == null || contexts.length == 0) {
			// Any context matches
			return true;
		}
		else {
			// Accept if one of the contexts from the pattern matches
			Resource stContext = st.getContext();

			for (Resource context : contexts) {
				if (context == null && stContext == null) {
					return true;
				}
				if (context != null && context.equals(stContext)) {
					return true;
				}
			}

			return false;
		}
	}

	abstract class FilteredSet extends AbstractSet<Statement> {

		@Override
		public Iterator<Statement> iterator() {
			final ModelIterator iter = statementIterator();
			return new Iterator<Statement>() {

				private ModelStatement current;

				private ModelStatement next;

				public boolean hasNext() {
					if (next == null && iter.hasNext()) {
						next = iter.next();
					}
					return next != null;
				}

				public ModelStatement next() {
					if (next == null) {
						next = iter.next();
					}
					current = next;
					next = null;
					return current;
				}

				public void remove() {
					iter.remove();
				}
			};
		}

		@Override
		public int size() {
			int size = 0;
			Iterator<ModelStatement> iter = statementIterator();
			while (iter.hasNext()) {
				size++;
				iter.next();
			}
			return size;
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof Statement) {
				Statement st = (Statement)o;
				if (accept(st))
					return ModelImpl.this.contains(o);
			}
			return false;
		}

		@Override
		public boolean add(Statement st) {
			if (accept(st))
				return ModelImpl.this.add(st);
			throw new IllegalArgumentException("Statement does not match filter: " + st);
		}

		protected abstract boolean accept(Statement st);

		protected abstract ModelIterator statementIterator();
	}

	abstract class ValueSet<V extends Value> extends AbstractSet<V> {

		@Override
		public Iterator<V> iterator() {
			final Set<V> set = new HashSet<V>();
			final ModelIterator iter = statementIterator();
			return new Iterator<V>() {

				private ModelStatement current;

				private ModelStatement next;

				public boolean hasNext() {
					if (next == null) {
						next = findNext();
					}
					return next != null;
				}

				public V next() {
					if (next == null) {
						next = findNext();
						if (next == null)
							throw new NoSuchElementException();
					}
					current = next;
					next = null;
					V value = convert(current);
					set.add(value);
					return value;
				}

				public void remove() {
					if (current == null)
						throw new IllegalStateException();
					removeAll(set(node(current)), iter.getOwner());
					current = null;
				}

				private ModelStatement findNext() {
					while (iter.hasNext()) {
						ModelStatement st = iter.next();
						if (accept(st))
							return st;
					}
					return null;
				}

				private boolean accept(ModelStatement st) {
					return !set.contains(convert(st));
				}

				private V convert(ModelStatement st) {
					return node(st).getValue();
				}
			};
		}

		@Override
		public int size() {
			Set<V> set = new HashSet<V>();
			Iterator<ModelStatement> iter = statementIterator();
			while (iter.hasNext()) {
				set.add(node(iter.next()).getValue());
			}
			return set.size();
		}

		@Override
		public boolean remove(Object o) {
			if (values.containsKey(o)) {
				return removeAll(set(values.get(o)), null);
			}
			return false;
		}

		protected abstract ModelIterator statementIterator();

		protected abstract ModelNode<V> node(ModelStatement st);

		protected abstract Set<ModelStatement> set(ModelNode<V> node);

		boolean removeAll(Set<ModelStatement> remove, Set<ModelStatement> owner) {
			if (remove.isEmpty())
				return false;
			for (ModelStatement st : remove) {
				ModelNode<Resource> subj = st.subj;
				Set<ModelStatement> subjects = subj.subjects;
				if (subjects == owner) {
					subj.subjects = new HashSet<ModelStatement>(owner);
					subj.subjects.removeAll(remove);
				}
				else if (subjects != remove) {
					subjects.remove(st);
				}
				ModelNode<URI> pred = st.pred;
				Set<ModelStatement> predicates = pred.predicates;
				if (predicates == owner) {
					pred.predicates = new HashSet<ModelStatement>(owner);
					pred.predicates.removeAll(remove);
				}
				else if (predicates != remove) {
					predicates.remove(st);
				}
				ModelNode<Value> obj = st.obj;
				Set<ModelStatement> objects = obj.objects;
				if (objects == owner) {
					obj.objects = new HashSet<ModelStatement>(owner);
					obj.objects.removeAll(remove);
				}
				else if (objects != remove) {
					objects.remove(st);
				}
				ModelNode<Resource> ctx = st.ctx;
				Set<ModelStatement> contexts = ctx.contexts;
				if (contexts == owner) {
					ctx.contexts = new HashSet<ModelStatement>(owner);
					ctx.contexts.removeAll(remove);
				}
				else if (contexts != remove) {
					contexts.remove(st);
				}
				if (statements == owner) {
					statements = new HashSet<ModelStatement>(statements);
					statements.removeAll(remove);
				}
				else if (statements != remove && statements != owner) {
					statements.remove(st);
				}
			}
			remove.clear();
			return true;
		}
	}

	class ModelIterator implements Iterator<ModelStatement> {

		private Iterator<ModelStatement> iter;

		private Set<ModelStatement> owner;

		private ModelStatement last;

		public ModelIterator(Iterator<ModelStatement> iter, Set<ModelStatement> owner) {
			this.iter = iter;
			this.owner = owner;
		}

		public Set<ModelStatement> getOwner() {
			return owner;
		}

		public boolean hasNext() {
			return iter.hasNext();
		}

		public ModelStatement next() {
			return last = iter.next();
		}

		public void remove() {
			if (last == null)
				throw new IllegalStateException();
			removeIfNotOwner(statements);
			removeIfNotOwner(last.subj.subjects);
			removeIfNotOwner(last.pred.predicates);
			removeIfNotOwner(last.obj.objects);
			removeIfNotOwner(last.ctx.contexts);
			iter.remove(); // remove from owner
		}

		private void removeIfNotOwner(Set<ModelStatement> subjects) {
			if (subjects != owner) {
				subjects.remove(last);
			}
		}
	}

	class ModelNode<V extends Value> implements Serializable {

		private static final long serialVersionUID = -1205676084606998540L;

		Set<ModelStatement> subjects = new HashSet<ModelStatement>();

		Set<ModelStatement> predicates = new HashSet<ModelStatement>();

		Set<ModelStatement> objects = new HashSet<ModelStatement>();

		Set<ModelStatement> contexts = new HashSet<ModelStatement>();

		private V value;

		public ModelNode(V value) {
			this.value = value;
		}

		public V getValue() {
			return value;
		}

		public boolean isNull() {
			return value == null;
		}
	}

	class ModelStatement extends StatementBase {

		private static final long serialVersionUID = 2200404772364346279L;

		ModelNode<Resource> subj;

		ModelNode<URI> pred;

		ModelNode<Value> obj;

		ModelNode<Resource> ctx;

		public ModelStatement(ModelNode<Resource> subj, ModelNode<URI> pred, ModelNode<Value> obj,
				ModelNode<Resource> ctx)
		{
			assert subj != null;
			assert pred != null;
			assert obj != null;
			assert ctx != null;
			this.subj = subj;
			this.pred = pred;
			this.obj = obj;
			this.ctx = ctx;
		}

		public Resource getSubject() {
			return subj.getValue();
		}

		public URI getPredicate() {
			return pred.getValue();
		}

		public Value getObject() {
			return obj.getValue();
		}

		public Resource getContext() {
			return ctx.getValue();
		}
	}

	class PatternIterator<S extends Statement> extends FilterIterator<S> {

		private Resource subj;

		private URI pred;

		private Value obj;

		private Resource[] contexts;

		public PatternIterator(Iterator<S> iter, Resource subj, URI pred, Value obj, Resource... contexts) {
			super(iter);
			this.subj = subj;
			this.pred = pred;
			this.obj = obj;
			this.contexts = contexts;
		}

		@Override
		protected boolean accept(S st) {
			return matches(st, subj, pred, obj, contexts);
		}
	}

	private void writeObject(ObjectOutputStream s)
		throws IOException
	{
		// Write out any hidden serialization magic
		s.defaultWriteObject();
		// Write in size
		s.writeInt(statements.size());
		// Write in all elements
		for (ModelStatement st : statements) {
			Resource subj = st.getSubject();
			URI pred = st.getPredicate();
			Value obj = st.getObject();
			Resource ctx = st.getContext();
			s.writeObject(new StatementImpl(subj, pred, obj, ctx));
		}
	}

	private void readObject(ObjectInputStream s)
		throws IOException, ClassNotFoundException
	{
		// Read in any hidden serialization magic
		s.defaultReadObject();
		// Read in size
		int size = s.readInt();
		values = new HashMap<Value, ModelNode>(size * 2);
		statements = new HashSet<ModelStatement>(size);
		// Read in all elements
		for (int i = 0; i < size; i++) {
			Statement st = (Statement)s.readObject();
			add(st);
		}
	}

	private Iterator find(Statement st) {
		Resource subj = st.getSubject();
		URI pred = st.getPredicate();
		Value obj = st.getObject();
		Resource ctx = st.getContext();
		return match(subj, pred, obj, ctx);
	}

	private boolean addModelStatement(ModelStatement st) {
		Set<ModelStatement> subj = st.subj.subjects;
		Set<ModelStatement> pred = st.pred.predicates;
		Set<ModelStatement> obj = st.obj.objects;
		Set<ModelStatement> ctx = st.ctx.contexts;
		if (smallest(subj, pred, obj, ctx).contains(st))
			return false;
		statements.add(st);
		subj.add(st);
		pred.add(st);
		obj.add(st);
		ctx.add(st);
		return true;
	}

	private Set<ModelStatement> smallest(Set<ModelStatement>... sets) {
		int minSize = Integer.MAX_VALUE;
		Set<ModelStatement> minSet = null;
		for (Set<ModelStatement> set : sets) {
			if (set != null && set.size() < minSize) {
				minSet = set;
			}
		}
		return minSet;
	}

	private <V extends Value> ModelNode<V> asGraphNode(V value) {
		if (values.containsKey(value))
			return values.get(value);
		ModelNode<V> node = new ModelNode<V>(value);
		values.put(value, node);
		return node;
	}
}