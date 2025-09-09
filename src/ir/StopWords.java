package ir;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class StopWords {
    private final Set<String> stopList = new HashSet<>();
    private final Set<String> stopByFreq = new HashSet<>();
    private final double topPercent; // es. 1.0 => top 1%

    public StopWords(Path stoplistPath, double topPercent) throws IOException {
        this.topPercent = topPercent;
        if (Files.exists(stoplistPath)) {
            for (String line : Files.readAllLines(stoplistPath)) {
                String w = line.strip().toLowerCase(Locale.ROOT);
                if (!w.isBlank()) stopList.add(w);
            }
        }
    }

    public boolean isStop(String term) {
        return stopList.contains(term) || stopByFreq.contains(term);
    }

    public void computeStopByFrequency(Map<String, Long> cf) throws IOException {
        if (cf.isEmpty()) return;
        // ordina per frequenza decrescente e prendi topPercent
        int k = (int)Math.ceil(cf.size() * (topPercent / 100.0));
        if (k <= 0) return;
        List<Map.Entry<String, Long>> sorted = cf.entrySet().stream()
            .sorted((a,b)->Long.compare(b.getValue(), a.getValue()))
            .collect(Collectors.toList());
        for (int i=0; i<Math.min(k, sorted.size()); i++) stopByFreq.add(sorted.get(i).getKey());
        // cache su file (facoltativo)
        Files.write(Indexer.STOP_BY_FREQ_CACHE, String.join("\n", stopByFreq).getBytes());
    }

    public static StopWords loadPrecomputed(Path cfFile, Path stoplistPath, Path cache) throws IOException {
        StopWords sw = new StopWords(stoplistPath, 0.0);
        if (Files.exists(cache)) {
            for (String line : Files.readAllLines(cache)) {
                String w = line.strip();
                if (!w.isBlank()) sw.stopByFreq.add(w);
            }
        }
        return sw;
    }
}
