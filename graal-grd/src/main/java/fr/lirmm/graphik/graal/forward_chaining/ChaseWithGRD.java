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
package fr.lirmm.graphik.graal.forward_chaining;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.GraphOfRuleDependencies;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.RulesCompilation;
import fr.lirmm.graphik.graal.api.forward_chaining.AbstractChase;
import fr.lirmm.graphik.graal.api.forward_chaining.ChaseException;
import fr.lirmm.graphik.graal.api.forward_chaining.RuleApplier;
import fr.lirmm.graphik.graal.core.grd.DefaultGraphOfRuleDependencies;
import fr.lirmm.graphik.graal.forward_chaining.rule_applier.DefaultRuleApplierWithCompilation;
import fr.lirmm.graphik.graal.forward_chaining.rule_applier.RestrictedChaseRuleApplier;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import fr.lirmm.graphik.util.stream.CloseableIteratorAdapter;

/**
 * This chase (forward-chaining) algorithm use GRD to define the Rules that will
 * be triggered in the next step.
 * 
 * @author Clément Sipieter (INRIA) <clement@6pi.fr>
 *
 */
public class ChaseWithGRD<T extends AtomSet> extends AbstractChase<Rule, T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChaseWithGRD.class);

	private GraphOfRuleDependencies grd;
	private T atomSet;

	/**
	 * Queue of the rules to apply in the saturation process
	 */
	private Queue<Rule> queue = new LinkedList<Rule>();

	// /////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	// /////////////////////////////////////////////////////////////////////////

	public ChaseWithGRD(GraphOfRuleDependencies grd, T atomSet, RuleApplier<Rule, ? super T> ruleApplier) {
		super(ruleApplier);
		this.grd = grd;
		this.atomSet = atomSet;

		for (Rule r : grd.getRules()) {
			this.queue.add(r);
		}
	}

	public ChaseWithGRD(GraphOfRuleDependencies grd, T atomSet) {
		this(grd, atomSet, new RestrictedChaseRuleApplier<T>());
	}

	public ChaseWithGRD(Iterator<Rule> rules, T atomSet) {
		this(new DefaultGraphOfRuleDependencies(rules), atomSet);
	}

	public ChaseWithGRD(Iterable<Rule> rules, T atomSet, RulesCompilation compilation)
			throws ChaseException, IOException {
		this(new DefaultGraphOfRuleDependencies(rules, compilation), atomSet,
				new DefaultRuleApplierWithCompilation<T>(compilation));
	}

	public ChaseWithGRD(Iterator<Rule> rules, T atomSet, RulesCompilation compilation)
			throws ChaseException, IOException {
		this(new DefaultGraphOfRuleDependencies(rules, compilation), atomSet,
				new DefaultRuleApplierWithCompilation<T>(compilation));
	}

	// /////////////////////////////////////////////////////////////////////////
	// METHODS
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void next() throws ChaseException {
		Queue<Rule> newQueue = new LinkedList<Rule>();
		List<Atom> newAtomSet = new LinkedList<Atom>();

		try {

			while (!queue.isEmpty()) {
				Rule rule = queue.poll();

				if (rule == null)
					continue;

				CloseableIterator<Atom> it = this.getRuleApplier().delegatedApply(rule, this.atomSet);

				// Nothing more to do for this iteration
				if (!it.hasNext()) {
					it.close();
					continue;
				}

				// Add the generated atoms in the atom set
				while (it.hasNext()) {
					newAtomSet.add(it.next());
				}

				// Make the next queue to use
				for (Rule triggeredRule : this.grd.getTriggeredRules(rule)) {

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("-- -- Dependency: " + triggeredRule);
					}

					if (!newQueue.contains(triggeredRule)) {
						newQueue.add(triggeredRule);
					}
				}
			}
			queue = newQueue;
			atomSet.addAll(new CloseableIteratorAdapter<Atom>(newAtomSet.iterator()));
		} catch (Exception e) {
			throw new ChaseException("An error occur pending saturation step.", e);
		}
	}

	@Override
	public boolean hasNext() {
		return !queue.isEmpty();
	}
}
