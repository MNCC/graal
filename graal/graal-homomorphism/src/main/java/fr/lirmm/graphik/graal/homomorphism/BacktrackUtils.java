/*
 * Copyright (C) Inria Sophia Antipolis - Méditerranée / LIRMM
 * (Université de Montpellier & CNRS) (2014 - 2016)
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
package fr.lirmm.graphik.graal.homomorphism;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.AtomSetException;
import fr.lirmm.graphik.graal.api.core.RulesCompilation;
import fr.lirmm.graphik.graal.api.core.Substitution;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.core.Variable;
import fr.lirmm.graphik.graal.core.DefaultAtom;
import fr.lirmm.graphik.graal.core.TreeMapSubstitution;

/**
 * @author Clément Sipieter (INRIA) {@literal <clement@6pi.fr>}
 *
 */
public final class BacktrackUtils {

	// /////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	// /////////////////////////////////////////////////////////////////////////

	private BacktrackUtils() {
	}

	// /////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	// /////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 * @param atomsFrom
	 * @param atomsTo
	 * @param index
	 * @param rc
	 * @return
	 * @throws AtomSetException
	 */
	public static boolean isHomomorphism(Iterable<Atom> atomsFrom, AtomSet atomsTo, Map<Variable, Var> index,
	    RulesCompilation rc) throws AtomSetException {
		for (Atom atom : atomsFrom) {
			Atom image = BacktrackUtils.createImageOf(atom, index);
			boolean contains = false;

			for (Atom a : rc.getRewritingOf(image)) {
				if (atomsTo.contains(a)) {
					contains = true;
					break;
				}
			}

			if (!contains)
				return false;
		}
		return true;
	}

	/**
	 * 
	 * @param atom
	 * @param images
	 * @return
	 */
	public static Atom createImageOf(Atom atom, Map<Variable, Var> map) {
		List<Term> termsSubstitut = new LinkedList<Term>();
		for (Term term : atom.getTerms()) {
			if (term instanceof Variable) {
				termsSubstitut.add(imageOf((Variable) term, map));
			} else {
				termsSubstitut.add(term);
			}
		}

		return new DefaultAtom(atom.getPredicate(), termsSubstitut);
	}

	/**
	 * Return the image of the specified variable (extracted from map).
	 * 
	 * @param var
	 * @return
	 */
	public static Term imageOf(Variable var, Map<Variable, Var> map) {
		Term t = map.get(var).image;
		if (t == null) {
			return var;
		} else {
			return t;
		}
	}

	/**
	 * Extract image of variables from Var class in a Substitution.
	 * 
	 * @param vars
	 * @return
	 */
	public static Substitution createSubstitution(Iterator<Var> vars) {
		Substitution s = new TreeMapSubstitution();
		while (vars.hasNext()) {
			Var v = vars.next();
			if (v.image != null) {
				s.put(v.value, v.image);
			}
		}
		return s;
	}

}
