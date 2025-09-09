package ir;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Formato semplice line-based (senza dipendenze):
 * index.dict:
 *   term|df|docID:tf,docID:tf,...
 * docs.map:
 *   docID\tpath
 * collection.freq:
 *   term\tcf
 */
public class IndexIO {
    public static void save(Map<String, PostingList> index, Path file) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            for (Map.Entry<String, PostingList> e : index.entrySet()) {
                String term = e.getKey();
                PostingList pl = e.getValue();
                StringBuilder sb = new StringBuilder();
                sb.append(term).append("|").append(pl.df()).append("|");
                List<String> parts = new ArrayList<>();
                for (int i=0;i<pl.size();i++) {
                    Posting p = pl.get(i);
                    parts.add(p.docID + ":" + p.tf);
                }
                sb.append(String.join(",", parts));
                w.write(sb.toString());
                w.newLine();
            }
        }
    }

    public static Map<String, PostingList> load(Path file) throws IOException {
        Map<String, PostingList> index = new HashMap<>();
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank()) continue;
            String[] a = line.split("\\|", -1);
            String term = a[0];
            PostingList pl = new PostingList();
            if (a.length >= 3 && !a[2].isBlank()) {
                String[] pairs = a[2].split(",");
                for (String pair : pairs) {
                    String[] p = pair.split(":");
                    int doc = Integer.parseInt(p[0]);
                    int tf = Integer.parseInt(p[1]);
                    Posting posting = new Posting(doc);
                    posting.tf = tf;
                    pl.add(posting);
                }
            }
            pl.sortByDocId();
            pl.computeSkips();
            index.put(term, pl);
        }
        return index;
    }

    public static void saveDocs(Map<Integer, String> docs, Path file) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            for (Map.Entry<Integer, String> e : docs.entrySet()) {
                w.write(e.getKey() + "\t" + e.getValue());
                w.newLine();
            }
        }
    }

    public static Map<Integer, String> loadDocs(Path file) throws IOException {
        Map<Integer, String> docs = new HashMap<>();
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank()) continue;
            int tab = line.indexOf('\t');
            int id = Integer.parseInt(line.substring(0, tab));
            String path = line.substring(tab+1);
            docs.put(id, path);
        }
        return docs;
    }

    public static void saveCF(Map<String, Long> cf, Path file) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            for (Map.Entry<String, Long> e : cf.entrySet()) {
                w.write(e.getKey() + "\t" + e.getValue());
                w.newLine();
            }
        }
    }
}
