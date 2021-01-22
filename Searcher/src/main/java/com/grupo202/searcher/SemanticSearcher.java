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

        //definimos la configuración del repositorio indexado



        Dataset ds = indexFiles();

        // cargamos el fichero deseado y lo almacenamos en el repositorio indexado
        ds.begin(ReadWrite.WRITE) ;
        RDFDataMgr.read(ds.getDefaultModel(), rdf) ;
        ds.commit();
        ds.end();

        String q = " PREFIX skos:<http://www.w3.org/2004/02/skos/core#>" +
                    "PREFIX g: <http://www.grupo202.com/model#> "
                  + "PREFIX text: <http://jena.apache.org/text#> "
                  + "SELECT DISTINCT ?x WHERE { "
                      + "?x g:tema ?y ."
                      +  " {"
                           // +  "{(?y ?score0) text:query (skos:prefLabel 'caciquismo' )} "
                       //     +  "  {(?y ?score1) text:query (skos:prefLabel 'represion política' )} "
                        //    +  " UNION {(?y ?score2) text:query (skos:prefLabel 'dictadura' )} "
                         //   +  "  UNION{(?y ?score3) text:query (skos:prefLabel 'españa' )} "
                      +  " } "
                      + "UNION { "
                         // + "{(?x ?score4) text:query (g:titulo 'siglo XX')} "
                          + "{(?x ?score5) text:query (g:titulo 'dictadura represión política')} "
                          + "UNION {(?x ?score6) text:query (g:descripcion 'caciquismo dictadura represión política huesca españa siglo XX')} "
                         // + "UNION {(?x ?score7) text:query (g:descripcion 'huesca españa')} "
                        //  + "UNION {(?x ?score8) text:query (g:tema 'huesca')} "
                      + "}"
                      + "bind (coalesce(?score6,10) +coalesce(?score5,10) +  " +
                        "coalesce(?score2,10)+ coalesce(?score1,5) " +
                   //     "coalesce(?score4,6)+ coalesce(?score5,15) +" +
                     //   "coalesce(?score6,10)+ coalesce(?score7,6) + " +
                    //    "coalesce(?score8,10)" +
                "as ?scoretot) "
                  + "} ORDER BY DESC(?scoretot)";

        q = " PREFIX skos:<http://www.w3.org/2004/02/skos/core#>" +
                "PREFIX g: <http://www.grupo202.com/model#> "
                + "PREFIX text: <http://jena.apache.org/text#> "
                + "SELECT DISTINCT ?x WHERE { "
                + "?x g:tema ?y ."
                + "{"
                    + "{(?x ?score0) text:query (g:titulo 'dictadura dictador dictatorship')}"
                    + " UNION {(?x ?score1) text:query (g:titulo 'Franco franquismo represión')}"
                    + " UNION {(?x ?score2) text:query (g:titulo 'caciquismo')}"
                    + " UNION {(?x ?score3) text:query (g:descripcion 'dictadura dictatorship')}"
                    + " UNION {(?x ?score4) text:query (g:descripcion 'caciquismo')}"
                + "}"
                +  "OPTIONAL {"
                    +  "{(?y ?score13) text:query (skos:prefLabel 'caciquismo' )} "
                    +  " UNION {(?y ?score5) text:query (skos:prefLabel 'represion política' )} "
                    +  " UNION {(?y ?score6) text:query (skos:prefLabel 'dictadura' )} "
                    +  " UNION {(?y ?score7) text:query (skos:prefLabel 'españa' )} "
                    +  " UNION {(?x ?score8) text:query (g:titulo 'siglo XX')} "
                    +  " UNION {(?x ?score10) text:query (g:descripcion 'Huesca')} "
                    +  " UNION {(?x ?score11) text:query (g:descripcion 'Primo de Rivera')} "
                    +  " UNION {(?x ?score12) text:query (g:tema 'huesca')} "
                + "}"
                + "bind (coalesce(?score0,5) + "
                + "coalesce(?score1,10)+ "
                + "coalesce(?score2,10)+ "
                + "coalesce(?score3,15)+ "
                + "coalesce(?score4,7)+ "
                + "coalesce(?score5,15)+ "
                + "coalesce(?score6,15)+ "
                + "coalesce(?score7,0)+ "
                + "coalesce(?score8,10)+ "
                + "coalesce(?score10,0)+ "
                + "coalesce(?score11,0)+ "
                + "coalesce(?score12,0)+ "
                + "coalesce(?score13,15) "

                + "as ?scoretot) "
                + "} ORDER BY DESC(?scoretot)";

        Query query = QueryFactory.create(q) ;
        ds.begin(ReadWrite.READ);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ds)) {
            ResultSet results = qexec.execSelect() ;
            PrintWriter out = new PrintWriter(new FileOutputStream(output));
            for ( ; results.hasNext() ; ) {
                QuerySolution soln = results.nextSolution() ;
                System.out.println(soln);
                Resource doc = soln.getResource("x");
                System.out.println(doc.getURI().replace("http://www.grupo202.com/model#", "105-5\t"));
                out.println(doc.getURI().replace("http://www.grupo202.com/model#", "105-5\t"));
            }
            out.close();
        }
        ds.end();







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
