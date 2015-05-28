/**
 * 
 */
package fr.lirmm.graphik.graal.store.triplestore;

import info.aduna.iteration.Iteration;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.lirmm.graphik.graal.core.Atom;
import fr.lirmm.graphik.graal.core.DefaultAtom;
import fr.lirmm.graphik.graal.core.Predicate;
import fr.lirmm.graphik.graal.core.atomset.AtomSetException;
import fr.lirmm.graphik.graal.core.term.DefaultTermFactory;
import fr.lirmm.graphik.graal.core.term.Term;
import fr.lirmm.graphik.graal.core.term.Term.Type;
import fr.lirmm.graphik.graal.store.AbstractTripleStore;

/**
 * @author Clément Sipieter (INRIA) {@literal <clement@6pi.fr>}
 *
 */
public class SailStore extends AbstractTripleStore {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(SailStore.class);

	private RepositoryConnection connection;
	private ValueFactory valueFactory;
	
	private TupleQuery predicatesQuery;
	private TupleQuery termsQuery;

	public SailStore() throws AtomSetException {
		Repository repo = new SailRepository(new MemoryStore());
		try {
			repo.initialize();
			this.connection = repo.getConnection();
		} catch (RepositoryException e) {
			throw new AtomSetException("Error while creating SailStore", e);
		}
		
		this.valueFactory = repo.getValueFactory();
		
	}

	@Override
	protected void finalize() throws Throwable {
		this.close();
		super.finalize();
	}

	// //////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	// //////////////////////////////////////////////////////////////////////////

	@Override
	public void close() {
		try {
			this.connection.close();
		} catch (RepositoryException e) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error(
						"Error while trying to close sail repository connection.",
						e);
			}
		}
	}

	@Override
	public boolean add(Atom atom) throws AtomSetException {
		try {
			this.connection.add(this.atomToStatement(atom));
		} catch (RepositoryException e) {
			throw new AtomSetException("Error while adding the atom " + atom, e);
		}
		return true;
	}

	@Override
	public boolean addAll(Iterator<? extends Atom> atom)
			throws AtomSetException {
		try {
			this.connection.add(new StatementIterator(atom));
		} catch (RepositoryException e) {
			throw new AtomSetException("Error while adding the atom " + atom, e);
		}
		return true;
	}

	/**
	 * 
	 * @param atom
	 * @return always return true.
	 * @throws AtomSetException
	 */
	@Override
	public boolean remove(Atom atom) throws AtomSetException {
		try {
			this.connection.remove(this.atomToStatement(atom));
		} catch (RepositoryException e) {
			throw new AtomSetException("Error while adding the atoms.", e);
		}
		return true;
	}

	@Override
	public boolean removeAll(Iterator<? extends Atom> atom)
			throws AtomSetException {
		try {
			this.connection.remove(new StatementIterator(atom));
		} catch (RepositoryException e) {
			throw new AtomSetException("Error while removing the atoms.", e);
		}
		return true;
	}

	@Override
	public boolean contains(Atom atom) throws AtomSetException {
		Statement stat = this.atomToStatement(atom);
		try {
			return this.connection.hasStatement(stat, false);
		} catch (RepositoryException e) {
			throw new AtomSetException(e);
		}
	}

	@Override
	public Iterator<Predicate> predicatesIterator() throws AtomSetException {
		TupleQueryResult result;
		try {
			result = this.getPredicatesQuery().evaluate();
			return new PredicatesIterator(result);
		} catch (QueryEvaluationException e) {
			throw new AtomSetException(e);
		}	
	}
	
	@Override 
	public Set<Predicate> getPredicates() throws AtomSetException{
		TreeSet<Predicate> set = new TreeSet<Predicate>();
		Iterator<Predicate> it = this.predicatesIterator();
		while(it.hasNext()) {
			set.add(it.next());
		}
		return set;
	}

	private TupleQuery getPredicatesQuery() throws AtomSetException {
		if (this.predicatesQuery == null) {
			try {
				this.predicatesQuery= this.connection.prepareTupleQuery(
						QueryLanguage.SPARQL, SELECT_PREDICATES_QUERY);
			} catch (RepositoryException e) {
				throw new AtomSetException(e);
			} catch (MalformedQueryException e) {
				if (LOGGER.isErrorEnabled()) {
					LOGGER.error("Error on SPARQL query syntax", e);
				}
			}
		}
		return this.predicatesQuery;
	}
	
	@Override
	public Iterator<Term> termsIterator() throws AtomSetException {
		TupleQueryResult result;
		try {
			result = this.getTermsQuery().evaluate();
			return new TermsIterator(result);
		} catch (QueryEvaluationException e) {
			throw new AtomSetException(e);
		}
	}

	@Override
	public Set<Term> getTerms() throws AtomSetException {
		TreeSet<Term> set = new TreeSet<Term>();
		Iterator<Term> it = this.termsIterator();
		while(it.hasNext()) {
			set.add(it.next());
		}
		return set;
	}

	@Override
	public Iterator<Term> termsIterator(Type type) throws AtomSetException {
		// TODO implements other type
		return this.termsIterator();
	}
	
	@Override
	public Set<Term> getTerms(Type type) throws AtomSetException {
		// TODO implements other type
		return this.getTerms();
	}
	
	private TupleQuery getTermsQuery() throws AtomSetException {
		if (this.termsQuery == null) {
			try {
				this.termsQuery = this.connection.prepareTupleQuery(
						QueryLanguage.SPARQL, SELECT_TERMS_QUERY);
			} catch (RepositoryException e) {
				throw new AtomSetException(e);
			} catch (MalformedQueryException e) {
				if (LOGGER.isErrorEnabled()) {
					LOGGER.error("Error on SPARQL query syntax", e);
				}
			}
		}
		return this.termsQuery;
	}

	@Override
	public void clear() throws AtomSetException {
		try {
			this.connection.clear();
		} catch (RepositoryException e) {
			throw new AtomSetException("Error during cleaning this atomSet", e);
		}
	}

	@Override
	public Iterator<Atom> iterator() {
		try {
			return new AtomIterator(this.connection.getStatements(null, null,
					null, false));
		} catch (RepositoryException e) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("Error during iterator creation", e);
			}
		}
		return null;
	}

	// //////////////////////////////////////////////////////////////////////////
	// PRIVATE
	// //////////////////////////////////////////////////////////////////////////

	private Statement atomToStatement(Atom atom) {
		URI predicate = this.createURI(atom.getPredicate().getIdentifier()
				.toString());
		URI term0 = this.createURI(atom.getTerm(0).getIdentifier()
				.toString());
		URI term1 = this.createURI(atom.getTerm(1).getIdentifier()
				.toString());
		return valueFactory.createStatement(term0, predicate, term1);
	}
	
	/**
	 * Create URI from string. If the specified string is not a valid URI,
	 * the method add a default prefix to the string.
	 */
	private URI createURI(String string) {
		return valueFactory.createURI(string);
	}

	private static Atom statementToAtom(Statement stat) {
		Predicate predicate = valueToPredicate(stat.getPredicate());
		Term term0 = DefaultTermFactory.instance().createConstant(
				stat.getSubject().toString());
		Term term1 = DefaultTermFactory.instance().createConstant(
				stat.getObject().toString());
		return new DefaultAtom(predicate, term0, term1);
	}

	private static Predicate valueToPredicate(Value value) {
		return new Predicate(value.toString(), 2);
	}
	
	private static Term valueToTerm(Value value) {
		return DefaultTermFactory.instance().createConstant(value.toString());
	}

	// //////////////////////////////////////////////////////////////////////////
	// PRIVATE CLASSES
	// //////////////////////////////////////////////////////////////////////////

	private class StatementIterator implements
			Iteration<Statement, RepositoryException> {

		private Iterator<? extends Atom> it;

		public StatementIterator(Iterator<? extends Atom> iterator) {
			this.it = iterator;
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public Statement next() {
			return atomToStatement(it.next());
		}

		@Override
		public void remove() {
			it.remove();
		}

	}

	private class AtomIterator implements Iterator<Atom> {
		RepositoryResult<Statement> it;

		AtomIterator(RepositoryResult<Statement> it) {
			this.it = it;
		}

		@Override
		protected void finalize() throws Throwable {
			this.close();
			super.finalize();
		}

		public void close() {
			try {
				this.it.close();
			} catch (RepositoryException e) {
				LOGGER.error("Error when closing SailStore iterator.");
			}
		}

		@Override
		public boolean hasNext() {
			try {
				if (it.hasNext()) {
					return true;
				} else {
					this.it.close();
					return false;
				}
			} catch (RepositoryException e) {
				// TODO manage this Exception
				LOGGER.error("Error on SailStore iterator.");
				return false;
			}
		}

		@Override
		public Atom next() {
			try {
				return statementToAtom(this.it.next());
			} catch (RepositoryException e) {
				// TODO manage this Exception
				LOGGER.error("Error on SailStore iterator.");
				return null;
			}
		}

		@Override
		public void remove() {
			try {
				this.it.remove();
			} catch (RepositoryException e) {
				// TODO manage this Exception
				LOGGER.error("Error on SailStore iterator.");
			}
		}
	}

	private abstract class TupleQueryResultIterator<E> implements Iterator<E> {

		protected TupleQueryResult it;

		@Override
		protected void finalize() throws Throwable {
			this.close();
			super.finalize();
		}

		public void close() {
			try {
				this.it.close();
			} catch (QueryEvaluationException e) {
				if (LOGGER.isErrorEnabled()) {
					LOGGER.error("Error during iteration closing", e);
				}
			}
		}

		@Override
		public boolean hasNext() {
			try {
				return this.it.hasNext();
			} catch (QueryEvaluationException e) {
				if (LOGGER.isErrorEnabled()) {
					// TODO manage this Exception
					LOGGER.error("Error during iteration", e);
				}
			}
			return false;
		}

		@Override
		public void remove() {
			try {
				this.it.remove();
			} catch (QueryEvaluationException e) {
				if (LOGGER.isErrorEnabled()) {
					// TODO manage this Exception
					LOGGER.error("Error during iteration", e);
				}
			}
		}

	}

	private class PredicatesIterator extends TupleQueryResultIterator<Predicate> {

		PredicatesIterator(TupleQueryResult results) {
			super.it = results;
		}
		
		@Override
		public Predicate next() {
			try {
				return valueToPredicate(this.it.next().getValue("p"));
			} catch (QueryEvaluationException e) {
				if (LOGGER.isErrorEnabled()) {
					// TODO manage this Exception
					LOGGER.error("Error during iteration", e);
				}
			}
			return null;
		}
		
		
	}
	
	private class TermsIterator extends TupleQueryResultIterator<Term> {

		TermsIterator(TupleQueryResult results) {
			super.it = results;
		}
		
		@Override
		public Term next() {
			try {
				return valueToTerm(this.it.next().getValue("term"));
			} catch (QueryEvaluationException e) {
				if (LOGGER.isErrorEnabled()) {
					// TODO manage this Exception
					LOGGER.error("Error during iteration", e);
				}
			}
			return null;
		}
		
		
	}
}
