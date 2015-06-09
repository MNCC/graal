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
 package fr.lirmm.graphik.graal.core;

import fr.lirmm.graphik.graal.core.term.DefaultTermFactory;
import fr.lirmm.graphik.graal.core.term.Term;

public class DefaultFreeVarGen implements SymbolGenerator {

	private static DefaultFreeVarGen instance = null;
	
	public static Term genFreeVar() {
		if(instance == null) {
			instance = new DefaultFreeVarGen("Y");
		}
		return instance.getFreeVar();
	}

	// /////////////////////////////////////////////////////////////////////////
	// 
	// /////////////////////////////////////////////////////////////////////////
	
	private String prefix;
	private int index;

	// /////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	// /////////////////////////////////////////////////////////////////////////

	public DefaultFreeVarGen(String prefix) {
		this.index = 0;
		this.prefix = prefix;
	}

	// /////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public Term getFreeVar() {
		return DefaultTermFactory.instance()
				.createVariable(this.prefix + getFreeIndex());
	}

	// /////////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	// /////////////////////////////////////////////////////////////////////////

	private int getFreeIndex() {
		return index++;
	}

};
