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
 /**
 * 
 */
package fr.lirmm.graphik.graal.core;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.core.factory.DefaultRuleFactory;
import fr.lirmm.graphik.graal.core.term.DefaultTermFactory;

/**
 * @author Clément Sipieter (INRIA) {@literal <clement@6pi.fr>}
 * 
 */
public class RuleTest {

	private static Predicate predicate = new Predicate("pred", 2);

	private static Atom atom1, atom2, atom3;
	static {
		Term[] terms = new Term[2];
		terms[0] = DefaultTermFactory.instance().createVariable("X");
		terms[1] = DefaultTermFactory.instance().createConstant("a");
		atom1 = new DefaultAtom(predicate, Arrays.asList(terms));
		terms = new Term[2];
		terms[0] = DefaultTermFactory.instance().createVariable("X");
		terms[1] = DefaultTermFactory.instance().createVariable("Y");
		atom2 = new DefaultAtom(predicate, Arrays.asList(terms));
		terms = new Term[2];
		terms[0] = DefaultTermFactory.instance().createConstant("b");
		terms[1] = DefaultTermFactory.instance().createVariable("Y");
		atom3 = new DefaultAtom(predicate, Arrays.asList(terms));
	}

	@Test
	public void piecesTest1() {
		Rule rule = DefaultRuleFactory.instance().create();
		rule.getHead().add(atom1);
		rule.getHead().add(atom2);
		rule.getHead().add(atom3);
		Assert.assertEquals(1, Rules.getPieces(rule).size());
	}
	
	@Test
	public void piecesTest2() {
		Rule rule = DefaultRuleFactory.instance().create();
		rule.getBody().add(atom2);
		rule.getHead().add(atom1);
		rule.getHead().add(atom2);
		rule.getHead().add(atom3);
		Assert.assertEquals(3, Rules.getPieces(rule).size());
	}
	
	@Test
	public void piecesTest3() {
		Rule rule = DefaultRuleFactory.instance().create();
		rule.getHead().add(atom3);
		rule.getHead().add(atom3);
		rule.getHead().add(atom3);
		Assert.assertEquals(1, Rules.getPieces(rule).size());
	}
}
