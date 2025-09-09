package ir;

import java.util.*;

public class PostingList {
    private final List<Posting> postings = new ArrayList<>();
    // skip pointers come mappa: indice -> indiceSaltato
    private final Map<Integer, Integer> skips = new HashMap<>();

    public void add(Posting p) { postings.add(p); }
    public void sortByDocId() { postings.sort(Comparator.comparingInt(a -> a.docID)); }
    public int size() { return postings.size(); }
    public boolean isEmpty() { return postings.isEmpty(); }
    public Posting get(int i) { return postings.get(i); }
    public int df() { return postings.size(); }
    public Integer skipFrom(int i) { return skips.get(i); }

    public void computeSkips() {
        skips.clear();
        int L = postings.size();
        if (L < 4) return;
        int span = (int)Math.floor(Math.sqrt(L));
        for (int i = 0; i + span < L; i += span) {
            skips.put(i, i + span);
        }
    }

    public List<Integer> docIds() {
        List<Integer> ids = new ArrayList<>(postings.size());
        for (Posting p : postings) ids.add(p.docID);
        return ids;
    }
}
