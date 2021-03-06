/*
 * Copyright (C) Inria Sophia Antipolis - Méditerranée / LIRMM
 * (Université de Montpellier & CNRS) (2014 - 2017)
 *
 * Contributors :
 *
 * Clément SIPIETER <clement.sipieter@inria.fr>
 * Mélanie KÖNIG
 * Swan ROCHER
 * Jean-François BAGET
 * Michel LECLÈRE
 * Marie-Laure MUGNIER <mugnier@lirmm.fr>
 *
 *
 * This file is part of Graal <https://graphik-team.github.io/graal/>.
 *
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */
package fr.lirmm.graphik.graal.core.unifier;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.Substitution;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.core.Variable;
import fr.lirmm.graphik.graal.api.core.unifier.DependencyChecker;
import fr.lirmm.graphik.graal.core.factory.DefaultSubstitutionFactory;
import fr.lirmm.graphik.util.LinkedSet;
import fr.lirmm.graphik.util.stream.AbstractCloseableIterator;
import fr.lirmm.graphik.util.stream.CloseableIteratorWithoutException;

/**
 * @author Clément Sipieter (INRIA) {@literal <clement@6pi.fr>}
 *
 */
class UnifierIterator extends AbstractCloseableIterator<Substitution> implements CloseableIteratorWithoutException<Substitution> {

	Queue<Substitution> unifiers = null;

	private Rule source, target;
	private DependencyChecker checkers[];
	private long maxSubstitution;

	// /////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	// /////////////////////////////////////////////////////////////////////////

	public UnifierIterator(Rule source, Rule target, DependencyChecker... checkers) {
		this(source, target, -1, checkers);
	}

	public UnifierIterator(Rule source, Rule target, boolean checkExists, DependencyChecker... checkers) {
		this(source, target, checkExists ? 1 : -1, checkers);
	}

	public UnifierIterator(Rule source, Rule target, long maxSubstitutions, DependencyChecker... checkers) {
		this.source = source;
		this.target = target;
		this.checkers = checkers;
		this.maxSubstitution = maxSubstitutions;
	}
	// /////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void close() {
	}

	@Override
	public boolean hasNext() {

		if (unifiers == null) {
			unifiers = new LinkedList<Substitution>(computePieceUnifiers(source, target, maxSubstitution));
		}
		return !unifiers.isEmpty();
	}

	@Override
	public Substitution next() {
		hasNext();
		return unifiers.poll();
	}

	// /////////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	// /////////////////////////////////////////////////////////////////////////

	public Set<Substitution> computePieceUnifiers(Rule source, Rule target) {
		return computePieceUnifiers(source, target, -1);
	}

	public Set<Substitution> computePieceUnifiers(Rule source, Rule target, boolean exists) {
		return computePieceUnifiers(source, target, exists ? 1 : -1);
	}

	public Set<Substitution> computePieceUnifiers(Rule source, Rule target, long maxSubstitutions) {
		Set<Substitution> unifiers = new LinkedSet<Substitution>();
		Queue<Atom> atomQueue = new LinkedList<Atom>();
		CloseableIteratorWithoutException<Atom> it = target.getBody().iterator();

		while (it.hasNext()) {
			Atom a = it.next();
			atomQueue.add(a);
		}

		/**
		 * Number of element added on the current iteration
		 */
		long nbAdd = 0;
		it = target.getBody().iterator();

		while (it.hasNext()) {
			Atom a = it.next();
			Queue<Atom> tmp = new LinkedList<Atom>(atomQueue);
			Collection<Substitution> substitutions = extendUnifier(source, tmp, a, new Unifier(), maxSubstitutions);
			nbAdd = substitutions.size();
			unifiers.addAll(substitutions);

			// nbAdd check
			if (nbAdd > 0 && maxSubstitutions > 0) {
				maxSubstitutions -= nbAdd;

				if (maxSubstitutions < 0)
					maxSubstitutions = 0;

				if (maxSubstitutions == 0)
					break;
			}
		}
		return unifiers;
	}

	/**
	 * 
	 * @param rule
	 * @param atomset
	 * @param pieceElement
	 * @param unifier
	 * @param maxSubstitutions The maximum number of elements to be in the return
	 *                         set. If a negative value is provided, all the
	 *                         elements will be computed.
	 * @return
	 */
	private Collection<Substitution> extendUnifier(Rule rule, Queue<Atom> atomset, Atom pieceElement, Unifier unifier, long maxSubstitutions) {
		atomset.remove(pieceElement);
		unifier.queryPiece.add(pieceElement);

		if (maxSubstitutions == 0)
			return Collections.emptySet();

		Collection<Substitution> unifierCollection = new LinkedList<Substitution>();
		Set<Variable> frontierVars = rule.getFrontier();
		Set<Variable> existentialVars = rule.getExistentials();

		CloseableIteratorWithoutException<Atom> it = rule.getHead().iterator();

		/**
		 * Number of element added in the current iteration
		 */
		long nbAdd = 0;
		final boolean notCheckMax = maxSubstitutions < 0;

		while (it.hasNext()) {
			Atom atom = it.next();
			Substitution u = unifier(unifier.s, pieceElement, atom, frontierVars, existentialVars);

			if (u == null)
				continue;

			unifier = new Unifier(unifier);
			unifier.ruleHeadPiece.add(atom);
			unifier.s = u;

			// look if there exist other element for the current piece
			Atom nextPieceElement = getNextPieceElementIfExist(u, atomset, existentialVars);

			if (nextPieceElement == null) {

				if (RuleDependencyUtils.validateUnifier(source, target, unifier.s, checkers)) {
					unifierCollection.add(unifier.s);
					nbAdd = 1;
				}
				// Nothing append; so we can pass the nbAdd check at the end of the loop
				else {
					// Think to uncomment in an evolution of code without 'continue'
					// nbAdd = 0;
					continue;
				}

			} else {
				Collection<Substitution> substitutions = extendUnifier(rule, new LinkedList<Atom>(atomset), nextPieceElement, unifier, maxSubstitutions);
				nbAdd = substitutions.size();

				if (nbAdd == 0)
					continue;

				unifierCollection.addAll(substitutions);
			}

			/*
			 * --> nbAdd check
			 * 
			 * At this point nbAdd MUST have a correct value according to the current
			 * iteration. Be careful for the future evolutions of the source code.
			 */
			/*
			 * In the following condition: "nbAdd == 0" is not necessary because the only
			 * case where nbAdd == 0 is switched before this point
			 */
			if (notCheckMax/* || nbAdd == 0 */)
				continue;

			maxSubstitutions -= nbAdd;

			if (maxSubstitutions <= 0)
				return unifierCollection;

			// <-- End of nbAdd check
		}
		return unifierCollection;
	}

	private static Atom getNextPieceElementIfExist(Substitution u, Queue<Atom> atomset, Set<Variable> glueVars) {
		Iterator<Atom> it = atomset.iterator();
		while (it.hasNext()) {
			Atom a = it.next();

			for (Term t1 : a) {

				for (Term t2 : glueVars) {

					if (u.createImageOf(t2).equals(u.createImageOf(t1)))
						return a;
				}
			}
		}
		return null;
	}

	private static Substitution unifier(Substitution baseUnifier, Atom a1, Atom a2, Set<Variable> frontierVars, Set<Variable> existentialVars) {

		if (!a1.getPredicate().equals(a2.getPredicate()))
			return null;

		boolean error = false;
		Substitution u = DefaultSubstitutionFactory.instance().createSubstitution();
		u.put(baseUnifier);

		for (int i = 0; i < a1.getPredicate().getArity(); ++i) {
			Term t1 = a1.getTerm(i);
			Term t2 = a2.getTerm(i);
			error = error || !compose(u, frontierVars, existentialVars, t1, t2);
		}

		if (error)
			return null;

		return u;
	}

	private static boolean compose(Substitution u, Set<Variable> frontierVars, Set<Variable> existentials, Term term, Term substitut) {
		Term termSubstitut = u.createImageOf(term);
		Term substitutSubstitut = u.createImageOf(substitut);

		if (!termSubstitut.equals(substitutSubstitut)) {

			if (termSubstitut.isConstant() || existentials.contains(termSubstitut)) {
				Term tmp = termSubstitut;
				termSubstitut = substitutSubstitut;
				substitutSubstitut = tmp;
			}

			for (Term t : u.getTerms()) {

				if (termSubstitut.equals(u.createImageOf(t))) {

					if (!put(u, frontierVars, existentials, t, substitutSubstitut))
						return false;
				}
			}

			if (!put(u, frontierVars, existentials, termSubstitut, substitutSubstitut))
				return false;
		}
		return true;
	}

	private static boolean put(Substitution u, Set<Variable> frontierVars, Set<Variable> existentials, Term term, Term substitut) {

		if (!term.equals(substitut)) {

			// two (constant | existentials vars)
			if (term.isConstant() || existentials.contains(term)) {
				return false;
			}
			// fr -> existential vars
			else if (frontierVars.contains(term) && existentials.contains(substitut)) {
				return false;
			}
			u.put((Variable) term, substitut);
		}
		return true;
	}
}
