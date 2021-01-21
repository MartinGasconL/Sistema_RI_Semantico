package com.grupo202.generator;

import openllet.jena.PelletReasonerFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileManager;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

public class SemanticGenerator {

    private final String NS = "http://www.grupo202.com/model/";

    private SemanticGenerator(@org.jetbrains.annotations.NotNull String[] args) throws ParserConfigurationException, IOException, SAXException {
        String rdf = null;
        String skos = null;
        String owl = null;
        String docs = null;

        for(int i=0;i<args.length;i++) {
            if ("-rdf".equals(args[i])) {
                rdf = args[i+1];
                i++;
            } else if ("-skos".equals(args[i])) {
                skos = args[i+1];
                i++;
            } else if ("-owl".equals(args[i])) {
                owl = args[i+1];
                i++;
            } else if ("-docs".equals(args[i])) {
                docs = args[i+1];
                i++;
            }
        }
        if (rdf == null || skos == null || owl == null || docs == null) {
            System.err.println("Error al suministrar los argumentos ");
            System.exit(1);
        }
        System.out.println("uno");
        Model modelCollection =  FileManager.get().loadModel(owl,"RDF/XML-ABBREV");
        Model skosModel = FileManager.get().loadModel(skos,"RDF/XML-ABBREV");

        loadResources(modelCollection, docs, skosModel);
        System.out.println("dos");

        //Model model = ModelFactory.createUnion(modelCollection, skosModel);
        System.out.println("tres");

        modelCollection.write(new FileOutputStream(rdf), "RDF/XML-ABBREV");

        /*InfModel inf = ModelFactory.createInfModel(PelletReasonerFactory.theInstance().create(), model);
        System.out.println("cinco");

        model = borrarRecursosOWL(inf);
        //mostramos el fragmento deseado del modelo inferido
        System.out.println("------------------------------");
        //model.write(System.out,"RDF/XML-ABBREV");
        System.out.println("------------------------------");*/
        modelCollection.write(new FileOutputStream(rdf), "RDF/XML-ABBREV");
    }


    /**
     * borramos las clases del modelo rdfs que se añaden automáticamene al hacer la inferencia
     * simplemente para facilitar la visualización de la parte que nos interesa
     * si quieres ver todo lo que genera el motor de inferencia comenta estas lineas
     */
    private static Model borrarRecursosOWL(Model inf) {
        //hacemos una copia del modelo ya que el modelo inferido es inmutable
        Model model2 = ModelFactory.createDefaultModel();
        model2.add(inf);
        model2.removeAll(inf.createResource("http://www.w3.org/2002/07/owl#topDataProperty"), null, (RDFNode)null);
        model2.removeAll(inf.createResource("http://www.w3.org/2002/07/owl#topObjectProperty"), null, (RDFNode)null);
        model2.removeAll(inf.createResource("http://www.w3.org/2002/07/owl#Thing"), null, (RDFNode)null);
        model2.removeAll(inf.createResource("http://www.w3.org/2002/07/owl#bottomObjectProperty"), null, (RDFNode)null);
        model2.removeAll(inf.createResource("http://www.w3.org/2002/07/owl#Nothing"), null, (RDFNode)null);
        model2.removeAll(inf.createResource("http://www.w3.org/2002/07/owl#bottomDataProperty"), null, (RDFNode)null);

        return model2;
    }

    private void loadResources(Model model, String docs, Model tesaurus) throws ParserConfigurationException, IOException, SAXException {
        String[] pathnames;
        File f = new File(docs);
        pathnames = f.list();
        int cont = 0;
        for (String pathname : pathnames) {
            Document document = new Document(docs + "/" + pathname);
            document.insertInModel(model, tesaurus, NS);
           /* cont++;
            if(cont > 10) break;*/
        }

    }

    //  args: -rdf -skos -owl -docs
    public static void main (String[] args) throws ParserConfigurationException, IOException, SAXException {
        new SemanticGenerator(args);
    }
}
