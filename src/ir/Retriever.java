package ir;

import java.util.*;

public class Retriever {
    private final Map<String, PostingList> index;
    @SuppressWarnings("unused")
    private final Map<Integer, String> docs;
    private final Tokenizer tokenizer = new Tokenizer();
    private final PorterStemmer stemmer = new PorterStemmer();
    private final StopWords stopWords;

    public Retriever(Map<String, PostingList> index, Map<Integer, String> docs, StopWords sw) {
        this.index = index;
        this.docs = docs;
        this.stopWords = sw;
    }

    public List<Integer> searchAnd(String query) {
        List<String> terms = normalizedTerms(query);
        if (terms.isEmpty()) return List.of();
        List<PostingList> lists = new ArrayList<>();
        for (String t : terms) {
            PostingList pl = index.get(t);
            if (pl == null) return List.of(); // un termine non presente -> AND vuoto
            lists.add(pl);
        }
        lists.sort(Comparator.comparingInt(a -> a.df())); // crescente per df
        PostingList acc = lists.get(0);
        for (int i = 1; i < lists.size(); i++) {
            acc = andWithSkips(acc, lists.get(i));
            if (acc.isEmpty()) break;
        }
        return acc.docIds();
    }

    public List<Integer> searchOr(String query) {
        List<String> terms = normalizedTerms(query);
        if (terms.isEmpty()) return List.of();
        List<PostingList> lists = new ArrayList<>();
        for (String t : terms) {
            PostingList pl = index.get(t);
            if (pl != null) lists.add(pl);
        }
        if (lists.isEmpty()) return List.of();
        PostingList acc = lists.get(0);
        for (int i = 1; i < lists.size(); i++) {
            acc = orMerge(acc, lists.get(i));
        }
        return acc.docIds();
    }

    private List<String> normalizedTerms(String q) {
        List<String> out = new ArrayList<>();
        for (String tok : tokenizer.tokenize(q)) {
            String t = stemmer.stem(tok.toLowerCase(Locale.ROOT));
            if (!stopWords.isStop(t)) out.add(t);
        }
        return out;
    }

    // AND con skip pointers
    private PostingList andWithSkips(PostingList A, PostingList B) {
        List<Posting> res = new ArrayList<>();
        int i = 0, j = 0;
        while (i < A.size() && j < B.size()) {
            int a = A.get(i).docID, b = B.get(j).docID;
            if (a == b) { res.add(new Posting(a)); i++; j++; }
            else if (a < b) {
                Integer skipTo = A.skipFrom(i);
                if (skipTo != null && A.get(skipTo).docID <= b) i = skipTo;
                else i++;
            } else {
                Integer skipTo = B.skipFrom(j);
                if (skipTo != null && B.get(skipTo).docID <= a) j = skipTo;
                else j++;
            }
        }
        PostingList R = new PostingList();
        for (Posting p : res) R.add(p);
        R.sortByDocId();
        R.computeSkips();
        return R;
    }

    // OR merge lineare
    private PostingList orMerge(PostingList A, PostingList B) {
        List<Posting> res = new ArrayList<>();
        int i = 0, j = 0;
        while (i < A.size() || j < B.size()) {
            if (j >= B.size() || (i < A.size() && A.get(i).docID <= B.get(j).docID)) {
                pushIfNew(res, A.get(i).docID);
                i++;
            } else {
                pushIfNew(res, B.get(j).docID);
                j++;
            }
        }
        PostingList R = new PostingList();
        for (Posting p : res) R.add(p);
        R.sortByDocId();
        R.computeSkips();
        return R;
    }

    private void pushIfNew(List<Posting> res, int docId) {
        if (res.isEmpty() || res.get(res.size()-1).docID != docId) res.add(new Posting(docId));
    }
}
