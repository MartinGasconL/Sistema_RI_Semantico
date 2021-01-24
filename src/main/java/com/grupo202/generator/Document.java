package com.grupo202.generator;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Document {

    private final String TITLE_ID = "titulo";
    private final String PUBLISHER_ID = "publicador";
    private final String LANGUAGE_ID = "idioma";
    private final String SUBJECT_ID = "tema";
    private final String DATE_ID = "fecha";
    private final String LICENSE_ID = "licencia";
    private final String DESCRIPTION_ID = "descripcion";
    private final String AUTHOR_ID = "autor";
    private final String CONTRIBUTOR_ID = "tutor";



    private final String DC_TITLE_ID = "dc:title";
    private final String DC_PUBLISHER_ID = "dc:publisher";
    private final String DC_LANGUAGE_ID = "dc:language";
    private final String DC_SUBJECT_ID = "dc:subject";
    private final String DC_DATE_ID = "dc:date";
    private final String DC_LICENSE_ID = "dc:rights";
    private final String DC_DESCRIPTION_ID = "dc:description";
    private final String DC_AUTHOR_ID = "dc:creator" ;
    private final String DC_CONTRIBUTOR_ID = "dc:contributor";
    private final String DC_TYPE_ID = "dc:type" ;

    private String id;
    private String title;
    private String publisher;
    private List<String> language;
    private List<String> subject;
    private String date;
    private String license;
    private String description;
    private List<String> author;
    private List<String> contributor;
    private String type;

    /**
     * This class specifies the fields of a document and realizes the process of load in a collection model.
     * @param pathname path of the document location.
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public Document(String pathname) throws ParserConfigurationException, IOException, SAXException {
        FileInputStream fis;
        String[] path = pathname.split("/");
        this.id = (path.length > 0)? path[path.length - 1] : "";

        try {
            fis = new FileInputStream(pathname);
        } catch (FileNotFoundException fnfe) {
            // at least on windows, some temporary files raise this exception with an "access denied" message
            // checking if the file can be read doesn't help
            return;
        }

        org.w3c.dom.Document dc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);


        this.title = loadUniqueNode(dc, DC_TITLE_ID);
        this.publisher = loadUniqueNode(dc, DC_PUBLISHER_ID);
        this.language = loadMultipleNode(dc, DC_LANGUAGE_ID);
        this.subject = loadMultipleNode(dc, DC_SUBJECT_ID);
        this.date = loadUniqueNode(dc, DC_DATE_ID);
        this.license = loadUniqueNode(dc, DC_LICENSE_ID);
        this.description = loadUniqueNode(dc, DC_DESCRIPTION_ID);
        this.author = loadMultipleNode(dc, DC_AUTHOR_ID);
        this.contributor = loadMultipleNode(dc, DC_CONTRIBUTOR_ID);
        this.type = loadUniqueNode(dc, DC_TYPE_ID);


    }

    /**
     * Reads a field from a XML formatted document.
     * @param dc XML Document
     * @param tag Name of the field to get the content.
     * @return content of the node which contains the field.
     */
    private String loadUniqueNode(org.w3c.dom.Document dc, String tag) {
        NodeList nl = dc.getElementsByTagName(tag);
        if(nl.item(0) == null)  return null;
        return nl.item(0).getTextContent();
    }
    /**
     * Reads a field from a XML formatted document which contains multiple nodes.
     * @param dc XML Document
     * @param tag Name of the field to get the content.
     * @return content of the nodes which contains the field.
     */
    private List<String> loadMultipleNode(org.w3c.dom.Document dc, String tag) {
        NodeList nl = dc.getElementsByTagName(tag);
        List<String> retval = new ArrayList<>();

        for(int i = 0 ; i < nl.getLength() ; i++){
            retval.add(nl.item(i).getTextContent());
        }
        return retval;
    }


    @Override
    public String toString() {
        return "Document{" +
                "  title='" + title + '\'' +
                ", id='" + id + '\'' +
                ", publisher='" + publisher + '\'' +
                ", language=" + language +
                ", subject=" + subject +
                ", date='" + date + '\'' +
                ", license='" + license + '\'' +
                ", description='" + description + '\'' +
                ", author=" + author +
                ", contributor=" + contributor +
                ", type='" + type + '\'' +
                '}';
    }

    /**
     * Save the document in a Jena model.
     * @param model Jena model with a collection.
     * @param thesaurus Thesaurus with concepts
     * @param NS Base URI of the model.
     */
    public void insertInModel(Model model, Model thesaurus, String NS) {
        Resource document = model.createResource(NS+this.id);
        Resource tipo = model.createResource("http://www.grupo202.com/model#" + this.type.toUpperCase().trim());
        document.addProperty(RDF.type, tipo);

        addProperty(model, NS, document, this.TITLE_ID, this.title);

        addComplexProperty(model, NS, document, this.PUBLISHER_ID, this.publisher, NS + "Organizacion", model.getProperty(NS + "Nombre"));

        if(this.date != null && !this.date.equals(""))
            document.addProperty(model.getProperty(NS + DATE_ID), model.createTypedLiteral(this.date, "xsd:gYear"));

        addProperty(model, NS, document, this.LICENSE_ID, this.license);
        addProperty(model, NS, document, this.DESCRIPTION_ID, this.description);

        for(String l : language)
            addProperty(model, NS, document, this.LANGUAGE_ID, l);

        for(String s : subject) {
            if(!s.equals("")) {
                addComplexProperty(model, NS, document, this.SUBJECT_ID, s.toLowerCase(Locale.ROOT), SKOS.Concept.toString(), SKOS.prefLabel);
                insertSubject(document, NS, model, thesaurus, s);
            }
        }

        for(String a : author)
            addComplexProperty(model, NS, document, this.AUTHOR_ID, a, NS + "Persona", model.getProperty(NS + "Nombre"));
        for(String c : contributor)
            addComplexProperty(model, NS, document, this.CONTRIBUTOR_ID, c, NS + "Persona", model.getProperty(NS + "Nombre"));
    }

    /**
     * Clean a string with special characters and spaces to be able to be part of an URI
     * @param text String to clean.
     * @return Cleaned String.
     */
    private String normalize(String text) {
        text = text.toLowerCase(Locale.ROOT).trim();
        text = text.replace(" ", "-")
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("–", "")
                .replace(",", "")
                .replace(".", "");
        for(int i = 0 ; i < text.length(); i++){
            int ascii = text.charAt(i);
            if( !StringUtils.isNumeric(text.charAt(i)+"") && text.charAt(i) != '-' &&
                    (ascii < 97 || ascii > 122)){
                text = text.replace(text.charAt(i)+"", "");
            }
        }

        return text;
    }

    /**
     * Inserts a subject field in the collection
     * @param document Resource node into insert.
     * @param NS Base URI of the model.
     * @param model Model wich contains the collection.
     * @param thesaurus Thesaurus with concepts.
     * @param s Content to insert.
     */
    private void insertSubject(Resource document, String NS, Model model, Model thesaurus, String s) {
        s= s.replace("'", "");
        s= s.replace("\n", "").toLowerCase(Locale.ROOT);
        String queryString =
                "prefix skos: <http://www.w3.org/2004/02/skos/core#>" +
                        "prefix s: <http://www.grupo202.com/skos/subjects#>" +
                        "select ?parentConcept ?parentLabel WHERE {" +
                            "{ ?childConcept skos:prefLabel '" + s + "' } . " +
                            "{ ?childConcept skos:broader ?parentConcept } . " +
                            "{?parentConcept skos:prefLabel ?parentLabel} " +
                        "}";

        Query q1 = QueryFactory.create(queryString) ;

        try (QueryExecution qexec = QueryExecutionFactory.create(q1, thesaurus)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Resource x = soln.getResource("parentConcept");
                RDFNode z = soln.get("parentLabel");

                Resource newResource = model.createResource(SKOS.Concept);
                Resource resourceToInsert = model.createResource(NS + normalize(x.getURI()));
                resourceToInsert.addProperty(RDF.type, newResource);

                resourceToInsert.addProperty(SKOS.prefLabel, model.createLiteral(z.toString().toLowerCase(Locale.ROOT)));
                document.addProperty(model.getProperty(NS + SUBJECT_ID), resourceToInsert);


            }
        }
    }

    /**
     * Adds a property with a class predicate node (Not literal)
     * @param document Resource node into insert.
     * @param NS Base URI of the model.
     * @param model Model wich contains the collection.
     * @param tag Field to insert
     * @param content Content to insert.
     * @param resourceClass Predicate node class.
     * @param property Property to insert.
     */
    private void addComplexProperty(Model model, String NS, Resource document, String tag, String content, String resourceClass, Property property) {
        Resource newResource = model.createResource(resourceClass);
        Resource resourceToInsert = model.createResource(NS + normalize(content));
        resourceToInsert.addProperty(RDF.type, newResource);

        resourceToInsert.addProperty(property, model.createLiteral(content));
        document.addProperty(model.getProperty(NS + tag), resourceToInsert);
    }

    /**
     *
     * Adds a property with a literal predicate
     * @param document Resource node into insert.
     * @param NS Base URI of the model.
     * @param model Model which contains the collection.
     * @param tag Field to insert
     * @param content Content to insert.
     */
    private void addProperty(Model model, String NS, Resource document, String tag, String content) {
        if(content != null && !content.equals(""))
            document.addProperty(model.getProperty(NS + tag), model.createTypedLiteral(content, "xsd:anyUri"));
    }
}
