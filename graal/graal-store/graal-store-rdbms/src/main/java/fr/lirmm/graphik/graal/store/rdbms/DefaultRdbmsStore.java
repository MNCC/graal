/**
 * 
 */
package fr.lirmm.graphik.graal.store.rdbms;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.lirmm.graphik.graal.core.Atom;
import fr.lirmm.graphik.graal.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.core.DefaultConjunctiveQuery;
import fr.lirmm.graphik.graal.core.Predicate;
import fr.lirmm.graphik.graal.core.Rule;
import fr.lirmm.graphik.graal.core.SymbolGenerator;
import fr.lirmm.graphik.graal.core.atomset.AtomSet;
import fr.lirmm.graphik.graal.core.atomset.AtomSetException;
import fr.lirmm.graphik.graal.core.atomset.InMemoryAtomSet;
import fr.lirmm.graphik.graal.core.term.DefaultTermFactory;
import fr.lirmm.graphik.graal.core.term.Term;
import fr.lirmm.graphik.graal.core.term.Term.Type;
import fr.lirmm.graphik.graal.store.rdbms.driver.RdbmsDriver;

/**
 * @author Clément Sipieter (INRIA) <clement@6pi.fr>
 * 
 *         This class represents an implementation of a store in a Relational
 *         Database System where each predicates is stored in a dedicated table.
 */
public class DefaultRdbmsStore extends AbstractRdbmsStore {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(DefaultRdbmsStore.class);

	private static final int VARCHAR_SIZE = 128;

	private static final String MAX_VARIABLE_ID_COUNTER = "max_variable_id";
	private static final String MAX_PREDICATE_ID_COUNTER = "max_predicate_id";

	// tables names
	static final String COUNTER_TABLE_NAME = "counters";
	static final String PREDICATE_TABLE_NAME = "predicates";
	static final String TERM_TABLE_NAME = "terms";

	// table fields name
	static final String PREFIX_TERM_FIELD = "term";

	// queries
	private static final String GET_PREDICATE_QUERY = "SELECT * FROM "
													+ PREDICATE_TABLE_NAME
													+ " WHERE predicate_label = ? " 
													+ " AND predicate_arity = ?;";
	private static final String INSERT_PREDICATE_QUERY = "INSERT INTO "
													   + PREDICATE_TABLE_NAME
													   + " VALUES ( ?, ?, ?)";

	private static final String GET_ALL_TERMS_QUERY = "SELECT * FROM "
												   + TERM_TABLE_NAME
												   + ";";
	
	private static final String GET_TERMS_BY_TYPE = "SELECT * FROM "
			   + TERM_TABLE_NAME
			   + " WHERE term_type = ?;";
	
	private static final String GET_TERM_QUERY = "SELECT * FROM "
											   + TERM_TABLE_NAME
											   + " WHERE term = ?;";

	// counter queries
	private static final String GET_COUNTER_VALUE_QUERY = "SELECT value FROM "
													   + COUNTER_TABLE_NAME
													   + " WHERE counter_name = ?;";

	private static final String UPDATE_COUNTER_VALUE_QUERY = "UPDATE "
														  + COUNTER_TABLE_NAME
														  + " SET value = ? WHERE counter_name = ?;";

	private static final String TEST_SCHEMA_QUERY = "SELECT 0 FROM "
													+ PREDICATE_TABLE_NAME
													+ " LIMIT 1";

	private PreparedStatement getPredicateTableStatement;
	private PreparedStatement insertPredicateStatement;

	private PreparedStatement getTermStatement;

	private PreparedStatement getCounterValueStatement;
	private PreparedStatement updateCounterValueStatement;

	private PreparedStatement getTermsByTypeStatement;
	
	private TreeMap<Predicate, String> predicateMap = new TreeMap<Predicate, String>();

	// /////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	// /////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 * @param driver
	 * @throws SQLException
	 * @throws AtomSetException
	 */
	public DefaultRdbmsStore(RdbmsDriver driver) throws AtomSetException {
		super(driver);

		try {
			this.getPredicateTableStatement = this.getConnection()
					.prepareStatement(GET_PREDICATE_QUERY);
			this.insertPredicateStatement = this.getConnection()
					.prepareStatement(INSERT_PREDICATE_QUERY);
			this.getCounterValueStatement = this.getConnection()
					.prepareStatement(GET_COUNTER_VALUE_QUERY);
			this.updateCounterValueStatement = this.getConnection()
					.prepareStatement(UPDATE_COUNTER_VALUE_QUERY);
			this.getTermStatement = this.getConnection().prepareStatement(
					GET_TERM_QUERY);
			this.getTermsByTypeStatement = this.getConnection().prepareStatement(GET_TERMS_BY_TYPE);
		} catch (SQLException e) {
			throw new AtomSetException(e.getMessage(), e);
		}

	}

	@Override
	protected boolean testDatabaseSchema() throws AtomSetException {
		Statement statement = null;
		try {
			statement = this.createStatement();
			ResultSet rs = statement.executeQuery(TEST_SCHEMA_QUERY);
			rs.close();
		} catch (SQLException e) {
			return false;
		} catch (AtomSetException e) {
			throw new AtomSetException(e.getMessage(), e);
		} finally {
			if(statement != null) {
				try {
					statement.close();
					this.getConnection().rollback();
				} catch (SQLException e) {
					throw new AtomSetException(e);
				}
			}
		}

		return true;
	}

	@Override
	protected void createDatabaseSchema() throws AtomSetException {
		final String createPredicateTableQuery = "CREATE TABLE "
												 + PREDICATE_TABLE_NAME
												 + "(predicate_label varchar("
												 + VARCHAR_SIZE
												 + "), predicate_arity int, "
												 + "predicate_table_name varchar("
												 + VARCHAR_SIZE
												 + "), PRIMARY KEY (predicate_label, predicate_arity));";

		final String createTermTableQuery = "CREATE TABLE "
											+ TERM_TABLE_NAME
											+ " (term varchar("
											+ VARCHAR_SIZE
											+ "), term_type varchar("
											+ VARCHAR_SIZE
											+ "), PRIMARY KEY (term));";

		final String termTypeTableName = "term_type";
		final String createTermTypeTableQuery = "CREATE TABLE "
												+ termTypeTableName
												+ " (term_type varchar("
												+ VARCHAR_SIZE
												+ "), PRIMARY KEY (term_type));";

		final String insertTermTypeQuery = "INSERT INTO "
										   + termTypeTableName
										   + " values (?);";
		Statement statement = null;
		PreparedStatement pstat = null;
		try {
			statement = this.createStatement();
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Create database schema");

			statement.executeUpdate("create table test (i int)");
			statement.executeUpdate("insert into test values (1)");
			if (LOGGER.isDebugEnabled())
				LOGGER.debug(createPredicateTableQuery);
			statement.executeUpdate(createPredicateTableQuery);

			if (LOGGER.isDebugEnabled())
				LOGGER.debug(createTermTypeTableQuery);
			statement.executeUpdate(createTermTypeTableQuery);

			pstat = this.getConnection().prepareStatement(insertTermTypeQuery);
			Term.Type[] types = Term.Type.values();
			for (int i = 0; i < types.length; ++i) {
				pstat.setString(1, types[i].toString());
				pstat.addBatch();
			}
			pstat.executeBatch();
			pstat.close();

			if (LOGGER.isDebugEnabled())
				LOGGER.debug(createTermTableQuery);
			statement.executeUpdate(createTermTableQuery);

			final String createCounterTableQuery = "CREATE TABLE "
												   + COUNTER_TABLE_NAME
												   + " (counter_name varchar(64), value BIGINT, PRIMARY KEY (counter_name));";

			if (LOGGER.isDebugEnabled())
				LOGGER.debug(createCounterTableQuery);
			statement.executeUpdate(createCounterTableQuery);
		} catch (SQLException e) {
			throw new AtomSetException(e.getMessage(), e);
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					throw new AtomSetException(e);
				}
			}
		}
		
		try {
			final String insertCounterTableQuery = "INSERT INTO "
					   + COUNTER_TABLE_NAME
					   + " values (?, -1);";
			final String[] counters = { MAX_PREDICATE_ID_COUNTER,
					MAX_VARIABLE_ID_COUNTER };
			pstat = this.getConnection().prepareStatement(
					insertCounterTableQuery);
			for (int i = 0; i < counters.length; ++i) {
				pstat.setString(1, counters[i]);
				pstat.addBatch();
			}
			pstat.executeBatch();
			this.getConnection().commit();
		} catch (SQLException e) {
			throw new AtomSetException(e.getMessage(), e);
		} finally {
			if (pstat != null) {
				try {
					pstat.close();
				} catch (SQLException e) {
					throw new AtomSetException(e);
				}
			}
		}

	}

	// /////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public Iterator<Atom> iterator() {
		try {
			return new DefaultRdbmsIterator(this);
		} catch (AtomSetException e) {
			if(LOGGER.isErrorEnabled()) {
				LOGGER.error(e.getMessage(), e);
			}
			return null;
		}
	}

	@Override
	public SymbolGenerator getFreeVarGen() {
		return new RdbmsSymbolGenenrator(this.getConnection(),
				MAX_VARIABLE_ID_COUNTER, GET_COUNTER_VALUE_QUERY,
				UPDATE_COUNTER_VALUE_QUERY);
	}

	@Override
	public boolean contains(Atom atom) throws AtomSetException {
		boolean res = false;
		Term term;
		Statement statement = null;
		int termIndex = -1;
		String tableName = this.predicateTableExist(atom.getPredicate());
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(atom.getPredicate() + " -- > " + tableName);
		}
		if (tableName != null) {

			StringBuilder query = new StringBuilder("SELECT * FROM ");
			query.append(tableName);
			query.append(" WHERE ");

			Iterator<Term> terms = atom.getTerms().iterator();

			term = terms.next();            // TODO: FIX THIS => if arity = 0 -> crash ?!
			++termIndex;
			query.append("term").append(termIndex).append(" = \'").append(term)
					.append('\'');

			while (terms.hasNext()) {
				term = terms.next();
				++termIndex;
				query.append(" and ").append(PREFIX_TERM_FIELD)
						.append(termIndex).append(" = \'").append(term)
						.append('\'');
			}
			query.append(" LIMIT 1;");

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(atom.toString() + " : " + query.toString());
			}
			ResultSet results;
			try {
				statement = this.createStatement();
				results = statement.executeQuery(query.toString());
				if (results.next()) {
					res = true;
				}
				results.close();
			} catch (SQLException e) {
				if(statement != null) {
					try {
						statement.close();
					} catch (SQLException sqlEx) {
						throw new AtomSetException(sqlEx);
					}
				}
				throw new AtomSetException(e);
			}
		}

		return res;
	}

	@Override
	public Set<Term> getTerms() throws AtomSetException {
		Statement statement = this.createStatement();
		ResultSet results = null;
		Set<Term> terms;
		
		try {
			results = statement.executeQuery(GET_ALL_TERMS_QUERY);
			terms = new TreeSet<Term>();

			while (results.next()) {
				terms.add(DefaultTermFactory.instance().createTerm(
						results.getString(1),
						Term.Type
						.valueOf(results.getString(2))));
			}
			
			results.close();
		} catch (SQLException e) {
			throw new AtomSetException(e);
		} finally {
			if(statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					throw new AtomSetException(e);
				}
			}
		}
		
		
		
		return terms;
	}

	@Override
	public Set<Term> getTerms(Type type) throws AtomSetException {
		ResultSet results = null;
		Set<Term> terms = new TreeSet<Term>();

		try {
			this.getTermsByTypeStatement.setString(1, type.toString());
			results = this.getTermsByTypeStatement.executeQuery();
			while (results.next()) {
				terms.add(DefaultTermFactory.instance().createTerm(
						results.getString(1), type));
			}
			results.close();
		} catch (SQLException e) {
			throw new AtomSetException(e);
		}
		return terms;
	}

	/**
	 * Get a term by its label
	 * 
	 * @param label
	 * @return
	 * @throws AtomSetException
	 */
	@Override
	public Term getTerm(String label) throws AtomSetException {
		ResultSet results;
		Term term = null;

		try {
			this.getTermStatement.setString(1, label);
			results = this.getTermStatement.executeQuery();
			if (results.next()) {
				term = DefaultTermFactory.instance().createTerm(
						results.getString(1),
						Term.Type.valueOf(results
						.getString(2)));
			}
			results.close();
		} catch (SQLException e) {
			throw new AtomSetException(e);
		}
		return term;

	}

	/**
	 * Transforms the fact into a SQL statement.
	 */
	@Override
	public String transformToSQL(ConjunctiveQuery cquery)
			throws AtomSetException {

		AtomSet atomSet = cquery.getAtomSet();

		StringBuilder fields = new StringBuilder();
		StringBuilder tables = new StringBuilder();
		StringBuilder where = new StringBuilder();

		HashMap<Atom, String> tableNames = new HashMap<Atom, String>();
		HashMap<String, String> lastOccurrence = new HashMap<String, String>();

		ArrayList<String> constants = new ArrayList<String>();
		ArrayList<String> equivalences = new ArrayList<String>();
		TreeMap<Term, String> columns = new TreeMap<Term, String>();

		int count = -1;
		for (Atom atom : atomSet) {
			String tableName = "atom" + ++count;
			tableNames.put(atom, tableName);
		}

		// Create WHERE clause
		for (Atom atom : atomSet) {
			String currentAtom = tableNames.get(atom) + ".";

			int position = 0;
			for (Term term : atom.getTerms()) {
				String thisTerm = currentAtom + PREFIX_TERM_FIELD + position;
				if (term.isConstant()) {
					constants.add(thisTerm + " = '" + term + "'");
				} else {
					if (lastOccurrence.containsKey(term.toString())) {
						equivalences.add(lastOccurrence.get(term.toString())
								+ " = " + thisTerm);
					}
					lastOccurrence.put(term.toString(), thisTerm);
					if (cquery.getAnswerVariables().contains(term))
						columns.put(term, thisTerm + " as " + term);
				}
				++position;
			}
		}

		for (String equivalence : equivalences) {
			if (where.length() != 0)
				where.append(" AND ");

			where.append(equivalence);
		}

		for (String constant : constants) {
			if (where.length() != 0)
				where.append(" AND ");

			where.append(constant);
		}

		// Create FROM clause
		String tableName = null;
		for (Map.Entry<Atom, String> entries : tableNames.entrySet()) {
			if (tables.length() != 0)
				tables.append(", ");

			tableName = this.predicateTableExist(entries.getKey()
					.getPredicate());
			if (tableName == null)
				return this
						.createEmptyQuery(cquery.getAnswerVariables().size());
			else
				tables.append(tableName);

			tables.append(" as ");
			tables.append(entries.getValue());
		}

		// Create SELECT clause
		for (Term t : cquery.getAnswerVariables()) {
			if (fields.length() != 0)
				fields.append(", ");

			fields.append(columns.get(t));
		}

		StringBuilder query = new StringBuilder("SELECT DISTINCT ");
		if (fields.length() > 0)
			query.append(fields);
		else
			query.append("1");

		if (tables.length() > 0)
			query.append(" FROM ").append(tables);

		if (where.length() > 0)
			query.append(" WHERE ").append(where);

		query.append(';');

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Generated SQL query :" + cquery + " --> "
					+ query.toString());

		return query.toString();
	}

	@Override
	public Iterator<String> transformToSQL(Rule rangeRestrictedRule)
			throws AtomSetException {
		Collection<String> queries = new LinkedList<String>();
		InMemoryAtomSet body = rangeRestrictedRule.getBody();
		for (Atom headAtom : rangeRestrictedRule.getHead()) {
			String tableName = this.getPredicateTable(headAtom.getPredicate()); 
			ConjunctiveQuery query = new DefaultConjunctiveQuery(body, headAtom.getTerms());
			String selectQuery = this.transformToSQL(query);
			queries.add(this.getDriver().getInsertOrIgnoreStatement(tableName,
					selectQuery));
		}
		return queries.iterator();
	}

	// /////////////////////////////////////////////////////////////////////////
	// PROTECTED METHODS
	// /////////////////////////////////////////////////////////////////////////

	/**
	 * @param statement
	 * @param atom
	 * @throws AtomSetException
	 * @throws SQLException
	 */
	@Override
	protected Statement add(Statement statement, Atom atom)
														   throws AtomSetException {
		try {
			for(Term t : atom.getTerms()) {
				this.add(statement, t);
			}
			String tableName = this.getPredicateTable(atom.getPredicate());
			Map<String, Object> data = new TreeMap<String, Object>();
			int i = -1;
			for(Term t : atom.getTerms()) {
				++i;
				data.put("term" + i, t);
			}
			String query = this.getDriver().getInsertOrIgnoreStatement(
					tableName, data);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(atom.toString() + " : " + query.toString());
			}
			statement.addBatch(query);
		} catch (SQLException e) {
			throw new AtomSetException(e.getMessage(), e);
		}
		return statement;
	}
	
	/**
	 * 
	 * @param atom
	 * @return
	 */
	@Override
	protected Statement remove(Statement statement, Atom atom) throws AtomSetException {
		try {
			String tableName = this.predicateTableExist(atom.getPredicate());
			if (tableName == null) 
				return statement;
			StringBuilder query = new StringBuilder("DELETE FROM ");
			query.append(tableName);
			query.append(" WHERE ");

			int termIndex = 0;
			for (Term t : atom.getTerms()) {
				if (termIndex != 0) {
					query.append(" and ");
				}
				query.append(PREFIX_TERM_FIELD).append(termIndex).append(" = '").append(t).append("'");
				++termIndex;
			}
			query.append(";");

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Removing " + atom.toString() + " : " + query.toString());
			}
			statement.addBatch(query.toString());
		} catch (SQLException e) {
			throw new AtomSetException(e.getMessage(), e);
		}
		return statement;
	}
	
	// /////////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	// /////////////////////////////////////////////////////////////////////////

	private void add(Statement statement, Term term) throws AtomSetException {
		try {
			Map<String, Object> data = new TreeMap<String, Object>();
			data.put("term", term.getIdentifier());
			data.put("term_type", term.getType());
			String query = this.getDriver()
					.getInsertOrIgnoreStatement(
					TERM_TABLE_NAME, data);
			statement.executeUpdate(query);
		} catch (SQLException e) {
			throw new AtomSetException("Error during insertion of a term", e);
		}
	}

	/**
	 * Get the table name of this predicate. If there is no table for this, a
	 * new table is created.
	 * 
	 * @param predicate
	 * @return
	 * @throws SQLException
	 * @throws AtomSetException
	 */
	private String getPredicateTable(Predicate predicate)
			throws AtomSetException {
		// look in the local map
		String tableName = this.predicateMap.get(predicate);

		if (tableName == null) {
			// look in the database
			tableName = this.predicateTableExist(predicate);
			if (tableName == null) {
				try {
					tableName = this.createPredicateTable(predicate);
				} catch (SQLException e) {
					throw new AtomSetException(
							"Error during the creation of a table for a predicate",
							e);
				}
			}

			// add to the local map
			this.predicateMap.put(predicate, tableName);
		}

		return tableName;
	}

	/**
	 * 
	 * @param predicate
	 * @return
	 * @throws AtomSetException
	 * @throws SQLException
	 */
	private String createPredicateTable(Predicate predicate)
															throws SQLException,
															AtomSetException {
		String tableName = "pred" + this.getFreePredicateId();
		if (predicate.getArity() >= 1) {
			Statement stat = this.createStatement();
			stat.executeUpdate(generateCreateTablePredicateQuery(tableName,
					predicate));
			if(stat != null) {
				try {
					stat.close();
				} catch (SQLException e) {
					throw new AtomSetException(e);
				}
			}
			insertPredicate(tableName, predicate);
		} else {
			throw new AtomSetException("Unsupported arity 0"); // TODO Why ?!
		}
		return tableName;
	}

	private static String generateCreateTablePredicateQuery(
															String tableName,
															Predicate predicate) {
		StringBuilder primaryKey = new StringBuilder("PRIMARY KEY (");
		StringBuilder query = new StringBuilder("CREATE TABLE ");
		query.append(tableName);

		query.append('(').append(PREFIX_TERM_FIELD).append('0');
		query.append(" varchar(").append(VARCHAR_SIZE).append(")");
		primaryKey.append("term0");
		for (int i = 1; i < predicate.getArity(); i++) {
			query.append(", ").append(PREFIX_TERM_FIELD).append(i)
					.append(" varchar(" + VARCHAR_SIZE + ")");
			primaryKey.append(", term" + i);
		}
		primaryKey.append(")");

		query.append(',');
		query.append(primaryKey);
		query.append(");");
		return query.toString();
	}

	/**
	 * 
	 * @param tableName
	 * @param predicate
	 * @throws SQLException
	 */
	private void insertPredicate(String tableName, Predicate predicate)
																	   throws SQLException {
		this.insertPredicateStatement.setString(1, predicate.getIdentifier());
		this.insertPredicateStatement.setInt(2, predicate.getArity());
		this.insertPredicateStatement.setString(3, tableName);
		this.insertPredicateStatement.execute();
	}

	/**
	 * 
	 * @param dbConnection
	 * @param predicate
	 * @return the table name corresponding to this predicate or null if this
	 *         predicate doesn't exist.
	 * @throws SQLException
	 */
	private String predicateTableExist(Predicate predicate)
														   throws AtomSetException {
		String predicateTableName = null;

		try {
			this.getPredicateTableStatement.setString(1, predicate.getIdentifier());
			this.getPredicateTableStatement.setInt(2, predicate.getArity());
			ResultSet results = this.getPredicateTableStatement.executeQuery();

			if (results.next())
				predicateTableName = results.getString("predicate_table_name");
			
			results.close();
		} catch (SQLException e) {
			throw new AtomSetException(e);
		}

		return predicateTableName;
	}

	/**
	 * 
	 * @param dbConnection
	 * @return
	 * @throws SQLException
	 */
	private long getFreePredicateId() throws SQLException {
		long value;

		this.getCounterValueStatement.setString(1, MAX_PREDICATE_ID_COUNTER);
		ResultSet result = this.getCounterValueStatement.executeQuery();
		result.next();
		value = result.getLong("value") + 1;
		result.close();
		this.updateCounterValueStatement.setLong(1, value);
		this.updateCounterValueStatement.setString(2, MAX_PREDICATE_ID_COUNTER);
		this.updateCounterValueStatement.executeUpdate();

		return value;
	}

	@Override
	public Iterator<Predicate> predicatesIterator() throws AtomSetException {
		return new DefaultRdbmsPredicateReader(this.getDriver());
	}
	
	@Override
	public Set<Predicate> getPredicates() throws AtomSetException {
		TreeSet<Predicate> set = new TreeSet<Predicate>();
		Iterator<Predicate> it = this.predicatesIterator();
		while(it.hasNext()) {
			set.add(it.next());
		}
		return set;
	}
	
	/**
	 * Return a SQL query like below:
	 * "select 0, …, 0 from (select 0) as t where 0;"
	 * @param nbAnswerVars number of column needed
	 * @return 
	 */
	private String createEmptyQuery(int nbAnswerVars) {
		StringBuilder s = new StringBuilder("select 0");
		
		for(int i=1; i<nbAnswerVars; ++i)
			s.append(", 0");
		
		s.append(" from (select 0) as t where 0;");
		return s.toString();

	}

	@Override
	public void clear() throws AtomSetException {
		this.removeAll(this.iterator());
	}

}