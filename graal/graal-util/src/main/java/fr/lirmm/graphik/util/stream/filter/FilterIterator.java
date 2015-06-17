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
package fr.lirmm.graphik.util.stream.filter;

import java.util.Iterator;


/**
 * @author Clément Sipieter (INRIA) <clement@6pi.fr>
 *
 */
public class FilterIterator<U, T extends U> implements Iterator<T> {

	private final Iterator<U> it;
	private final Filter<U> filter;
	private T next;

	public FilterIterator(Iterator<U> it, Filter<U> filter) {
		this.filter = filter;
		this.it = it;
		this.next = null;
	}

	@Override
	public boolean hasNext() {
		if(this.next == null && this.it.hasNext()) {
			U o = this.it.next();
			if(this.filter.filter(o)) {
				this.next = (T) o;
			} else {
				this.hasNext();
			}
		}
		return this.next != null;
	}

	@Override
	public T next() {
		this.hasNext();
		T t = this.next;
		this.next = null;
		return t;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	

}