package ir;

import java.util.*;

public class Posting {
    public final int docID;
    public int tf = 0;
    public List<Integer> positions = new ArrayList<>();

    public Posting(int docID) { this.docID = docID; }

    public void addPosition(int pos) { positions.add(pos); }
}
