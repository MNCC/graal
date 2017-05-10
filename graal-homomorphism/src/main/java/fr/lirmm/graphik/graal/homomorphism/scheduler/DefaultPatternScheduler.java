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
package fr.lirmm.graphik.graal.homomorphism.scheduler;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.InMemoryAtomSet;
import fr.lirmm.graphik.graal.api.core.RulesCompilation;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.core.Variable;
import fr.lirmm.graphik.graal.api.homomorphism.HomomorphismException;
import fr.lirmm.graphik.graal.homomorphism.Var;
import fr.lirmm.graphik.util.profiler.AbstractProfilable;

/**
 * Compute an order over variables from h. This scheduler put answer
 * variables first, then other variables are put in the order from
 * h.getTerms(Term.Type.VARIABLE).iterator().
 *
 * @author Clément Sipieter (INRIA) {@literal <clement@6pi.fr>}
 *
 */
public class DefaultPatternScheduler extends AbstractProfilable  implements PatternScheduler {

	private static DefaultPatternScheduler instance;

	private DefaultPatternScheduler() {
		super();
	}

	public static synchronized DefaultPatternScheduler instance() {
		if (instance == null)
			instance = new DefaultPatternScheduler();

		return instance;
	}

	public Var[] execute(InMemoryAtomSet h, Set<Variable> preAffectedVars, List<Term> ans, AtomSet data, RulesCompilation rc) {
		Set<Variable> variables = h.getVariables();
		variables.removeAll(preAffectedVars);
		
		Var[] vars = new Var[variables.size() + 2];

		int level = 0;
		vars[level] = new Var(level);

		int lastAnswerVariable = -1;
		Set<Variable> alreadyAffected = new HashSet<Variable>();
		for (Variable t : variables) {
			if (t.isVariable() && alreadyAffected.add(t)) {
				++level;
				vars[level] = new Var(level);
				vars[level].value = (Variable) t;
				
				if (ans.contains(t)) {
					if (level > lastAnswerVariable)
						lastAnswerVariable = level;
				}
			}
		}

		++level;
		vars[level] = new Var(level);
		vars[level].previousLevel = lastAnswerVariable;
		
		return vars;
	}

	@Override
	public Var[] execute(InMemoryAtomSet h, List<Term> ans, AtomSet data, RulesCompilation rc)
			throws HomomorphismException {
		return this.execute(h, Collections.<Variable>emptySet(), ans, data, rc);
	}

	@Override
	public boolean isAllowed(Var var, Term image) {
		return true;
	}

}
