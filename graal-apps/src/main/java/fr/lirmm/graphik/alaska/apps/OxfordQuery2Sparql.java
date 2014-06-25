package fr.lirmm.graphik.alaska.apps;
import java.util.LinkedList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import fr.lirmm.graphik.graal.parser.ParseException;
import fr.lirmm.graphik.graal.parser.oxford.BasicOxfordQueryParserListener;
import fr.lirmm.graphik.graal.parser.oxford.OxfordQueryParser;
import fr.lirmm.graphik.graal.writer.SparqlConjunctiveQueryWriter;
import fr.lirmm.graphik.graal.writer.WriterException;


/**
 * @author Clément Sipieter (INRIA) <clement@6pi.fr>
 *
 */
public class OxfordQuery2Sparql {
	
	@Parameter(names = {"-p", "--prefix"}, description = "Rdf default prefix")
	private String rdfPrefix = "";
	
	@Parameter(names = {"-h", "--help"}, help = true)
	private boolean help;
	
	@Parameter
	private List<String> queries = new LinkedList<String>();
	
	
	public static void main(String[] args) throws ParseException, WriterException {

		OxfordQuery2Sparql options = new OxfordQuery2Sparql();
		JCommander commander = new JCommander(options, args);
		
		if(options.help) {
			commander.usage();
			System.exit(0);
		}
		
		for(String query : options.queries) {
			OxfordQueryParser parser = new OxfordQueryParser(query);
			BasicOxfordQueryParserListener listener = new BasicOxfordQueryParserListener();
			parser.addListener(listener);
			parser.parse();
			
			SparqlConjunctiveQueryWriter writer = new SparqlConjunctiveQueryWriter();
			writer.write(listener.getQuery(), options.rdfPrefix);
		}
	}


	/**
	 * 
	 */
	private static void usage() {
		// TODO implement this method
		throw new Error("This method isn't implemented");
	}
}
