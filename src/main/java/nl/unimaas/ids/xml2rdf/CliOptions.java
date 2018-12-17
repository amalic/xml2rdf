package nl.unimaas.ids.xml2rdf;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "xml2rdf")
public class CliOptions {
	
	@Option(names = { "-?", "--help" }, usageHelp = true, description = "display a help message")
	boolean help = false;
	
	@Option(names= {"-i", "--inputfile"}, description = "Path to input file (.xml or .xml.gz)", required = true)
	String inputFilePath = null;
	
	@Option(names = {"-o", "--outputfile"}, description = "Path to output file (.nq or .nq.gz)", required = true )
	String outputFilePath = null;
	
	@Option(names = {"-g", "--graphuri"}, description = "Graph URI. Default is http://data2services/xml2rdf/graph" )
	String graphUri = "http://data2services/xml2rdf/graph";
	
	@Option(names = {"-b", "--baseuri"}, description = "Base URI. Default is http://data2services/" )
	String baseUri = "http://data2services/";

}
