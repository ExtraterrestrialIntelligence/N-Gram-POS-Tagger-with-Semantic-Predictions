package com.extraterrestrial.intelligence.gui;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import com.extraterrestrial.intelligence.model.*;
import com.extraterrestrial.intelligence.repository.CSVDatasetRepository;
import com.extraterrestrial.intelligence.repository.DatasetRepository;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A modern, beautiful text editor with multi-word prediction capabilities
 * Navy blue color scheme and elegant UI
 */
public class ModernPredictiveEditor extends JFrame {
    // UI Components
    private JTextPane editorPane;
    private JPanel suggestionPanel;
    private JTextPane analysisPane;
    private JButton analyzeButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private java.util.List<JButton> suggestionButtons;
    
    // Color scheme
    private final Color NAVY_BLUE = new Color(25, 50, 95);
    private final Color ROYAL_BLUE = new Color(65, 105, 225);
    private final Color LIGHT_BLUE = new Color(135, 206, 250);
    private final Color VERY_LIGHT_BLUE = new Color(240, 248, 255);
    private final Color GOLD_ACCENT = new Color(255, 215, 0);
    private final Color TEXT_COLOR = new Color(240, 240, 240);
    
    // n-gram word sequence prediction maps
    private Map<String, List<String>> unigramWordMap;
    private Map<String, List<String>> bigramWordMap;
    private Map<String, List<String>> trigramWordMap;
    private Map<String, List<String>> quadgramWordMap;
    
    // POS taggers
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
    
    // Maximum number of suggestion buttons
    private final int MAX_SUGGESTIONS = 5;
    
    public ModernPredictiveEditor() {
        super("Smart Editor");
        
        // Setup UI components
        setupUI();
        
        // Start loading models in a background thread
        startModelLoading();
        
        // Display the frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        
        // Set global font
        setUIFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        // Create a modern and stylish header panel
        JPanel headerPanel = createHeaderPanel();
        
        // Main content panel with editor and suggestions
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(VERY_LIGHT_BLUE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Editor panel
        JPanel editorPanel = createEditorPanel();
        
        // Suggestion panel
        suggestionPanel = new JPanel();
        suggestionPanel.setLayout(new BoxLayout(suggestionPanel, BoxLayout.Y_AXIS));
        suggestionPanel.setBackground(VERY_LIGHT_BLUE);
        suggestionPanel.setPreferredSize(new Dimension(300, 400));
        suggestionPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(NAVY_BLUE, 1, true),
                "Predictions", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), NAVY_BLUE));
        
        // Create suggestion buttons list but don't add them yet
        // We'll add them dynamically in updatePredictions()
        suggestionButtons = new ArrayList<>();
        for (int i = 0; i < MAX_SUGGESTIONS; i++) {
            JButton button = createStylishButton("Suggestion " + (i+1));
            button.setVisible(false);
            suggestionButtons.add(button);
        }
        
        // Add a placeholder message
        JLabel placeholderLabel = new JLabel("Loading predictions...");
        placeholderLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        placeholderLabel.setForeground(Color.GRAY);
        placeholderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        suggestionPanel.add(placeholderLabel);
        
        // Analysis panel
        JPanel analysisPanel = createAnalysisPanel();
        
        // Status panel
        JPanel statusPanel = createStatusPanel();
        
        // Add components to content panel
        contentPanel.add(editorPanel, BorderLayout.CENTER);
        contentPanel.add(suggestionPanel, BorderLayout.EAST);
        contentPanel.add(analysisPanel, BorderLayout.SOUTH);
        
        // Add all components to the frame
        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(NAVY_BLUE);
        headerPanel.setPreferredSize(new Dimension(getWidth(), 70));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        JLabel titleLabel = new JLabel("Advanced Predictive Text Editor");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(TEXT_COLOR);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        analyzeButton = new JButton("Analyze Text");
        analyzeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        analyzeButton.setForeground(Color.WHITE);
        analyzeButton.setBackground(ROYAL_BLUE);
        analyzeButton.setFocusPainted(false);
        analyzeButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD_ACCENT, 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)));
        analyzeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        analyzeButton.setEnabled(false); // Disabled until models are loaded
        
        analyzeButton.addActionListener(e -> analyzeText());
        
        buttonPanel.add(analyzeButton);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private JPanel createEditorPanel() {
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBackground(VERY_LIGHT_BLUE);
        editorPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(NAVY_BLUE, 1, true),
                "Editor", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), NAVY_BLUE));
        
        editorPane = new JTextPane();
        editorPane.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        editorPane.setBackground(Color.WHITE);
        editorPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add document listener for real-time predictions
        editorPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePredictions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePredictions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Plain text doesn't trigger this
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        editorPanel.add(scrollPane, BorderLayout.CENTER);
        
        return editorPanel;
    }
    
    private JPanel createAnalysisPanel() {
        JPanel analysisPanel = new JPanel(new BorderLayout());
        analysisPanel.setBackground(VERY_LIGHT_BLUE);
        analysisPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(NAVY_BLUE, 1, true),
                "POS Tagging Analysis", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), NAVY_BLUE));
        
        analysisPane = new JTextPane();
        analysisPane.setEditable(false);
        analysisPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        analysisPane.setBackground(Color.WHITE);
        analysisPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Set up styled document for colored output
        StyledDocument doc = analysisPane.getStyledDocument();
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        
        Style regular = doc.addStyle("regular", defaultStyle);
        StyleConstants.setFontFamily(regular, "Segoe UI");
        
        Style heading = doc.addStyle("heading", regular);
        StyleConstants.setBold(heading, true);
        StyleConstants.setForeground(heading, NAVY_BLUE);
        StyleConstants.setFontSize(heading, 16);
        
        Style tag = doc.addStyle("tag", regular);
        StyleConstants.setBold(tag, true);
        StyleConstants.setForeground(tag, ROYAL_BLUE);
        
        Style word = doc.addStyle("word", regular);
        StyleConstants.setForeground(word, new Color(60, 60, 60));
        
        JScrollPane scrollPane = new JScrollPane(analysisPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(getWidth(), 150));
        
        analysisPanel.add(scrollPane, BorderLayout.CENTER);
        
        return analysisPanel;
    }
    
    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout(10, 0));
        statusPanel.setBackground(NAVY_BLUE);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusPanel.setPreferredSize(new Dimension(getWidth(), 30));
        
        statusLabel = new JLabel("Loading models...");
        statusLabel.setForeground(TEXT_COLOR);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        progressBar.setString("Loading language models...");
        progressBar.setForeground(GOLD_ACCENT);
        progressBar.setBackground(new Color(40, 70, 120));
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        progressBar.setPreferredSize(new Dimension(200, 15));
        
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.EAST);
        
        return statusPanel;
    }
    
    private JButton createStylishButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(NAVY_BLUE);
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LIGHT_BLUE, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(VERY_LIGHT_BLUE);
                button.setForeground(ROYAL_BLUE);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ROYAL_BLUE, 1),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.WHITE);
                button.setForeground(NAVY_BLUE);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(LIGHT_BLUE, 1),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            }
        });
        
        // When clicked, insert the text
        button.addActionListener(e -> {
            // Get the text to insert - either from actionCommand or button text
            String textToInsert = button.getActionCommand();
            if (textToInsert == null || textToInsert.isEmpty()) {
                textToInsert = text;
            }
            
            if (!textToInsert.isEmpty()) {
                insertText(textToInsert);
                
                // Update predictions after insertion
                SwingUtilities.invokeLater(() -> updatePredictions());
            }
        });
        
        return button;
    }
    
    private void setUIFont(Font font) {
        UIManager.put("Button.font", font);
        UIManager.put("Label.font", font);
        UIManager.put("Panel.font", font);
        UIManager.put("TextPane.font", font);
        UIManager.put("ScrollPane.font", font);
        UIManager.put("TitledBorder.font", font);
    }
    
    private void startModelLoading() {
        // Disable editor during loading
        editorPane.setEnabled(false);
        
        // Show loading message in analysis pane
        try {
            StyledDocument doc = analysisPane.getStyledDocument();
            doc.remove(0, doc.getLength());
            doc.insertString(0, "Loading language models and taggers...\nPlease wait, this may take a minute.", 
                    analysisPane.getStyle("regular"));
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        
        // Start loading models in a background thread
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("Loading dataset...");
                
                // Load dataset
                DatasetRepository repository = new CSVDatasetRepository();
                List<TaggedSentence> sentences = repository.loadSentences();
                
                if (sentences.isEmpty()) {
                    throw new Exception("No sentences loaded. Check the dataset file.");
                }
                
                publish("Loaded " + sentences.size() + " sentences");
                
                // Split data for training
                int splitPoint = (int) (sentences.size() * 0.9);
                List<TaggedSentence> trainingSentences = sentences.subList(0, splitPoint);
                
                publish("Building word prediction models...");
                buildWordSequenceModels(trainingSentences);
                
                publish("Training POS taggers...");
                trainTaggers(trainingSentences);
                
                return null;
            }
            
            @Override
            protected void process(List<String> chunks) {
                // Update status label with the latest message
                if (!chunks.isEmpty()) {
                    String latestMessage = chunks.get(chunks.size() - 1);
                    statusLabel.setText(latestMessage);
                    progressBar.setString(latestMessage);
                }
            }
            
            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    
                    // Update UI elements
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    progressBar.setString("Ready");
                    statusLabel.setText("Models loaded successfully");
                    
                    // Enable editor and analysis button
                    editorPane.setEnabled(true);
                    analyzeButton.setEnabled(true);
                    
                    // Clear analysis pane
                    StyledDocument doc = analysisPane.getStyledDocument();
                    doc.remove(0, doc.getLength());
                    doc.insertString(0, "Ready! Type text in the editor or click 'Analyze Text'", 
                            analysisPane.getStyle("regular"));
                    
                    // Set focus to editor
                    editorPane.requestFocusInWindow();
                    
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                    progressBar.setString("Error loading models");
                    
                    JOptionPane.showMessageDialog(ModernPredictiveEditor.this,
                            "Error loading models: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
    
    private void updatePredictions() {
        if (unigramWordMap == null || !editorPane.isEnabled()) {
            return; // Models not loaded yet
        }
        
        try {
            String text = editorPane.getText();
            if (text.trim().isEmpty()) {
                clearSuggestions();
                return;
            }
            
            // Clear existing buttons first
            clearSuggestions();
            
            // Make sure suggestionPanel is using the right layout
            suggestionPanel.removeAll();
            suggestionPanel.setLayout(new BoxLayout(suggestionPanel, BoxLayout.Y_AXIS));
            
            // Add section headers and predictions for different n-gram models
            String[] tokens = text.toLowerCase().split("\\s+");
            int numTokens = tokens.length;
            int buttonIndex = 0;
            
            // Add a header for complete phrases
            JLabel completeHeader = new JLabel("Complete Phrases");
            completeHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
            completeHeader.setForeground(NAVY_BLUE);
            completeHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            suggestionPanel.add(completeHeader);
            suggestionPanel.add(Box.createVerticalStrut(5));
            
            // Generate and add complete phrase predictions
            List<String> completePhrases = generatePhrases(text, 3, 3);
            if (!completePhrases.isEmpty()) {
                for (String phrase : completePhrases) {
                    if (buttonIndex < MAX_SUGGESTIONS) {
                        JButton button = createStylishButton(phrase);
                        button.setBackground(VERY_LIGHT_BLUE);
                        suggestionButtons.set(buttonIndex, button);
                        suggestionPanel.add(button);
                        suggestionPanel.add(Box.createVerticalStrut(5));
                        buttonIndex++;
                    }
                }
            } else {
                JLabel noComplete = new JLabel("No complete phrases available");
                noComplete.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                noComplete.setForeground(Color.GRAY);
                noComplete.setAlignmentX(Component.LEFT_ALIGNMENT);
                suggestionPanel.add(noComplete);
            }
            
            suggestionPanel.add(Box.createVerticalStrut(10));
            
            // Add headers and predictions for different n-gram levels
            if (numTokens >= 1) {
                // Header for next word predictions
                JLabel nextWordHeader = new JLabel("Next Word Predictions");
                nextWordHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
                nextWordHeader.setForeground(NAVY_BLUE);
                nextWordHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
                suggestionPanel.add(nextWordHeader);
                suggestionPanel.add(Box.createVerticalStrut(5));
                
                // Get single word predictions from each n-gram model
                Map<String, String> allPredictions = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order
                
                // 4-gram (highest priority)
                if (numTokens >= 4) {
                    String context = tokens[numTokens-4] + " " + tokens[numTokens-3] + " " + 
                            tokens[numTokens-2] + " " + tokens[numTokens-1];
                    List<String> quadPredictions = getTopPredictions(context, quadgramWordMap, 2);
                    for (String word : quadPredictions) {
                        allPredictions.put(word, "4-gram");
                    }
                }
                
                // 3-gram
                if (numTokens >= 3) {
                    String context = tokens[numTokens-3] + " " + tokens[numTokens-2] + " " + tokens[numTokens-1];
                    List<String> triPredictions = getTopPredictions(context, trigramWordMap, 2);
                    for (String word : triPredictions) {
                        allPredictions.putIfAbsent(word, "3-gram");
                    }
                }
                
                // 2-gram
                if (numTokens >= 2) {
                    String context = tokens[numTokens-2] + " " + tokens[numTokens-1];
                    List<String> biPredictions = getTopPredictions(context, bigramWordMap, 2);
                    for (String word : biPredictions) {
                        allPredictions.putIfAbsent(word, "2-gram");
                    }
                }
                
                // 1-gram (lowest priority)
                String context = tokens[numTokens-1];
                List<String> uniPredictions = getTopPredictions(context, unigramWordMap, 2);
                for (String word : uniPredictions) {
                    allPredictions.putIfAbsent(word, "1-gram");
                }
                
                // Add buttons for single word predictions
                if (!allPredictions.isEmpty()) {
                    for (Map.Entry<String, String> entry : allPredictions.entrySet()) {
                        if (buttonIndex < MAX_SUGGESTIONS) {
                            String nextWord = text + (text.endsWith(" ") ? "" : " ") + entry.getKey();
                            String buttonText = entry.getKey() + " (" + entry.getValue() + ")";
                            JButton button = createStylishButton(buttonText);
                            button.setActionCommand(nextWord); // Store the full text to insert
                            suggestionButtons.set(buttonIndex, button);
                            suggestionPanel.add(button);
                            suggestionPanel.add(Box.createVerticalStrut(5));
                            buttonIndex++;
                        }
                    }
                } else {
                    JLabel noWords = new JLabel("No word predictions available");
                    noWords.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                    noWords.setForeground(Color.GRAY);
                    noWords.setAlignmentX(Component.LEFT_ALIGNMENT);
                    suggestionPanel.add(noWords);
                }
            }
            
            // Make sure all buttons are enabled
            for (JButton button : suggestionButtons) {
                if (button != null) {
                    button.setEnabled(true);
                }
            }
            
            // Refresh the panel
            suggestionPanel.revalidate();
            suggestionPanel.repaint();
            
        } catch (Exception e) {
            System.err.println("Error updating predictions: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void analyzeText() {
        String text = editorPane.getText().trim();
        if (text.isEmpty() || quadgramTagger == null) {
            return;
        }
        
        try {
            // Clear analysis pane
            StyledDocument doc = analysisPane.getStyledDocument();
            doc.remove(0, doc.getLength());
            
            // Insert heading
            doc.insertString(0, "POS Tagging Analysis\n\n", analysisPane.getStyle("heading"));
            
            // Tokenize text
            String[] tokens = text.split("\\s+");
            TaggedSentence sentence = new TaggedSentence();
            for (String token : tokens) {
                sentence.addWord(new TaggerWord(token, ""));
            }
            
            // Tag the sentence with all taggers
            List<TaggerWord> words = sentence.getWords();
            
            // Create a table header
            doc.insertString(doc.getLength(), String.format("%-15s %-8s %-8s %-8s %-8s %-30s\n", 
                    "Word", "1-gram", "2-gram", "3-gram", "4-gram", "Description"), 
                    analysisPane.getStyle("heading"));
            doc.insertString(doc.getLength(), "-".repeat(80) + "\n", 
                    analysisPane.getStyle("regular"));
            
            // Process each word
            for (int i = 0; i < words.size(); i++) {
                String word = words.get(i).getWord();
                
                String unigramTag = unigramTagger.predict(words, i);
                String bigramTag = bigramTagger.predict(words, i);
                String trigramTag = trigramTagger.predict(words, i);
                String quadgramTag = quadgramTagger.predict(words, i);
                
                // Set the tag for subsequent predictions
                words.get(i).setTag(quadgramTag);
                
                String description = POS_TAG_DESCRIPTIONS.getOrDefault(quadgramTag, "");
                
                // Insert word
                doc.insertString(doc.getLength(), String.format("%-15s ", word), 
                        analysisPane.getStyle("word"));
                
                // Insert tags
                doc.insertString(doc.getLength(), String.format("%-8s ", unigramTag), 
                        analysisPane.getStyle("tag"));
                doc.insertString(doc.getLength(), String.format("%-8s ", bigramTag), 
                        analysisPane.getStyle("tag"));
                doc.insertString(doc.getLength(), String.format("%-8s ", trigramTag), 
                        analysisPane.getStyle("tag"));
                doc.insertString(doc.getLength(), String.format("%-8s ", quadgramTag), 
                        analysisPane.getStyle("tag"));
                
                // Insert description
                doc.insertString(doc.getLength(), String.format("%-30s\n", description), 
                        analysisPane.getStyle("regular"));
            }
            
            // Insert complete tagged sentence
            doc.insertString(doc.getLength(), "\nComplete Tagged Sentence:\n", 
                    analysisPane.getStyle("heading"));
            
            String formattedSentence = words.stream()
                    .map(w -> w.getWord() + "/" + w.getTag())
                    .collect(Collectors.joining(" "));
            
            doc.insertString(doc.getLength(), formattedSentence, 
                    analysisPane.getStyle("regular"));
            
        } catch (Exception e) {
            System.err.println("Error analyzing text: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void clearSuggestions() {
        for (JButton button : suggestionButtons) {
            button.setVisible(false);
        }
    }
    
    private void insertText(String text) {
        // Replace the entire text with the new text 
        // This works better for complete phrases
        try {
            // Replace the entire text content
            editorPane.setText(text);
            
            // Move caret to the end
            editorPane.setCaretPosition(text.length());
            
            // Set focus back to editor
            editorPane.requestFocusInWindow();
            
            // Log the insertion for debugging
            System.out.println("Inserted text: " + text);
        } catch (Exception e) {
            System.err.println("Error inserting text: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void buildWordSequenceModels(List<TaggedSentence> sentences) {
        unigramWordMap = new HashMap<>();
        bigramWordMap = new HashMap<>();
        trigramWordMap = new HashMap<>();
        quadgramWordMap = new HashMap<>();
        
        // Build n-gram models from the sentences
        for (TaggedSentence sentence : sentences) {
            List<String> words = sentence.getWords().stream()
                    .map(w -> w.getWord().toLowerCase())
                    .collect(Collectors.toList());
            
            // Need at least 4 words for quadgram model
            if (words.size() < 4) continue;
            
            // For each position in the sentence
            for (int i = 0; i < words.size() - 1; i++) {
                // Unigram model (single previous word)
                String w1 = words.get(i);
                unigramWordMap.putIfAbsent(w1, new ArrayList<>());
                if (i < words.size() - 1) {
                    unigramWordMap.get(w1).add(words.get(i + 1));
                }
                
                // Bigram model (two previous words)
                if (i > 0 && i < words.size() - 1) {
                    String w2 = words.get(i-1) + " " + words.get(i);
                    bigramWordMap.putIfAbsent(w2, new ArrayList<>());
                    bigramWordMap.get(w2).add(words.get(i + 1));
                }
                
                // Trigram model (three previous words)
                if (i > 1 && i < words.size() - 1) {
                    String w3 = words.get(i-2) + " " + words.get(i-1) + " " + words.get(i);
                    trigramWordMap.putIfAbsent(w3, new ArrayList<>());
                    trigramWordMap.get(w3).add(words.get(i + 1));
                }
                
                // Quadgram model (four previous words)
                if (i > 2 && i < words.size() - 1) {
                    String w4 = words.get(i-3) + " " + words.get(i-2) + " " + 
                            words.get(i-1) + " " + words.get(i);
                    quadgramWordMap.putIfAbsent(w4, new ArrayList<>());
                    quadgramWordMap.get(w4).add(words.get(i + 1));
                }
            }
        }
    }
    
    private void trainTaggers(List<TaggedSentence> trainingSentences) {
        DefaultTagger defaultTagger = new DefaultTagger();
        
        unigramTagger = new UniGramTagger(defaultTagger);
        unigramTagger.train(trainingSentences);
        
        bigramTagger = new BiGramTagger(unigramTagger);
        bigramTagger.train(trainingSentences);
        
        trigramTagger = new TriGramTagger(bigramTagger);
        trigramTagger.train(trainingSentences);
        
        quadgramTagger = new QuadGramTagger(trigramTagger);
        quadgramTagger.train(trainingSentences);
    }
    
    private List<String> getTopPredictions(String context, Map<String, List<String>> predictionMap, int limit) {
        if (!predictionMap.containsKey(context)) {
            return new ArrayList<>();
        }
        
        Map<String, Integer> countMap = new HashMap<>();
        for (String next : predictionMap.get(context)) {
            countMap.put(next, countMap.getOrDefault(next, 0) + 1);
        }
        
        return countMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    private List<String> generatePhrases(String text, int wordsToAdd, int numPhrases) {
        List<String> phrases = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        
        String[] tokens = text.toLowerCase().split("\\s+");
        if (tokens.length == 0) {
            return phrases;
        }
        
        // Get the base text to extend
        String baseText = text;
        
        // Start with several possible next words
        List<String> nextWords = new ArrayList<>();
        if (tokens.length >= 3) {
            String context = tokens[tokens.length-3] + " " + tokens[tokens.length-2] + " " + tokens[tokens.length-1];
            nextWords.addAll(getTopPredictions(context, trigramWordMap, 3));
        }
        if (tokens.length >= 2) {
            String context = tokens[tokens.length-2] + " " + tokens[tokens.length-1];
            nextWords.addAll(getTopPredictions(context, bigramWordMap, 3));
        }
        
        String context = tokens[tokens.length-1];
        nextWords.addAll(getTopPredictions(context, unigramWordMap, 3));
        
        // Remove duplicates and limit
        nextWords = nextWords.stream().distinct().limit(5).collect(Collectors.toList());
        
        // For each starting word, try to build a phrase
        for (String nextWord : nextWords) {
            String currentPhrase = baseText + (baseText.endsWith(" ") ? "" : " ") + nextWord;
            
            // Now iteratively add more words
            for (int i = 1; i < wordsToAdd; i++) {
                String[] currentTokens = currentPhrase.toLowerCase().split("\\s+");
                int len = currentTokens.length;
                
                // Try each n-gram model in turn, from highest to lowest
                List<String> predictions = new ArrayList<>();
                
                if (len >= 4) {
                    String ctx = currentTokens[len-4] + " " + currentTokens[len-3] + " " + 
                            currentTokens[len-2] + " " + currentTokens[len-1];
                    predictions = getTopPredictions(ctx, quadgramWordMap, 1);
                }
                
                if (predictions.isEmpty() && len >= 3) {
                    String ctx = currentTokens[len-3] + " " + currentTokens[len-2] + " " + currentTokens[len-1];
                    predictions = getTopPredictions(ctx, trigramWordMap, 1);
                }
                
                if (predictions.isEmpty() && len >= 2) {
                    String ctx = currentTokens[len-2] + " " + currentTokens[len-1];
                    predictions = getTopPredictions(ctx, bigramWordMap, 1);
                }
                
                if (predictions.isEmpty() && len >= 1) {
                    String ctx = currentTokens[len-1];
                    predictions = getTopPredictions(ctx, unigramWordMap, 1);
                }
                
                // Add the predicted word if we found one
                if (!predictions.isEmpty()) {
                    currentPhrase += " " + predictions.get(0);
                } else {
                    // No more predictions available
                    break;
                }
            }
            
            // Add the phrase if we haven't seen it before
            if (!seen.contains(currentPhrase) && !currentPhrase.equals(baseText)) {
                phrases.add(currentPhrase);
                seen.add(currentPhrase);
                
                // Stop when we have enough phrases
                if (phrases.size() >= numPhrases) {
                    break;
                }
            }
        }
        
        return phrases;
    }
    
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create and show the GUI
        SwingUtilities.invokeLater(() -> new ModernPredictiveEditor());
    }
}