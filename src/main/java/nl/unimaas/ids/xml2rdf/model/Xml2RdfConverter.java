package nl.unimaas.ids.xml2rdf.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

public class Xml2RdfConverter {
	static final ValueFactory valueFactory = SimpleValueFactory.getInstance();
	public static String modelUri;
	public static String dataUri;
	public static String vocabUri;

	private final String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
	private final String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private IRI xmlElement ;
	private IRI xmlAttribute;
	                        ;
	private IRI hasName;
	private IRI hasValue;
	private IRI hasXPath;
	private IRI hasChild;
	private IRI hasAttribute;
	                        
	private IRI subClassOf = valueFactory.createIRI(rdfs, "subClassOf");
	private IRI type = valueFactory.createIRI(rdf, "type");;
	
	private XmlDocument xmlDocument = null;
	private XmlNode xmlNode = null;
	private File inputFile = null;
	private File outputFile = null;
	private IRI graphIRI = null;
	
	public Xml2RdfConverter(File inputFile, File outputFile, String baseUri, String graphUri) {
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.graphIRI = valueFactory.createIRI(graphUri);
		baseUri = StringUtils.appendIfMissing(baseUri, "/");
		
		// Old version: http://ids.unimaas.nl/rdf2xml/model
		Xml2RdfConverter.modelUri = baseUri + "model/";
		Xml2RdfConverter.dataUri= baseUri + "data/";
		Xml2RdfConverter.vocabUri= baseUri + "vocab/";
		
	 	xmlElement = valueFactory.createIRI(Xml2RdfConverter.modelUri, "XmlNode");
	 	xmlAttribute = valueFactory.createIRI(Xml2RdfConverter.modelUri, "XmlAttribute");
	 	hasName = valueFactory.createIRI(Xml2RdfConverter.modelUri, "hasName");
	 	hasValue = valueFactory.createIRI(Xml2RdfConverter.modelUri, "hasValue");
	   	hasXPath = valueFactory.createIRI(Xml2RdfConverter.modelUri, "hasXPath");
	   	hasChild = valueFactory.createIRI(Xml2RdfConverter.modelUri, "hasChild");
	   	hasAttribute = valueFactory.createIRI(Xml2RdfConverter.modelUri, "hasAttribute");
	  
		xmlDocument = new XmlDocument();
		xmlNode = xmlDocument;
	}
	
	public Xml2RdfConverter doWork() throws XMLStreamException, UnsupportedRDFormatException, IOException {
		OutputStream outputStream = new FileOutputStream(outputFile, false);
		if(outputFile.getName().endsWith(".gz"))
			outputStream = new GZIPOutputStream(outputStream);
		RDFWriter rdfWriter = Rio.createWriter(RDFFormat.NQUADS, outputStream);
		rdfWriter.startRDF();
		
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		InputStream inputStream = new FileInputStream(inputFile);
		if(inputFile.getName().toLowerCase().endsWith(".gz"))
			inputStream = new GZIPInputStream(inputStream);
		XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(inputStream);
		
		String name = null;
		
		while(xmlStreamReader.hasNext()) {
			int event = xmlStreamReader.next();
			if(event==XMLStreamConstants.START_ELEMENT) {
				name = xmlStreamReader.getLocalName();
				xmlNode = xmlNode.registerChild(name, null);
				xmlNode.value = null;
				for(int i=0; i<xmlStreamReader.getAttributeCount(); i++) {
					xmlNode.registerAttribute(xmlStreamReader.getAttributeLocalName(i), xmlStreamReader.getAttributeValue(i));
				}
			} else if (event == XMLStreamConstants.CHARACTERS) {
				xmlNode.registerValue(xmlStreamReader.getText(), true);
			} else if (event==XMLStreamConstants.END_ELEMENT) {
				toRdf(xmlNode, rdfWriter);
				// because it is already part of the child-map (for statistics)
				// index will be incremented immediately when registered
				xmlNode.childs.values().forEach(child -> {child.index = -1; child.value = null;});
				xmlNode.attributes.values().forEach(attribute -> {attribute.index = -1; attribute.value = null;});
				xmlNode = xmlNode.parent;
			}
		}
		
		xmlStreamReader.close();
		rdfWriter.endRDF();
		outputStream.close();
		
		return this;
	}
	
	private void toRdf(XmlNode node, final RDFWriter rdfWriter) {
		// first element, let's create the XmlNode subclass
		if(node.isNew) {
			/* Instead of using 
			 * subClassOf can be either XmlNode, Attribute or . RdfType is the XPath. And atm we use hasChildren to point to child nodes
			 * We want to change the hasChildren to use directly the node label as predicate 
			 * Use a different base URL for predicate (http://data2services/vocab instead of model for example)
			if(node.parent.isRoot())
				rdfWriter.handleStatement(valueFactory.createStatement(node.class_iri, subClassOf, xmlElement, graphIRI));
			else
				rdfWriter.handleStatement(valueFactory.createStatement(node.class_iri, subClassOf, node.parent.class_iri, graphIRI)); */
			rdfWriter.handleStatement(valueFactory.createStatement(node.class_iri, subClassOf, xmlElement, graphIRI));
			rdfWriter.handleStatement(valueFactory.createStatement(node.class_iri, hasName, valueFactory.createLiteral(node.name), graphIRI));
			rdfWriter.handleStatement(valueFactory.createStatement(node.class_iri, hasXPath, valueFactory.createLiteral(node.getRelativeXPath()), graphIRI));
			if(!node.parent.isRoot()) {
				rdfWriter.handleStatement(valueFactory.createStatement(node.parent.class_iri, hasChild, node.class_iri, graphIRI));
				// replace hasChild by a predicate based on the XML label
				rdfWriter.handleStatement(valueFactory.createStatement(node.parent.class_iri, hasChild, node.class_iri, graphIRI));
			}
			node.isNew = false;
		}
		// let's check for new attributes
		for(XmlAttribute attribute : node.attributes.values()) {
			if(attribute.isNew) {
				rdfWriter.handleStatement(valueFactory.createStatement(attribute.class_iri, subClassOf, xmlAttribute, graphIRI));
				rdfWriter.handleStatement(valueFactory.createStatement(node.class_iri, hasAttribute, attribute.class_iri, graphIRI));
				rdfWriter.handleStatement(valueFactory.createStatement(attribute.class_iri, hasName, valueFactory.createLiteral(attribute.name), graphIRI));
				rdfWriter.handleStatement(valueFactory.createStatement(attribute.class_iri, hasXPath, valueFactory.createLiteral(attribute.getRelativeXPath()), graphIRI));
				attribute.isNew = false;
			}
		}
		
		// now the data
		rdfWriter.handleStatement(valueFactory.createStatement(node.iri, type, node.class_iri, graphIRI));
		if(!node.parent.isRoot())
			rdfWriter.handleStatement(valueFactory.createStatement(node.parent.iri, hasChild, node.iri, graphIRI));
		if(node.value != null)
			rdfWriter.handleStatement(valueFactory.createStatement(node.iri, hasValue, valueFactory.createLiteral(node.value), graphIRI));
		rdfWriter.handleStatement(valueFactory.createStatement(node.iri, hasXPath, valueFactory.createLiteral(node.getAbsoluteXpath()), graphIRI));
		
		for(XmlAttribute attribute : node.actualAttributes.values()) {
			rdfWriter.handleStatement(valueFactory.createStatement(node.iri, hasAttribute, attribute.iri, graphIRI));
			rdfWriter.handleStatement(valueFactory.createStatement(attribute.iri, type , attribute.class_iri, graphIRI));
			rdfWriter.handleStatement(valueFactory.createStatement(attribute.iri, hasXPath, valueFactory.createLiteral(attribute.getAbsoluteXpath()), graphIRI));
			if(attribute.value != null && !attribute.value.isEmpty()) {
				rdfWriter.handleStatement(valueFactory.createStatement(attribute.iri, hasValue, valueFactory.createLiteral(attribute.value), graphIRI));
			}
			attribute.isNew = false;
		}
	}
	
	public Xml2RdfConverter structuredPrint() {
		printStructureAndStats(xmlDocument, "" , "| ");
		return this;
	}
	
	private void printStructureAndStats(XmlNode node, String indent, String baseIndent) {
		System.out.println(indent + "# " + node.toString());
		
		for(XmlAttribute attribute : node.attributes.values())
			System.out.println(indent + baseIndent + "* " + attribute.toString());
		
		for(XmlNode child : node.childs.values())
			printStructureAndStats(child, indent + baseIndent, baseIndent);
	}

}
