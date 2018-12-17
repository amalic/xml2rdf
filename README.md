[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)


# About
This tool converts any xml file into a generic rdf model.



# better_subclass_structure BRANCH

SPARQL query to transform the new structure in `insert_drugbank_product_Polypeptide_Target.rq`

We filter directly on predicates instead of prefixes, much more comfortable

```sql
PREFIX x2rm: <http://data2services/model/>
PREFIX d2s: <http://data2services/vocab/>
PREFIX ido: <http://identifiers.org/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX obo: <http://purl.obolibrary.org/obo/>
PREFIX dc: <http://purl.org/dc/elements/1.1/>
PREFIX dcterms: <http://purl.org/dc/terms/>
PREFIX bl: <http://w3id.org/biolink/vocab/>
INSERT { 
    GRAPH <http://data2services/biolink/drugbank> {   

        ?targetUri a bl:GeneOrGeneProduct ;
            bl:same_as ?polypeptideAttrUri ;
            bl:in_taxon ?ncbiTaxonIdentifierUri ;
            bl:category ?polypeptidePfamUri .

        # Get GO TERM
        ?goTermAssociationUri a bl:GeneToGoTermAssociation ;
            bl:relation bl:category ;
            bl:gene_to_go_term_association_object ?targetUri ;
            bl:gene_to_go_term_association_subject ?goTermUri .

        # Direct link
        #?targetUri bl:category ?goTermUri .

        ?goTermUri a bl:GeneOntologyClass ;
            bl:description ?polyGoTermDescription ;
            bl:category ?goTermCategoryUri .

        ?goTermCategoryUri a bl:OntologyClass ;
            bl:name ?polyGoTermCategory .


        # Get Pfam
        ?polypeptidePfamUri bl:id ?polyPfamId ;
            bl:name ?polyPfamName . 

        # Get TAXON
        ?ncbiTaxonIdentifierUri a bl:OrganismTaxon ;
            bl:id ?ncbiTaxonId ;
            bl:name ?ncbiTaxonName .

        #?polypeptideAttrUri a bl:GeneOrGeneProduct ;
        #    bl:same_as ?polypeptideAttrUniprotUri .

    }
}
WHERE {
    SERVICE <http://localhost:7200/repositories/test>  {
        GRAPH <http://data2services/graph/xml2rdf> {

            # Iterate directly on targets
            ?targetObj a x2rm:drugbank\/drug\/targets\/target ;
                # Get target ID
                 d2s:id [
                    x2rm:hasValue ?targetId
                ] ;
                d2s:polypeptide [
                    x2rm:hasAttribute [
                        a x2rm:drugbank\/drug\/targets\/target\/polypeptide\/\@id ;
                        x2rm:hasValue ?polypeptideAttrId
                    ] ;
                    x2rm:hasAttribute [
                        a x2rm:drugbank\/drug\/targets\/target\/polypeptide\/\@source ;
                        x2rm:hasValue "Swiss-Prot"
                    ] ;
                    d2s:pfams [ 
                        d2s:pfam [ 
                            d2s:identifier [ 
                                x2rm:hasValue ?polyPfamId
                            ] ;
                            d2s:name [ 
                                x2rm:hasValue ?polyPfamName
                            ]
                        ]
                    ] ;
                    d2s:go-classifiers [ 
                        d2s:go-classifier [ 
                            d2s:category [ 
                                x2rm:hasValue ?polyGoTermCategory
                            ] ;
                            d2s:description [ 
                                x2rm:hasValue ?polyGoTermDescription
                            ]
                        ]
                    ] ;
                    d2s:organism [ 
                        x2rm:hasAttribute [ 
                            a x2rm:drugbank\/drug\/targets\/target\/polypeptide\/organism\/\@ncbi-taxonomy-id ;
                            x2rm:hasValue ?ncbiTaxonId
                        ] ;
                        x2rm:hasValue ?ncbiTaxonName
                    ]
                ]
                    

            BIND ( iri(concat("http://data2services/data/drugbank/", ?targetId)) AS ?targetUri )
            BIND ( iri(concat("http://identifiers.org/uniprot/", ?polypeptideAttrId)) AS ?polypeptideAttrUri )
            #BIND ( iri(concat("https://purl.uniprot.org/uniprot/", ?polypeptideAttrId)) AS ?polypeptideAttrUniprotUri )

            BIND ( iri(concat("http://data2services/data/association/go-term/", md5(concat(?targetId, ?polyGoTermDescription)))) AS ?goTermAssociationUri )
            BIND ( iri(concat("http://data2services/data/go-classifier/", md5(?polyGoTermDescription))) AS ?goTermUri )
            BIND ( iri(concat("http://data2services/data/go-category/", ?polyGoTermCategory)) AS ?goTermCategoryUri )
            BIND ( iri(concat("http://identifiers.org/pfam/", ?polyPfamId)) AS ?polypeptidePfamUri )

            BIND ( iri(concat("http://identifiers.org/taxonomy/", ?ncbiTaxonId)) AS ?ncbiTaxonIdentifierUri )

        }
    }
}
```



## RDF model
```shell
PREFIX x2rm: <http://data2services/model#>
PREFIX x2rd: <http://data2services/data/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl:  <http://www.w3.org/2002/07/owl#>
PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

insert data {
    # Base XmlElement
    x2rm:XmlElement a rdfs:type .
    # Attributes
    x2rm:hasName a rdf:Property ;
        rdfs:domain x2rm:XmlElement;
        rdfs:type rdfs:Literal .
    x2rm:hasValue a rdf:Property ;
        rdfs:domain x2rm:XmlElement;
        rdfs:type rdfs:Literal .
    x2rm:hasXPath a rdf:Property ;
        rdfs:domain x2rm:XmlElement;
        rdfs:type rdfs:Literal . 
        
    # XmlAttribue
    x2rm:XmlAttribute rdfs:subClassOf x2rm:XmlElement .
    
    # XmlNode
    x2rm:XmlNode rdfs:subClassOf x2rm:XmlElement .
    # Attributes
    x2rm:hasChild a rdf:Property ;
        rdfs:domain x2rm:XmlNode ;
        rdfs:range x2rm:XmlNode .
    x2rm:hasAttribute a rdf:Property ;
        rdfs:domain x2rm:XmlNode ;
        rdfs:range x2rm:XmlAttribute .
        
    # Inverse attributes 
    x2rm:isNameOf owl:inverseOf x2rm:hasName .
    x2rm:isValueOf owl:inverseOf x2rm:hasValue.
    x2rm:isXPathOf owl:inverseOf x2rm:hasXPath .
    x2rm:isChildOf owl:inverseOf x2rm:hasChild .
    x2rm:isAttributeOf owl:inverseOf x2rm:hasAttribute .
}
```
# Docker
## Build
```shell
docker build -t xml2rdf .
```
## Usage
```shell
docker run --rm -it xml2rdf -?

Usage: xml2rdf [-?] [-b=<baseUri>] [-g=<graphUri>] -i=<inputFilePath>
               -o=<outputFilePath>
  -?, --help                display a help message
  -b, --baseuri=<baseUri>   Base URI. Default is http://data2services/
  -g, --graphuri=<graphUri> Graph URI. Default is http://data2services/xml2rdf/graph
  -i, --inputfile=<inputFilePath>
                            Path to input file (.xml or .xml.gz)
  -o, --outputfile=<outputFilePath>
                            Path to output file (.nq or .nq.gz)
```
## Run

### Linux / OSX
```shell
docker run --rm -it -v /data/xml2rdfdata:/data xml2rdf  -i "/data/input.xml.gz" -o "/data/output.nq.gz" -b "http://data2services/" -g "http://data2services/xml2rdf/graph"
```
### Windows
```shell
docker run --rm -it -v c:/data/xml2rdfdata:/data xml2rdf  -i "/data/input.xml.gz" -o "/data/output.nq.gz" -b "http://data2services/" -g "http://data2services/xml2rdf/graph"
```

