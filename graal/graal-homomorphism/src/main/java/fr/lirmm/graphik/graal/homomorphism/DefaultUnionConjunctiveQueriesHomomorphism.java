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
package fr.lirmm.graphik.graal.homomorphism;

import fr.lirmm.graphik.graal.core.UnionConjunctiveQueries;
import fr.lirmm.graphik.graal.core.atomset.AtomSet;
import fr.lirmm.graphik.graal.core.stream.SubstitutionReader;

/**
 * @author Clément Sipieter (INRIA) <clement@6pi.fr>
 *
 */
public final class DefaultUnionConjunctiveQueriesHomomorphism implements UnionConjunctiveQueriesHomomorphism<AtomSet> {

	private static DefaultUnionConjunctiveQueriesHomomorphism instance;
    
	/**
     * @param queries
     * @param atomSet
     */
    private DefaultUnionConjunctiveQueriesHomomorphism() {
    }
    
    public static synchronized DefaultUnionConjunctiveQueriesHomomorphism getInstance() {
    	if(instance == null)
    		instance = new DefaultUnionConjunctiveQueriesHomomorphism();
    	
    	return instance;
    }

	@Override
	public SubstitutionReader execute(UnionConjunctiveQueries queries,
			AtomSet atomset) throws HomomorphismException {
        return new UnionConjunctiveQueriesSubstitutionReader(queries, atomset);
	}

}
