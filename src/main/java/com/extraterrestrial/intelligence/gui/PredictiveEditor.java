package com.extraterrestrial.intelligence.gui;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import com.extraterrestrial.intelligence.model.*;
import com.extraterrestrial.intelligence.repository.CSVDatasetRepository;
import com.extraterrestrial.intelligence.repository.DatasetRepository;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple text editor with word prediction capabilities
 */
public class PredictiveEditor extends JFrame {
    private JTextArea textArea;
    private JList<String> suggestionList;
    private JTextArea tagResultArea;
    private JButton analyzeButton;
    
    // Prediction maps
    private Map<String, List<String>> wordPredictionMap;
    private Map<String, List<String>> prefixCompletionMap;
    
    // POS Taggers
    private Tagger unigramTagger;
    private Tagger bigramTagger;
    private Tagger trigramTagger;
    private Tagger quadgramTagger;
    
    // Tag descriptions
    private static final Map<String, String> POS_TAG_DESCRIPTIONS = new HashMap<>() {{
        put("CC", "Coordinating conjunction");
        put("CD", "Cardinal number");
        put("DT", "Determiner");
        put("EX", "Existential there");
        put("FW", "Foreign word");
        put("IN", "Preposition or subordinating conjunction");
        put("JJ", "Adjective");
        put("JJR", "Adjective, comparative");
        put("JJS", "Adjective, superlative");
        put("LS", "List item marker");
        put("MD", "Modal");
        put("NN", "Noun, singular or mass");
        put("NNS", "Noun, plural");
        put("NNP", "Proper noun, singular");
        put("NNPS", "Proper noun, plural");
        put("PDT", "Predeterminer");
        put("POS", "Possessive ending");
        put("PRP", "Personal pronoun");
        put("PRP$", "Possessive pronoun");
        put("RB", "Adverb");
        put("RBR", "Adverb, comparative");
        put("RBS", "Adverb, superlative");
        put("RP", "Particle");
        put("SYM", "Symbol");
        put("TO", "to");
        put("UH", "Interjection");
        put("VB", "Verb, base form");
        put("VBD", "Verb, past tense");
        put("VBG", "Verb, gerund or present participle");
        put("VBN", "Verb, past participle");
        put("VBP", "Verb, non-3rd person singular present");
        put("VBZ", "Verb, 3rd person singular present");
        put("WDT", "Wh-determiner");
        put("WP", "Wh-pronoun");
        put("WP$", "Possessive wh-pronoun");
        put("WRB", "Wh-adverb");
    }};
    
    public PredictiveEditor() {
        super("Predictive Text Editor with POS Tagging");
        
        // Initialize models before UI
        initModels();
        
        // Set up the UI
        initUI();
        
        // Add listeners
        addListeners();
        
        // Display the window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void initModels() {
        try {
            System.out.println("Loading models...");
            
            // Load dataset
            DatasetRepository repository = new CSVDatasetRepository();
            List<TaggedSentence> sentences = repository.loadSentences();
            
            if (sentences.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                        "No sentences loaded. Please check the dataset file.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            
            System.out.println("Loaded " + sentences.size() + " sentences");
            
            // Split into training (90%) and testing (10%) sets
            int splitPoint = (int) (sentences.size() * 0.9);
            List<TaggedSentence> trainingSentences = sentences.subList(0, splitPoint);
            
            // Build prediction models
            buildPredictionModels(trainingSentences);
            
            // Initialize and train taggers
            System.out.println("Training POS taggers...");
            DefaultTagger defaultTagger = new DefaultTagger();
            
            unigramTagger = new UniGramTagger(defaultTagger);
            unigramTagger.train(trainingSentences);
            
            bigramTagger = new BiGramTagger(unigramTagger);
            bigramTagger.train(trainingSentences);
            
            trigramTagger = new TriGramTagger(bigramTagger);
            trigramTagger.train(trainingSentences);
            
            quadgramTagger = new QuadGramTagger(trigramTagger);
            quadgramTagger.train(trainingSentences);
            
            System.out.println("Models loaded successfully");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                    "Error initializing models: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private void initUI() {
        // Main layout
        setLayout(new BorderLayout());
        
        // Text area for input
        textArea = new JTextArea(15, 50);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane textScrollPane = new JScrollPane(textArea);
        
        // Suggestion list
        suggestionList = new JList<>();
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setVisibleRowCount(5);
        suggestionList.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane suggestScrollPane = new JScrollPane(suggestionList);
        
        // Analysis results area
        tagResultArea = new JTextArea(10, 50);
        tagResultArea.setEditable(false);
        tagResultArea.setLineWrap(true);
        tagResultArea.setWrapStyleWord(true);
        tagResultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane resultScrollPane = new JScrollPane(tagResultArea);
        
        // Analyze button
        analyzeButton = new JButton("Analyze Text");
        
        // Create panels and add components
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(textScrollPane, BorderLayout.CENTER);
        topPanel.add(suggestScrollPane, BorderLayout.SOUTH);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(analyzeButton, BorderLayout.NORTH);
        bottomPanel.add(resultScrollPane, BorderLayout.CENTER);
        
        // Add panels to the frame
        add(topPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Set minimum size
        setMinimumSize(new Dimension(800, 600));
    }
    
    private void addListeners() {
        // Text area document listener for suggestions
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateSuggestions());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateSuggestions());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Plain text doesn't trigger this
            }
        });
        
        // Key listener for the text area
        textArea.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                // Not used
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    e.consume(); // Prevent tab from moving focus
                    if (!suggestionList.isSelectionEmpty()) {
                        insertSelectedSuggestion();
                    }
                }
                else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    // Move to suggestion list
                    if (suggestionList.getModel().getSize() > 0) {
                        suggestionList.setSelectedIndex(0);
                        suggestionList.requestFocusInWindow();
                        e.consume();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // Not used
            }
        });
        
        // Key listener for suggestion list
        suggestionList.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                // Not used
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
                    insertSelectedSuggestion();
                    e.consume();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // Not used
            }
        });
        
        // Double-click on suggestion
        suggestionList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    insertSelectedSuggestion();
                }
            }
        });
        
        // Analyze button action
        analyzeButton.addActionListener((ActionEvent e) -> analyzeText());
    }
    
    private void updateSuggestions() {
        try {
            // Get current text and caret position
            String text = textArea.getText();
            int caretPos = textArea.getCaretPosition();
            
            // Find the word being typed
            int wordStart = findWordStart(text, caretPos);
            String currentWord = text.substring(wordStart, caretPos);
            
            // Get prior word for context
            String prevWord = findPreviousWord(text, wordStart);
            
            DefaultListModel<String> model = new DefaultListModel<>();
            
            // If word is being typed, show word completions
            if (!currentWord.isEmpty()) {
                List<String> completions = getCompletions(currentWord);
                for (String completion : completions) {
                    model.addElement(completion);
                }
            } 
            // If word just ended (space/punctuation), show next word predictions
            else if (!prevWord.isEmpty()) {
                List<String> predictions = getNextWordPredictions(prevWord);
                for (String prediction : predictions) {
                    model.addElement(prediction);
                }
            }
            
            suggestionList.setModel(model);
            
        } catch (Exception e) {
            // Just log the error but don't break the UI
            e.printStackTrace();
        }
    }
    
    private void insertSelectedSuggestion() {
        if (suggestionList.isSelectionEmpty()) {
            return;
        }
        
        try {
            String selected = suggestionList.getSelectedValue();
            String text = textArea.getText();
            int caretPos = textArea.getCaretPosition();
            
            // Find the start of the current word
            int wordStart = findWordStart(text, caretPos);
            
            // Replace the partial word with the selected suggestion
            textArea.getDocument().remove(wordStart, caretPos - wordStart);
            textArea.getDocument().insertString(wordStart, selected + " ", null);
            
            // Return focus to text area
            textArea.requestFocusInWindow();
            
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private void analyzeText() {
        String text = textArea.getText().trim();
        if (text.isEmpty()) {
            tagResultArea.setText("Please enter some text to analyze.");
            return;
        }
        
        // Tokenize the text
        String[] tokens = text.split("\\s+");
        List<String> words = new ArrayList<>(Arrays.asList(tokens));
        
        // Create a TaggedSentence
        TaggedSentence sentence = new TaggedSentence();
        for (String word : words) {
            sentence.addWord(new TaggerWord(word, ""));
        }
        
        // Analyze with all taggers
        StringBuilder result = new StringBuilder();
        result.append("===== POS Tagging Analysis =====\n\n");
        result.append(String.format("%-15s %-8s %-8s %-8s %-8s %-30s\n", 
                "Word", "1-gram", "2-gram", "3-gram", "4-gram", "Description"));
        result.append("-".repeat(80)).append("\n");
        
        List<TaggerWord> taggedWords = sentence.getWords();
        for (int i = 0; i < taggedWords.size(); i++) {
            String word = taggedWords.get(i).getWord();
            
            String unigramTag = unigramTagger.predict(taggedWords, i);
            String bigramTag = bigramTagger.predict(taggedWords, i);
            String trigramTag = trigramTagger.predict(taggedWords, i);
            String quadgramTag = quadgramTagger.predict(taggedWords, i);
            
            // Set the tag for subsequent predictions
            taggedWords.get(i).setTag(quadgramTag);
            
            String description = POS_TAG_DESCRIPTIONS.getOrDefault(quadgramTag, "");
            
            result.append(String.format("%-15s %-8s %-8s %-8s %-8s %-30s\n", 
                    word, unigramTag, bigramTag, trigramTag, quadgramTag, description));
        }
        
        // Complete tagged sentence
        result.append("\n===== Tagged Sentence =====\n");
        String formattedSentence = taggedWords.stream()
                .map(w -> w.getWord() + "/" + w.getTag())
                .collect(Collectors.joining(" "));
        result.append(formattedSentence).append("\n");
        
        tagResultArea.setText(result.toString());
    }
    
    private int findWordStart(String text, int caretPos) {
        int pos = caretPos - 1;
        // Move backwards until we hit a word boundary
        while (pos >= 0) {
            if (Character.isWhitespace(text.charAt(pos)) || 
                    !Character.isLetterOrDigit(text.charAt(pos))) {
                break;
            }
            pos--;
        }
        return pos + 1;
    }
    
    private String findPreviousWord(String text, int currentWordStart) {
        if (currentWordStart <= 0) {
            return "";
        }
        
        int end = currentWordStart - 1;
        // Skip white space
        while (end >= 0 && Character.isWhitespace(text.charAt(end))) {
            end--;
        }
        
        if (end < 0) {
            return "";
        }
        
        int start = end;
        // Find the start of the previous word
        while (start >= 0) {
            if (Character.isWhitespace(text.charAt(start)) ||
                    !Character.isLetterOrDigit(text.charAt(start))) {
                break;
            }
            start--;
        }
        
        return text.substring(start + 1, end + 1);
    }
    
    private void buildPredictionModels(List<TaggedSentence> sentences) {
        System.out.println("Building prediction models...");
        
        wordPredictionMap = new HashMap<>();
        prefixCompletionMap = new HashMap<>();
        
        // Build word prediction map (next word predictions)
        for (TaggedSentence sentence : sentences) {
            List<TaggerWord> words = sentence.getWords();
            
            for (int i = 0; i < words.size() - 1; i++) {
                String currentWord = words.get(i).getWord().toLowerCase();
                String nextWord = words.get(i + 1).getWord();
                
                wordPredictionMap.putIfAbsent(currentWord, new ArrayList<>());
                wordPredictionMap.get(currentWord).add(nextWord);
            }
        }
        
        // Build prefix completion map
        Set<String> uniqueWords = new HashSet<>();
        for (TaggedSentence sentence : sentences) {
            for (TaggerWord word : sentence.getWords()) {
                uniqueWords.add(word.getWord().toLowerCase());
            }
        }
        
        // For each word, add it to all possible prefixes
        for (String word : uniqueWords) {
            for (int i = 1; i <= word.length(); i++) {
                String prefix = word.substring(0, i);
                prefixCompletionMap.putIfAbsent(prefix, new ArrayList<>());
                prefixCompletionMap.get(prefix).add(word);
            }
        }
        
        System.out.println("Prediction models built.");
    }
    
    private List<String> getCompletions(String prefix) {
        // Get all possible completions for the prefix
        if (!prefixCompletionMap.containsKey(prefix.toLowerCase())) {
            return new ArrayList<>();
        }
        
        List<String> completions = prefixCompletionMap.get(prefix.toLowerCase());
        
        // Sort by frequency and length
        Map<String, Integer> completionFreq = new HashMap<>();
        for (String completion : completions) {
            completionFreq.put(completion, completionFreq.getOrDefault(completion, 0) + 1);
        }
        
        // Sort by frequency and then by length (shorter words first)
        List<String> sortedCompletions = new ArrayList<>(completionFreq.keySet());
        sortedCompletions.sort((a, b) -> {
            int freqComp = Integer.compare(completionFreq.get(b), completionFreq.get(a));
            if (freqComp != 0) {
                return freqComp;
            }
            return Integer.compare(a.length(), b.length());
        });
        
        // Limit to top 10
        return sortedCompletions.size() <= 10 ? 
                sortedCompletions : sortedCompletions.subList(0, 10);
    }
    
    private List<String> getNextWordPredictions(String word) {
        if (!wordPredictionMap.containsKey(word.toLowerCase())) {
            return new ArrayList<>();
        }
        
        List<String> predictions = wordPredictionMap.get(word.toLowerCase());
        
        // Count frequencies
        Map<String, Integer> predictionFreq = new HashMap<>();
        for (String prediction : predictions) {
            predictionFreq.put(prediction, predictionFreq.getOrDefault(prediction, 0) + 1);
        }
        
        // Sort by frequency
        List<String> sortedPredictions = new ArrayList<>(predictionFreq.keySet());
        sortedPredictions.sort((a, b) -> 
                Integer.compare(predictionFreq.get(b), predictionFreq.get(a)));
        
        // Limit to top 10
        return sortedPredictions.size() <= 10 ? 
                sortedPredictions : sortedPredictions.subList(0, 10);
    }
    
    public static void main(String[] args) {
        // Use system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Start the UI
        SwingUtilities.invokeLater(() -> new PredictiveEditor());
    }
}