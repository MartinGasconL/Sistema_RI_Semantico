package com.grupo202.searcher;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.*;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.query.text.TextIndexConfig;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.RAMDirectory;

import java.io.*;
import java.nio.file.Paths;

public class SemanticSearcher {

    private final float DESCRIPTION_INFONEED_WEIGHT = 5;
    private final float TITLE_INFONEED_WEIGHT =  4;
    private final float SUBJECT_INFONEED_WEIGHT = 3;
    private final float TYPE_WEIGHT = 10;
    private final float LOCATION_WEIGHT = 5;
    private final float NAME_CREATOR_WEIGHT = 10;
    private final float NAME_CONTRIBUTOR_WEIGHT = 10;
    private final float DESCRIPTION_NAME_WEIGHT = 10;
    private final float BEGIN_DATE_WEIGHT = 10;
    private final float END_DATE_WEIGHT = 10;


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

        //definimos la configuración del repositorio indexado



        Dataset ds = indexFiles();

        // cargamos el fichero deseado y lo almacenamos en el repositorio indexado
        ds.begin(ReadWrite.WRITE) ;
        RDFDataMgr.read(ds.getDefaultModel(), rdf) ;
        ds.commit();
        ds.end();


        String q = "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>"
                + "prefix g: <http://www.grupo202.com/model/> "
                + "prefix text: <http://jena.apache.org/text#> "
                + "Select distinct ?x where { "
                + "{(?x ?score1) text:query (g:descripcion 'caciquismo dictadura represión política' )} "
                + "UNION {(?x ?score2) text:query (g:titulo 'caciquismo dictadura represión política Huesca España siglo XX' )} "
                + "optional {(?x ?score3) text:query (g:Tema 'huesca')}  "
                + "optional {(?x ?score4) text:query (skos:prefLabel 'dictadura')}  "
                + "optional {(?x ?score5) text:query (skos:prefLabel 'represión política')}  "
            //    + "optional {(?x ?score6) text:query (g:descripcion 'Huesca España siglo XX' )} "
                + "optional {(?x ?score7) text:query (skos:prefLabel 'españa' )} "
                + "optional {(?x ?score8) text:query (skos:prefLabel 'historia' )} "
                + "bind (coalesce(?score1," + 15 + ")"
                     + "+coalesce(?score2," + TITLE_INFONEED_WEIGHT + ") "
                     + "+coalesce(?score3," + 4 + ")"
                     + "+coalesce(?score4," + 6 + ")"
                     + "+coalesce(?score5," + 6 + ")"
                     + "+coalesce(?score6," + 5 + ")"
                     + "+coalesce(?score7," + 5 + ")"
                     + "+coalesce(?score8," + 3 + ")"
                     + " as ?scoretot) "
                + "} ORDER BY DESC(?scoretot)";

       // q = QUERY_105;

        Query query = QueryFactory.create(q) ;
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ds)) {
            ResultSet results = qexec.execSelect() ;
            PrintWriter out = new PrintWriter(new FileOutputStream(output));
            for ( ; results.hasNext() ; ) {
                QuerySolution soln = results.nextSolution() ;
                System.out.println(soln);
                Resource doc = soln.getResource("x");
                System.out.println(doc.getURI().replace("http://www.grupo202.com/model/", "105-5\t"));
                out.println(doc.getURI().replace("http://www.grupo202.com/model/", "105-5\t"));
            }
            out.close();
        }








    }

    private Dataset indexFiles() throws IOException {
        EntityDefinition entDef = new EntityDefinition("uri", "tema",
                ResourceFactory.createProperty("http://www.grupo202.com/model/","tema"));

        entDef.set("titulo", ResourceFactory.createProperty("http://www.grupo202.com/model/","titulo").asNode());
        entDef.set("descripcion", ResourceFactory.createProperty("http://www.grupo202.com/model/","descripcion").asNode());
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
