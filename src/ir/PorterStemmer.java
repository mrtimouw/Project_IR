package ir;

/**
 * Implementazione leggera del Porter Stemmer (regole base).
 * Nota: sufficiente per il progetto; non usa librerie esterne.
 * (Ispirato alle regole originali di Porter; semplificato).
 */
public class PorterStemmer {
    public String stem(String s) {
        if (s == null || s.length() < 3) return s;
        s = step1a(s);
        s = step1b(s);
        s = step1c(s);
        s = step2(s);
        s = step3(s);
        s = step4(s);
        s = step5a(s);
        s = step5b(s);
        return s;
    }

    // ----- helpers -----
    private boolean isVowel(char ch) {
        return "aeiou".indexOf(ch) >= 0;
    }
    private boolean hasVowel(String s) {
        for (char c : s.toCharArray()) if (isVowel(c)) return true;
        return false;
    }
    private int measure(String s) {
        // m = #VC sequences
        boolean prevVowel = false; int m = 0;
        for (char c : s.toCharArray()) {
            boolean v = isVowel(c);
            if (!prevVowel && v) prevVowel = true;
            else if (prevVowel && !v) { m++; prevVowel = false; }
        }
        return m;
    }
    private boolean ends(String s, String suf) { return s.endsWith(suf); }
    private String replaceEnd(String s, String suf, String rep) {
        return s.substring(0, s.length()-suf.length()) + rep;
    }
    private boolean cvc(String s) {
        if (s.length() < 3) return false;
        char c1 = s.charAt(s.length()-3), c2 = s.charAt(s.length()-2), c3 = s.charAt(s.length()-1);
        return !isVowel(c1) && isVowel(c2) && !isVowel(c3) && "wxy".indexOf(c3) < 0;
    }

    // ----- steps (semplificati) -----
    private String step1a(String s) {
        if (ends(s,"sses")) return replaceEnd(s,"sses","ss");
        if (ends(s,"ies")) return replaceEnd(s,"ies","i");
        if (ends(s,"ss")) return s;
        if (ends(s,"s")) return s.substring(0, s.length()-1);
        return s;
    }
    private String step1b(String s) {
        if (ends(s,"eed")) {
            String stem = s.substring(0, s.length()-3);
            if (measure(stem) > 0) return replaceEnd(s,"eed","ee");
            else return s;
        }
        boolean flag = false;
        if (ends(s,"ed")) {
            String stem = s.substring(0, s.length()-2);
            if (hasVowel(stem)) { s = stem; flag = true; }
        } else if (ends(s,"ing")) {
            String stem = s.substring(0, s.length()-3);
            if (hasVowel(stem)) { s = stem; flag = true; }
        }
        if (flag) {
            if (ends(s,"at")||ends(s,"bl")||ends(s,"iz")) return s+"e";
            if (doubleConsonant(s)) return s.substring(0, s.length()-1);
            if (measure(s)==1 && cvc(s)) return s+"e";
        }
        return s;
    }
    private boolean doubleConsonant(String s) {
        if (s.length()<2) return false;
        char a=s.charAt(s.length()-1), b=s.charAt(s.length()-2);
        return a==b && !isVowel(a);
    }
    private String step1c(String s) {
        if (ends(s,"y")) {
            String stem = s.substring(0, s.length()-1);
            if (hasVowel(stem)) return stem+"i";
        }
        return s;
    }
    private String step2(String s) {
        String[][] map = {
            {"ational","ate"},{"tional","tion"},{"enci","ence"},{"anci","ance"},
            {"izer","ize"},{"abli","able"},{"alli","al"},{"entli","ent"},{"eli","e"},
            {"ousli","ous"},{"ization","ize"},{"ation","ate"},{"ator","ate"},{"alism","al"},
            {"iveness","ive"},{"fulness","ful"},{"ousness","ous"},{"aliti","al"},
            {"iviti","ive"},{"biliti","ble"}
        };
        for (String[] r : map) {
            if (ends(s,r[0])) {
                String stem = s.substring(0, s.length()-r[0].length());
                if (measure(stem)>0) return stem + r[1];
                return s;
            }
        }
        return s;
    }
    private String step3(String s) {
        String[][] map = {
            {"icate","ic"},{"ative",""},{"alize","al"},{"iciti","ic"},{"ical","ic"},{"ful",""},{"ness",""}
        };
        for (String[] r : map) {
            if (ends(s,r[0])) {
                String stem = s.substring(0, s.length()-r[0].length());
                if (measure(stem)>0) return stem + r[1];
                return s;
            }
        }
        return s;
    }
    private String step4(String s) {
        String[] suf = {"al","ance","ence","er","ic","able","ible","ant","ement","ment","ent",
                        "ion","ou","ism","ate","iti","ous","ive","ize"};
        for (String x : suf) {
            if (ends(s,x)) {
                String stem = s.substring(0, s.length()-x.length());
                if (measure(stem)>1) {
                    if (x.equals("ion")) {
                        if (stem.endsWith("s")||stem.endsWith("t")) return stem;
                        else return s;
                    }
                    return stem;
                }
                return s;
            }
        }
        return s;
    }
    private String step5a(String s) {
        if (ends(s,"e")) {
            String stem = s.substring(0, s.length()-1);
            if (measure(stem)>1 || (measure(stem)==1 && !cvc(stem))) return stem;
        }
        return s;
    }
    private String step5b(String s) {
        if (measure(s)>1 && doubleConsonant(s) && s.endsWith("l")) return s.substring(0,s.length()-1);
        return s;
    }
}
