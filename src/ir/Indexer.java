package ir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Costruisce l'inverted index con:
 * - Porter Stemmer
 * - Stop list (file)
 * - Stop words per frequenza (top %)
 * - Posting list ordinate + skip pointers (span ≈ ⌊√L⌋)
 *
 * Output atteso (salvati da IndexIO):
 *   - index.dict
 *   - docs.map
 *   - collection.freq
 */
public class Indexer {
    public static final Path STOPLIST_PATH = Paths.get("stoplist.txt");
    public static final Path STOP_BY_FREQ_CACHE = Paths.get("stop_by_freq.cache");

    // Indice: term -> posting list
    Map<String, PostingList> index = new HashMap<>();
    // Tabella documenti: docID -> path
    Map<Integer, String> docTable = new HashMap<>();
    // Frequenze globali di collezione: term -> cf
    Map<String, Long> collectionFreq = new HashMap<>();

    private final StopWords stopWords;
    private final Tokenizer tokenizer = new Tokenizer();
    private final PorterStemmer stemmer = new PorterStemmer();

    /**
     * @param stoplistPath          percorso della stop list (una parola per riga, già stemmata o no)
     * @param topPercentAsStopWords percentuale (es. 1.0 significa top 1% per cf diventa stop)
     */
    public Indexer(Path stoplistPath, double topPercentAsStopWords) throws IOException {
        this.stopWords = new StopWords(stoplistPath, topPercentAsStopWords);
    }

    /**
     * Costruisce l'indice percorrendo ricorsivamente la cartella dataset.
     */
    public void build(Path datasetDir) throws IOException {
        // --- Pass 0: CF (collection frequency) per determinare le stop words per soglia ---
        List<Path> files = new ArrayList<>();
        try (var stream = Files.walk(datasetDir)) {
            stream.filter(Files::isRegularFile).forEach(files::add);
        }

        for (Path p : files) {
            String text = Files.readString(p);
            List<String> toks = tokenizer.tokenize(text);
            for (String tok : toks) {
                String t = stemmer.stem(tok.toLowerCase(Locale.ROOT));
                // incrementa cf
                Long old = collectionFreq.get(t);
                if (old == null) collectionFreq.put(t, 1L);
                else collectionFreq.put(t, old + 1L);
            }
        }

        // Calcola l'insieme di stop words per frequenza (e salva cache)
        stopWords.computeStopByFrequency(collectionFreq);

        // --- Pass 1: costruzione indice vero e proprio ---
        int docID = 0;
        for (Path p : files) {
            String text = Files.readString(p);
            docTable.put(docID, p.toString());

            Map<String, Posting> local = new HashMap<>(); // accumula tf/posizioni per questo documento
            int pos = 0;

            List<String> toks = tokenizer.tokenize(text);
            for (String tok : toks) {
                String t = stemmer.stem(tok.toLowerCase(Locale.ROOT));

                // filtri stop (lista + frequenza)
                if (stopWords.isStop(t)) {
                    pos++;
                    continue;
                }

                // ---- NIENTE lambda: no computeIfAbsent con cattura di docID ----
                Posting posting = local.get(t);
                if (posting == null) {
                    posting = new Posting(docID);
                    local.put(t, posting);
                }
                posting.tf++;
                posting.addPosition(pos);

                pos++;
            }

            // unisci nel dizionario globale
            for (Map.Entry<String, Posting> e : local.entrySet()) {
                String term = e.getKey();
                Posting docPosting = e.getValue();

                PostingList pl = index.get(term);
                if (pl == null) {
                    pl = new PostingList();
                    index.put(term, pl);
                }
                pl.add(docPosting);
            }

            docID++;
        }

        // Ordina posting list per docID, calcola df e skip pointers
        for (PostingList pl : index.values()) {
            pl.sortByDocId();
            pl.computeSkips(); // span ≈ ⌊√L⌋
        }
    }
}
