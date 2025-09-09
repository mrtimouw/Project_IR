package ir;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.List;
import java.util.*;
import java.io.IOException;

public class Gui extends JFrame {
    private final JTextField queryField = new JTextField();
    private final JRadioButton andBtn = new JRadioButton("AND", true);
    private final JRadioButton orBtn  = new JRadioButton("OR");
    private final JButton searchBtn = new JButton("Cerca");
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> resultsList = new JList<>(listModel);
    private final JLabel statusLabel = new JLabel("Pronto");
    private final JButton reloadBtn = new JButton("Ricarica indice");

    // Percorsi di default (stessi usati nel Main)
    private final Path INDEX_DICT = Paths.get("index.dict");
    private final Path DOCS_MAP   = Paths.get("docs.map");
    private final Path CF_FILE    = Paths.get("collection.freq");
    private final Path STOPLIST   = Paths.get("stoplist.txt");
    private final Path FREQ_CACHE = Indexer.STOP_BY_FREQ_CACHE;

    private Map<String, PostingList> index;
    private Map<Integer, String> docs;
    private StopWords stopWords;
    private Retriever retriever;

    public Gui() {
        super("IR Mini Search");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12,12));

        // TOP: query + AND/OR + search
        JPanel top = new JPanel(new BorderLayout(8,8));
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ButtonGroup bg = new ButtonGroup(); bg.add(andBtn); bg.add(orBtn);
        modePanel.add(andBtn); modePanel.add(orBtn);

        JPanel leftTop = new JPanel(new BorderLayout(8,8));
        leftTop.add(new JLabel("Query:"), BorderLayout.WEST);
        leftTop.add(queryField, BorderLayout.CENTER);
        top.add(leftTop, BorderLayout.CENTER);

        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTop.add(modePanel);
        rightTop.add(searchBtn);
        rightTop.add(reloadBtn);
        top.add(rightTop, BorderLayout.EAST);

        // CENTER: results
        resultsList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(resultsList);

        // STATUS
        JPanel status = new JPanel(new BorderLayout());
        status.setBorder(new EmptyBorder(4,8,4,8));
        status.add(statusLabel, BorderLayout.WEST);

        // PADDING
        ((JComponent)getContentPane()).setBorder(new EmptyBorder(12,12,12,12));
        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        // Actions
        searchBtn.addActionListener(e -> doSearch());
        queryField.addActionListener(e -> doSearch()); // invio
        reloadBtn.addActionListener(e -> loadIndexAsync());

        resultsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==2) openSelectedDoc();
            }
        });

        // carica indice allo start
        SwingUtilities.invokeLater(this::loadIndexAsync);
    }

    private void loadIndexAsync() {
        listModel.clear();
        statusLabel.setText("Carico indice...");
        searchBtn.setEnabled(false);
        reloadBtn.setEnabled(false);

        new SwingWorker<Void, Void>() {
            private String message = "OK";
            @Override protected Void doInBackground() {
                try {
                    if (!Files.exists(INDEX_DICT) || !Files.exists(DOCS_MAP)) {
                        message = "index.dict/docs.map mancanti. Esegui l'indicizzazione da CLI.";
                        return null;
                    }
                    index = IndexIO.load(INDEX_DICT);
                    docs  = IndexIO.loadDocs(DOCS_MAP);
                    stopWords = StopWords.loadPrecomputed(CF_FILE, STOPLIST, FREQ_CACHE);
                    retriever = new Retriever(index, docs, stopWords);
                } catch (IOException ex) {
                    message = "Errore caricamento: " + ex.getMessage();
                }
                return null;
            }
            @Override protected void done() {
                statusLabel.setText("Indice: " + (index==null ? "—" : index.size()+" termini") + " | " + message);
                searchBtn.setEnabled(true);
                reloadBtn.setEnabled(true);
            }
        }.execute();
    }

    private void doSearch() {
        String q = queryField.getText().trim();
        if (q.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Inserisci una query.", "Attenzione", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (retriever == null) {
            JOptionPane.showMessageDialog(this, "Indice non caricato.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }
        listModel.clear();
        searchBtn.setEnabled(false);
        reloadBtn.setEnabled(false);
        long t0 = System.nanoTime();

        new SwingWorker<List<Integer>, Void>() {
            @Override protected List<Integer> doInBackground() {
                return andBtn.isSelected() ? retriever.searchAnd(q) : retriever.searchOr(q);
            }
            @Override protected void done() {
                try {
                    List<Integer> ids = get();
                    long ms = (System.nanoTime()-t0)/1_000_000;
                    if (ids.isEmpty()) {
                        listModel.addElement("Nessun risultato.");
                        statusLabel.setText("0 risultati in " + ms + " ms");
                    } else {
                        for (int id : ids) {
                            String path = docs.getOrDefault(id, "<unknown>");
                            listModel.addElement(String.format("%-6d  %s", id, path));
                        }
                        statusLabel.setText(ids.size() + " risultati in " + ms + " ms");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(Gui.this, "Errore ricerca: " + ex.getMessage(),
                            "Errore", JOptionPane.ERROR_MESSAGE);
                } finally {
                    searchBtn.setEnabled(true);
                    reloadBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void openSelectedDoc() {
        int idx = resultsList.getSelectedIndex();
        if (idx < 0) return;
        String line = listModel.get(idx);
        // formato: "docId  path"
        int i = line.indexOf("  ");
        if (i < 0) return;
        String path = line.substring(i).trim();
        try {
            Desktop.getDesktop().open(Paths.get(path).toFile());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Impossibile aprire il file:\n" + path,
                    "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Gui().setVisible(true));
    }
}
