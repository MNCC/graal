package fr.lirmm.graphik.graal.core;

import fr.lirmm.graphik.graal.core.term.Term;

public interface PredicateFunction {

	public boolean evaluate(Term... t);

};
