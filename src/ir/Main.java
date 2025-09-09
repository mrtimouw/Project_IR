package ir;

import java.nio.file.*;
import java.util.*;

public class Main {
    // Percorsi dove salviamo l'indice
    static final Path INDEX_DICT = Paths.get("index.dict");
    static final Path DOCS_MAP   = Paths.get("docs.map");
    static final Path CF_FILE    = Paths.get("collection.freq");

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            usage();
            return;
        }
        switch (args[0]) {
            case "index" -> {
                if (args.length < 3) { System.err.println("Uso: index <cartella_dataset> <stoplist.txt> [freqThresholdPercent=1.0]"); return; }
                Path dataset = Paths.get(args[1]);
                Path stoplist = Paths.get(args[2]);
                double thrPct = (args.length >= 4) ? Double.parseDouble(args[3]) : 1.0; // top 1% come stop words
                Indexer indexer = new Indexer(stoplist, thrPct);
                indexer.build(dataset);
                IndexIO.save(indexer.index, INDEX_DICT);
                IndexIO.saveDocs(indexer.docTable, DOCS_MAP);
                IndexIO.saveCF(indexer.collectionFreq, CF_FILE);
                System.out.println("Indicizzazione completata. Termini nel dizionario: " + indexer.index.size());
            }
            case "search" -> {
                if (args.length < 3) { System.err.println("Uso: search <and|or> \"query...\""); return; }
                String mode = args[1].toLowerCase(Locale.ROOT);
                String q = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                Map<String, PostingList> index = IndexIO.load(INDEX_DICT);
                Map<Integer, String> docs = IndexIO.loadDocs(DOCS_MAP);
                StopWords sw = StopWords.loadPrecomputed(CF_FILE, Indexer.STOPLIST_PATH, Indexer.STOP_BY_FREQ_CACHE);
                Retriever r = new Retriever(index, docs, sw);
                List<Integer> results = switch (mode) {
                    case "and" -> r.searchAnd(q);
                    case "or"  -> r.searchOr(q);
                    default -> { System.err.println("Modo non valido, usa and|or"); yield List.of(); }
                };
                if (results.isEmpty()) {
                    System.out.println("Nessun risultato.");
                } else {
                    System.out.println("DocIDs (" + results.size() + "):");
                    for (int id : results) System.out.println(id + "\t" + docs.get(id));
                }
            }
            default -> usage();
        }
    }

    private static void usage() {
        System.out.println("""
            Comandi:
              index <cartella_dataset> <stoplist.txt> [freqThresholdPercent=1.0]
              search <and|or> "query..."
            """);
    }
}
