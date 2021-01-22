package com.grupo202.generator;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.VCARD;
import org.w3c.dom.Node;
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

    private String loadUniqueNode(org.w3c.dom.Document dc, String tag) {
        NodeList nl = dc.getElementsByTagName(tag);
        if(nl.item(0) == null)  return null;
        return nl.item(0).getTextContent();
    }

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

    public void insertInModel(Model model, Model tesaurus, String NS) {
        Resource document = model.createResource(NS+this.id);
        Resource tipo = model.createResource("http://www.grupo202.com/model#" + this.type.toUpperCase().trim());
        document.addProperty(RDF.type, tipo);

        addProperty(model, NS, document, this.TITLE_ID, this.title);
        addProperty(model, NS, document, this.PUBLISHER_ID, this.publisher);
        if(this.date != null && !this.date.equals(""))
            document.addProperty(model.getProperty(NS + DATE_ID), model.createTypedLiteral(this.date, "xsd:gYear"));

        addProperty(model, NS, document, this.LICENSE_ID, this.license);
        addProperty(model, NS, document, this.DESCRIPTION_ID, this.description);

        for(String l : language)
            addProperty(model, NS, document, this.LANGUAGE_ID, l);

        for(String s : subject) {
            if(!s.equals("")) {
                Resource r = model.createResource(SKOS.Concept);
                r.addProperty(SKOS.prefLabel, s);
                document.addProperty(model.getProperty(NS + SUBJECT_ID), r);
                insertSubject(document, NS, model, tesaurus, s);
            }
        }

        for(String a : author)
            addPersonProperty(model, NS, document, this.AUTHOR_ID, a);
        for(String c : contributor)
            addPersonProperty(model, NS, document, this.CONTRIBUTOR_ID, c);
    }

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
            int ascii = (int) text.charAt(i);
            if( !StringUtils.isNumeric(text.charAt(i)+"") && text.charAt(i) != '-' &&
                    (ascii < 97 || ascii > 122)){
                text = text.replace(text.charAt(i)+"", "");
         //       System.out.println("++++++++++++++++++" + text);
            }
        }
       // System.out.println("-------------------- " + text);
        return text; /*.trim()
                .replace(" ","-")
                .replace(",", "")
                .replace(".", "")
                .replace("(", "")
                .replace(")", "")
                .replace("/", "")
                .replace("º", "")
                .replace("™", "")
                .replace("'", "")
                .replace("´", "")
                .replace("\"", "")
                .replace("”", "")
                .replace("#", "")
                .replace("“", "")
                .replace("–", "")
                .replace("+", "")
                .replace("%", "")
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                ;*/
    }

    private void insertSubject(Resource document, String NS, Model model, Model tesaurus, String s) {
       // System.out.println(s);
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

        QueryExecution qexec = QueryExecutionFactory.create(q1, tesaurus) ;

        try {
            ResultSet results = qexec.execSelect() ;
            for ( ; results.hasNext() ; )
            {
                QuerySolution soln = results.nextSolution() ;
                Resource x = soln.getResource("parentConcept");
                RDFNode z = soln.get("parentLabel") ;
                System.out.println("INSERTANDO "+ soln);

                Resource r = model.createResource(/*x.getURI().replace("#","/"), */ SKOS.Concept);
                r.addProperty(SKOS.prefLabel, z.toString());
                document.addProperty(model.getProperty(NS + SUBJECT_ID), r);


                /*Resource r = model.createResource("http://www.grupo202.com/skos/subjects/" + normalize(s), SKOS.Concept);
                r.addProperty(SKOS.prefLabel, s);
                document.addProperty(model.getProperty(NS + SUBJECT_ID), r);*/


            }
        } finally { qexec.close() ; }
    }

    private void addPersonProperty(Model model, String NS, Resource documento, String tag, String content) {
        if(content != null && !content.equals("")) {
            Resource r = model.createResource("http://www.grupo202.com/model#Persona");
            Resource person = model.createResource(NS + content.trim().replace(" ","-").replace(",", ""));
            person.addProperty(RDF.type, r);
            documento.addProperty(model.getProperty(NS + tag), person);
        }
    }

    private void addProperty(Model model, String NS, Resource documento, String tag, String content) {
        if(content != null && !content.equals(""))
            documento.addProperty(model.getProperty(NS + tag), model.createTypedLiteral(content, "xsd:anyUri"));
    }
}
