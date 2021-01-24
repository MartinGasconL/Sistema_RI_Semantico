package com.grupo202.searcher;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.*;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.query.text.TextIndexConfig;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb2.TDB2Factory;

import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;


import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SemanticSearcher {


    SemanticSearcher(String[] args) throws IOException {
        String rdf = null;
        String infoNeeds = null;
        String output = null;

        for(int i=0;i<args.length;i++) {
            if ("-rdf".equals(args[i])) {
                rdf = args[i+1];
                i++;
            } else if ("-infoNeeds".equals(args[i])) {
                infoNeeds = args[i+1];
                i++;
            } else if ("-output".equals(args[i])) {
                output = args[i+1];;
            }
        }

        if (rdf == null || infoNeeds == null || output == null) {
            System.err.println("Error al suministrar los argumentos ");
            System.exit(1);
        }

        Dataset ds = indexFiles();

        // cargamos el fichero deseado y lo almacenamos en el repositorio indexado
        ds.begin(ReadWrite.WRITE) ;
        RDFDataMgr.read(ds.getDefaultModel(), rdf) ;
        ds.commit();
        ds.end();


        BufferedReader in = new BufferedReader(new FileReader(infoNeeds));
        String line;
        PrintWriter out = new PrintWriter(new FileOutputStream(output));

        //Estoy interesado en trabajos académicos sobre Bioinformática (también conocida como Biología Computacional,
        // Bioinformatics o Computational Biology) o Filogenética (Phylogenetics), publicados entre 2010 y 2018


        /*List<String> results = executeQuery(ds, q, "105-5");
        writeResults(out, results);*/

        while((line = in.readLine())!=null) {
            String[] infoNeed = line.split("\t");
            List<String> results = executeQuery(ds, infoNeed[1], infoNeed[0]);
            writeResults(out, results);
        }
        out.close();

    }

    private List<String> executeQuery(Dataset ds, String q, String infoNeed){
        List<String> retval = new ArrayList<>();
        Query query = QueryFactory.create(q);
        ds.begin(ReadWrite.READ);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ds)) {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext(); ) {
                QuerySolution soln = results.nextSolution();
                System.out.println(soln);
                Resource doc = soln.getResource("x");
                retval.add(doc.getURI().replace("http://www.grupo202.com/model#", infoNeed + "\t"));
            }
        }
        ds.end();
        return retval;
    }
    private void writeResults(PrintWriter out, List<String> results){
        for ( String result : results) {
            out.println(result);
        }
    }
    private Dataset indexFiles() throws IOException {
        EntityDefinition entDef = new EntityDefinition("uri", "tema",
                ResourceFactory.createProperty("http://www.grupo202.com/model#","tema"));
        entDef.set("descripcion", ResourceFactory.createProperty("http://www.grupo202.com/model#","descripcion").asNode());
        entDef.set("prefLabel", ResourceFactory.createProperty("http://www.w3.org/2004/02/skos/core#","prefLabel").asNode());
        entDef.set("documento", ResourceFactory.createProperty("http://www.grupo202.com/model#","documento").asNode());
        entDef.set("titulo", ResourceFactory.createProperty("http://www.grupo202.com/model#","titulo").asNode());
        entDef.set("tutor", ResourceFactory.createProperty("http://www.grupo202.com/model#","tutor").asNode());
        entDef.set("autor", ResourceFactory.createProperty("http://www.grupo202.com/model#","autor").asNode());
        entDef.set("publicador", ResourceFactory.createProperty("http://www.grupo202.com/model#","publicador").asNode());
        entDef.set("fecha", ResourceFactory.createProperty("http://www.grupo202.com/model#","fecha").asNode());
        entDef.set("idioma", ResourceFactory.createProperty("http://www.grupo202.com/model#","idioma").asNode());

        TextIndexConfig config = new TextIndexConfig(entDef);
        config.setAnalyzer(new SpanishAnalyzer());
        config.setQueryAnalyzer(new SpanishAnalyzer());
        config.setMultilingualSupport(true);


        FileUtils.deleteDirectory(new File("repositorio"));
        Dataset ds1 = TDB2Factory.connectDataset("repositorio/tdb2");
        Directory dir =  new MMapDirectory(Paths.get("./repositorio/lucene"));
        return TextDatasetFactory.createLucene(ds1, dir, config) ;
    }

    //java SemanticSearcher -rdf <rdfPath> -infoNeeds <infoNeedsFile> -output <resultsFile>
    public static void main (String[] args) throws IOException {
        new SemanticSearcher(args);

    }
}
