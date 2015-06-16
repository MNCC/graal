/* Graal v0.7.4
 * Copyright (c) 2014-2015 Inria Sophia Antipolis - Méditerranée / LIRMM (Université de Montpellier & CNRS)
 * All rights reserved.
 * This file is part of Graal <https://graphik-team.github.io/graal/>.
 *
 * Author(s): Clément SIPIETER
 *            Mélanie KÖNIG
 *            Swan ROCHER
 *            Jean-François BAGET
 *            Michel LECLÈRE
 *            Marie-Laure MUGNIER
 */
 /**
 * 
 */
package fr.lirmm.graphik.graal.grd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

import fr.lirmm.graphik.graal.core.LabelRuleComparator;
import fr.lirmm.graphik.graal.core.Rule;
import fr.lirmm.graphik.graal.core.Substitution;
import fr.lirmm.graphik.graal.core.TreeMapSubstitution;
import fr.lirmm.graphik.graal.core.Unifier;
import fr.lirmm.graphik.util.LinkedSet;
import fr.lirmm.graphik.util.graph.scc.StronglyConnectedComponentsGraph;
import fr.lirmm.graphik.util.stream.filter.Filter;

/**
 * The graph of rule dependencies (GRD) is a directed graph built from a rule
 * set as follows: there is a vertex for each rule in the set and there is an
 * edge from a rule R1 to a rule R2 if R1 may lead to trigger R2, i.e., R2
 * depends on R1. R2 depends on R1 if and only if there is piece-unifier (for
 * this notion, see f.i. this paper (TODO)) between the body of R2 and the head
 * of R1.
 * 
 * @author Clément Sipieter (INRIA) {@literal <clement@6pi.fr>} <br/>
 *         Swan Rocher {@literal <swan.rocher@lirmm.fr>}
 * 
 */
public class GraphOfRuleDependencies {

	public static abstract class DependencyChecker implements Filter<Substitution> {
		@Override
		public final boolean filter(Substitution s) {
			return isValidDependency(this.rule1,this.rule2,s);
		}

		protected abstract boolean isValidDependency(Rule r1, Rule r2, Substitution s);

		public final void setRule1(Rule r1) { this.rule1 = r1; }
		public final void setRule2(Rule r2) { this.rule2 = r2; }

		private Rule rule1;
		private Rule rule2;

		public static final DependencyChecker DEFAULT = new DependencyChecker() {
			@Override
			protected boolean isValidDependency(Rule r1, Rule r2, Substitution s) { return true; }
		};
	};

	private DirectedGraph<Rule, Integer> graph;
	private ArrayList<Set<Substitution>> edgesValue;
	private boolean computingUnifiers;

	// /////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	// /////////////////////////////////////////////////////////////////////////

	public GraphOfRuleDependencies(Iterable<Rule> rules, boolean withUnifiers, DependencyChecker checker) {
		this.graph = new DefaultDirectedGraph<Rule, Integer>(Integer.class);
		this.edgesValue = new ArrayList<Set<Substitution>>();
		this.computingUnifiers = withUnifiers;

		for (Rule r : rules) {
			this.addRule(r);
		}
		for (Rule r1 : rules) {
			for (Rule r2 : rules) {
				computeDependency(r1,r2,checker);
			}
		}
	}

	public GraphOfRuleDependencies(Iterable<Rule> rules, boolean wu) {
		this(rules,wu,DependencyChecker.DEFAULT);
	}

	public GraphOfRuleDependencies(Iterable<Rule> rules, DependencyChecker checker) {
		this(rules,false,checker);
	}

	public GraphOfRuleDependencies(Iterable<Rule> rules) {
		this(rules,false);
	}

	protected GraphOfRuleDependencies(boolean wu) {
		this.graph = new DefaultDirectedGraph<Rule, Integer>(Integer.class);
		this.edgesValue = new ArrayList<Set<Substitution>>();
		this.computingUnifiers = wu;
	}

	protected GraphOfRuleDependencies() {
		this.graph = new DefaultDirectedGraph<Rule, Integer>(Integer.class);
		this.edgesValue = new ArrayList<Set<Substitution>>();
		this.computingUnifiers = false;
	}




	// /////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	// /////////////////////////////////////////////////////////////////////////

	// TODO
	// add some unit test for this method...
	public boolean hasCircuit() {
		DepthFirstIterator<Rule,Integer> it = new DepthFirstIterator(this.graph);
		Set<Rule> color = new TreeSet<Rule>();
		Rule r = null;
		while (it.hasNext()) {
			r = it.next();
			if (color.contains(r))
				return true;
			color.add(r);
		}
		return false;
	}

	public Set<Substitution> getUnifiers(Integer e) {
		return Collections.unmodifiableSet(this.edgesValue.get(e));
	}

	/**
	 * @param ruleset
	 * @return
	 */
	// TODO refactoring: return type has changed!
	// (from GRDWithUnifiers to GRD)
	public GraphOfRuleDependencies getSubGraph(Iterable<Rule> ruleSet) {
		GraphOfRuleDependencies subGRD = new GraphOfRuleDependencies();
		subGRD.addRuleSet(ruleSet);
		for (Rule src : ruleSet) {
			for (Rule target : ruleSet) {
				Integer e = this.graph.getEdge(src, target);
				if (e != null) { // there is an edge
					for (Substitution s : this.edgesValue.get(e)) {
						subGRD.addDependency(src, s, target);
					}
				}
			}
		}
		return subGRD;
	}

	// /////////////////////////////////////////////////////////////////////////
	// OVERRIDE METHODS

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		TreeSet<Rule> rules = new TreeSet<Rule>(new LabelRuleComparator());
		for (Rule r : this.graph.vertexSet()) {
			rules.add(r);
		}
		for (Rule src : rules) {
			for (Integer e : this.graph.outgoingEdgesOf(src)) {
				Rule dest = this.graph.getEdgeTarget(e);

				s.append(src.getLabel());
				s.append("--");
				for (Substitution sub : this.edgesValue.get(this.graph.getEdge(
						src, dest))) {
					s.append(sub);
				}
				s.append("-->");
				s.append(dest.getLabel());
				s.append('\n');
			}

		}
		return s.toString();
	}

	// /////////////////////////////////////////////////////////////////////////
	// PROTECTED METHODS
	// /////////////////////////////////////////////////////////////////////////

	protected void addDependency(Rule src, Substitution sub, Rule dest) {
		Integer edgeIndex = this.graph.getEdge(src, dest);
		Set<Substitution> edge = null;
		if (edgeIndex != null) {
			edge = this.edgesValue.get(edgeIndex);
		} else {
			edgeIndex = this.edgesValue.size();
			edge = new LinkedSet<Substitution>();
			this.edgesValue.add(edgeIndex, edge);
			this.graph.addEdge(src, dest, edgeIndex);
		}
		edge.add(sub);
	}

	protected void addDependency(Rule src, Rule dest) {
		addDependency(src,new TreeMapSubstitution(),dest);
	}

	protected void setDependency(Rule src, Set<Substitution> subs, Rule dest) {
		Integer edgeIndex = this.graph.getEdge(src, dest);
		if (edgeIndex == null) {
			edgeIndex = this.edgesValue.size();
			this.edgesValue.add(edgeIndex, subs);
			this.graph.addEdge(src, dest, edgeIndex);
		} else {
			this.edgesValue.set(edgeIndex, subs);
		}
	}


	// /////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	// /////////////////////////////////////////////////////////////////////////

	public Iterable<Rule> getRules() {
		return this.graph.vertexSet();
	}

	public Iterable<Integer> getOutgoingEdgesOf(Rule src) {
		return this.graph.outgoingEdgesOf(src);
	}

	public Rule getEdgeTarget(Integer i) {
		return this.graph.getEdgeTarget(i);
	}

	public boolean existUnifier(Rule src, Rule dest) {
		Integer index = this.graph.getEdge(src, dest);
		if (index != null) {
			return true;
		}
		return false;
	}

	public StronglyConnectedComponentsGraph<Rule> getStronglyConnectedComponentsGraph() {
		return new<Integer> StronglyConnectedComponentsGraph<Rule>(this.graph);
	}


	// /////////////////////////////////////////////////////////////////////////
	// OVERRIDE METHODS

//	@Override
//	public String toString() {
//		StringBuilder s = new StringBuilder();
//		for (Rule src : this.graph.vertexSet()) {
//			for (Integer e : this.graph.outgoingEdgesOf(src)) {
//
//				s.append(this.graph.getEdgeSource(e).getLabel());
//				s.append("-->");
//				s.append(this.graph.getEdgeTarget(e).getLabel());
//				s.append('\n');
//			}
//
//		}
//		return s.toString();
//	}

	// /////////////////////////////////////////////////////////////////////////
	// PROTECTED METHODS
	// /////////////////////////////////////////////////////////////////////////

	protected void computeDependency(Rule r1, Rule r2, DependencyChecker checker) {
		checker.setRule1(r1);
		checker.setRule2(r2);
		if (this.computingUnifiers) {
			Set<Substitution> unifiers = Unifier.instance().computePieceUnifier(r1,r2.getBody(),checker);
			if (!unifiers.isEmpty()) {
				this.setDependency(r1, unifiers, r2);
			}
		}
		else {
			if(Unifier.instance().existPieceUnifier(r1,r2.getBody(),checker)) {
				this.addDependency(r1, r2);
			}
		}
	}


	private static final String PREFIX = "R" + new Date().hashCode() + "-";
	private static int ruleIndex = -1;
	protected void addRule(Rule r) {
		if(r.getLabel().isEmpty()) {
			r.setLabel(PREFIX + ++ruleIndex);
		}
		this.graph.addVertex(r);
	}

	protected void addRuleSet(Iterable<Rule> ruleSet) {
		for (Rule r : ruleSet) {
			this.addRule(r);
		}
	}

}
