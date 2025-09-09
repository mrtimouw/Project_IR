package ir;

import java.util.*;

public class Tokenizer {
    public List<String> tokenize(String text) {
        // split su non-lettere; conserva lettere accentate base
        String[] raw = text.split("[^\\p{IsAlphabetic}]+");
        List<String> out = new ArrayList<>();
        for (String s : raw) if (!s.isBlank()) out.add(s);
        return out;
    }
}
