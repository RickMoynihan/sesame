/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.algebra.evaluation.cursors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import info.aduna.lang.ObjectUtil;

import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Cursor;
import org.openrdf.query.algebra.AggregateOperator;
import org.openrdf.query.algebra.Count;
import org.openrdf.query.algebra.Group;
import org.openrdf.query.algebra.GroupElem;
import org.openrdf.query.algebra.Max;
import org.openrdf.query.algebra.Min;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.evaluation.EvaluationStrategy;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;
import org.openrdf.query.impl.IteratorCursor;
import org.openrdf.store.StoreException;

/**
 * @author David Huynh
 * @author Arjohn Kampman
 */
public class GroupCursor extends IteratorCursor<BindingSet> {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public GroupCursor(EvaluationStrategy strategy, Group group, BindingSet parentBindings)
		throws StoreException
	{
		super(createIterator(strategy, group, parentBindings));

	}

	/*---------*
	 * Methods *
	 *---------*/

	private static Iterator<BindingSet> createIterator(EvaluationStrategy strategy, Group group, BindingSet parentBindings)
		throws StoreException
	{
		Collection<BindingSet> bindingSets;
		Collection<Entry> entries;
		boolean ordered = false;

		if (ordered) {
			bindingSets = new ArrayList<BindingSet>();
			entries = buildOrderedEntries(strategy, group, parentBindings);
		}
		else {
			bindingSets = new HashSet<BindingSet>();
			entries = buildUnorderedEntries(strategy, group, parentBindings);
		}

		for (Entry entry : entries) {
			QueryBindingSet sol = new QueryBindingSet(parentBindings);

			for (String name : group.getGroupBindingNames()) {
				Value value = entry.getPrototype().getValue(name);
				if (value != null) {
					// Potentially overwrites bindings from super
					sol.setBinding(name, value);
				}
			}

			for (GroupElem ge : group.getGroupElements()) {
				Value value = processAggregate(strategy, entry.getSolutions(), ge.getOperator());
				if (value != null) {
					// Potentially overwrites bindings from super
					sol.setBinding(ge.getName(), value);
				}
			}

			bindingSets.add(sol);
		}

		return bindingSets.iterator();
	}

	private static Collection<Entry> buildOrderedEntries(EvaluationStrategy strategy, Group group, BindingSet parentBindings)
		throws StoreException
	{
		Cursor<BindingSet> iter = strategy.evaluate(group.getArg(),
				parentBindings);

		try {
			List<Entry> orderedEntries = new ArrayList<Entry>();
			Map<Key, Entry> entries = new HashMap<Key, Entry>();

			BindingSet bindingSet;
			while ((bindingSet = iter.next()) != null) {
				Key key = new Key(group, bindingSet);
				Entry entry = entries.get(key);

				if (entry == null) {
					entry = new Entry(bindingSet);
					entries.put(key, entry);
					orderedEntries.add(entry);
				}

				entry.addSolution(bindingSet);
			}

			return orderedEntries;
		}
		finally {
			iter.close();
		}
	}

	private static Collection<Entry> buildUnorderedEntries(EvaluationStrategy strategy, Group group, BindingSet parentBindings)
		throws StoreException
	{
		Cursor<BindingSet> iter = strategy.evaluate(group.getArg(),
				parentBindings);

		try {
			Map<Key, Entry> entries = new HashMap<Key, Entry>();

			BindingSet sol;
			while ((sol = iter.next()) != null) {
				Key key = new Key(group, sol);
				Entry entry = entries.get(key);

				if (entry == null) {
					entry = new Entry(sol);
					entries.put(key, entry);
				}

				entry.addSolution(sol);
			}

			return entries.values();
		}
		finally {
			iter.close();
		}

	}

	private static Value processAggregate(EvaluationStrategy strategy, Set<BindingSet> bindingSets, AggregateOperator operator)
		throws StoreException
	{
		if (operator instanceof Count) {
			Count countOp = (Count)operator;

			ValueExpr arg = countOp.getArg();

			if (arg != null) {
				Set<Value> values = makeValueSet(strategy, arg, bindingSets);
				return new LiteralImpl(Integer.toString(values.size()), XMLSchema.INTEGER);
			}
			else {
				return new LiteralImpl(Integer.toString(bindingSets.size()), XMLSchema.INTEGER);
			}
		}
		else if (operator instanceof Min) {
			Min minOp = (Min)operator;

			Set<Value> values = makeValueSet(strategy, minOp.getArg(), bindingSets);

			// FIXME: handle case where 'values' is empty
			double min = Double.POSITIVE_INFINITY;
			for (Value v : values) {
				if (v instanceof Literal) {
					Literal l = (Literal)v;
					try {
						min = Math.min(min, Double.parseDouble(l.getLabel()));
					}
					catch (NumberFormatException e) {
						// ignore
					}
				}
			}

			return new LiteralImpl(Double.toString(min), XMLSchema.DOUBLE);
		}
		else if (operator instanceof Max) {
			Max maxOp = (Max)operator;

			Set<Value> values = makeValueSet(strategy, maxOp.getArg(), bindingSets);

			// FIXME: handle case where 'values' is empty
			double max = Double.NEGATIVE_INFINITY;
			for (Value v : values) {
				if (v instanceof Literal) {
					Literal l = (Literal)v;
					try {
						max = Math.max(max, Double.parseDouble(l.getLabel()));
					}
					catch (NumberFormatException e) {
						// ignore
					}
				}
			}

			return new LiteralImpl(Double.toString(max), XMLSchema.DOUBLE);
		}

		return null;
	}

	private static Set<Value> makeValueSet(EvaluationStrategy strategy, ValueExpr arg, Set<BindingSet> bindingSets)
		throws StoreException
	{
		Set<Value> valueSet = new HashSet<Value>();

		for (BindingSet s : bindingSets) {
			Value value = strategy.evaluate(arg, s);
			if (value != null) {
				valueSet.add(value);
			}
		}

		return valueSet;
	}

	/**
	 * A unique key for a set of existing bindings.
	 * 
	 * @author David Huynh
	 */
	protected static class Key {

		private Group group;

		private BindingSet bindingSet;

		private int hash;

		public Key(Group group, BindingSet bindingSet) {
			this.group = group;
			this.bindingSet = bindingSet;

			for (String name : group.getGroupBindingNames()) {
				Value value = bindingSet.getValue(name);
				if (value != null) {
					this.hash ^= value.hashCode();
				}
			}
		}

		@Override
		public int hashCode()
		{
			return hash;
		}

		@Override
		public boolean equals(Object other)
		{
			if (other instanceof Key && other.hashCode() == hash) {
				BindingSet otherSolution = ((Key)other).bindingSet;

				for (String name : group.getGroupBindingNames()) {
					Value v1 = bindingSet.getValue(name);
					Value v2 = otherSolution.getValue(name);

					if (!ObjectUtil.nullEquals(v1, v2)) {
						return false;
					}
				}

				return true;
			}

			return false;
		}
	}

	protected static class Entry {

		private BindingSet prototype;

		private Set<BindingSet> bindingSets;

		public Entry(BindingSet prototype) {
			this.prototype = prototype;
			this.bindingSets = new HashSet<BindingSet>();
		}

		public BindingSet getPrototype() {
			return prototype;
		}

		public void addSolution(BindingSet bindingSet) {
			bindingSets.add(bindingSet);
		}

		public Set<BindingSet> getSolutions() {
			return bindingSets;
		}
	}
}