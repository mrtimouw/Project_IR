package ir;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {

    // Precompiled pattern: only a-z sequences
    private static final Pattern WORD = Pattern.compile("[a-z]+");

    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();

        // 1) Normalize to NFD (base char + diacritics)
        String norm = Normalizer.normalize(text, Normalizer.Form.NFD);
        // 2) Remove diacritics (Mn)
        norm = norm.replaceAll("\\p{M}+", "");

        // 3) Handle special cases not fixed by NFD
        //    Using Unicode escapes to stay ASCII-safe under any source encoding.
       norm = norm
        .replace("\u00DF", "ss")   // U+00DF (LATIN SMALL LETTER SHARP S) -> ss
        .replace("\u00C6", "AE")   // U+00C6 (LATIN CAPITAL LETTER AE) -> AE
        .replace("\u00E6", "ae")   // U+00E6 (LATIN SMALL LETTER AE) -> ae
        .replace("\u0152", "OE")   // U+0152 (LATIN CAPITAL LIGATURE OE) -> OE
        .replace("\u0153", "oe")   // U+0153 (LATIN SMALL LIGATURE OE) -> oe
        .replace("\u00D8", "O")    // U+00D8 (LATIN CAPITAL LETTER O WITH STROKE) -> O
        .replace("\u00F8", "o")    // U+00F8 (LATIN SMALL LETTER O WITH STROKE) -> o
        .replace("\u00D0", "D")    // U+00D0 (LATIN CAPITAL LETTER ETH) -> D
        .replace("\u00F0", "d")    // U+00F0 (LATIN SMALL LETTER ETH) -> d
        .replace("\u00DE", "TH")   // U+00DE (LATIN CAPITAL LETTER THORN) -> TH
        .replace("\u00FE", "th");  // U+00FE (LATIN SMALL LETTER THORN) -> th

        // 4) Lowercase (locale neutral)
        norm = norm.toLowerCase(Locale.ROOT);

        // 5) Extract only [a-z]+ tokens
        Matcher m = WORD.matcher(norm);
        List<String> out = new ArrayList<>();
        while (m.find()) {
            String tok = m.group();
            // Optional: drop 1-char tokens to reduce noise
            if (tok.length() >= 2) out.add(tok);
        }
        return out;
    }
}
