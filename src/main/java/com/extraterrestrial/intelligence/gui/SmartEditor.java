package com.extraterrestrial.intelligence.gui;

import com.extraterrestrial.intelligence.model.SemanticModel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import javax.swing.Timer; // Explicitly import Timer from Swing
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * A smart text editor with semantic word predictions
 */
public class SmartEditor extends JFrame {
    // UI Components
    private JTextPane editorPane;
    private JPanel suggestionsPanel;
    private JTextPane statusPane;
    private JButton analyzeButton;
    private List<JButton> predictionButtons;
    
    // Semantic model
    private SemanticModel semanticModel;
    private Timer predictionTimer;
    private static final int PREDICTION_DELAY = 300; // ms
    
    // Colors
    private final Color NAVY_BLUE = new Color(25, 50, 95);
    private final Color ROYAL_BLUE = new Color(65, 105, 225);
    private final Color LIGHT_BLUE = new Color(135, 206, 250);
    private final Color VERY_LIGHT_BLUE = new Color(240, 248, 255);
    private final Color GOLD = new Color(255, 215, 0);
    private final Color WHITE = new Color(255, 255, 255);
    
    public SmartEditor() {
        super("Smart Semantic Editor");
        
        // Initialize UI
        initializeUI();
        
        // Load dataset in a background thread
        loadDatasetInBackground();
        
        // Set up the window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void initializeUI() {
        // Set overall layout
        setLayout(new BorderLayout(10, 10));
        
        // Create header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Main content panel
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.setBackground(VERY_LIGHT_BLUE);
        
        // Editor area
        JPanel editorPanel = createEditorPanel();
        contentPanel.add(editorPanel, BorderLayout.CENTER);
        
        // Suggestions area
        suggestionsPanel = new JPanel();
        suggestionsPanel.setLayout(new BoxLayout(suggestionsPanel, BoxLayout.Y_AXIS));
        suggestionsPanel.setBackground(VERY_LIGHT_BLUE);
        suggestionsPanel.setBorder(createTitledBorder("Predictions"));
        suggestionsPanel.setPreferredSize(new Dimension(300, 0));
        
        // Create prediction buttons
        predictionButtons = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            JButton button = createPredictionButton("Loading...");
            button.setVisible(false);
            predictionButtons.add(button);
        }
        
        JLabel loadingLabel = new JLabel("Loading predictions...");
        loadingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        loadingLabel.setForeground(Color.GRAY);
        loadingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        suggestionsPanel.add(loadingLabel);
        
        contentPanel.add(suggestionsPanel, BorderLayout.EAST);
        
        // Status area
        JPanel statusPanel = createStatusPanel();
        contentPanel.add(statusPanel, BorderLayout.SOUTH);
        
        add(contentPanel, BorderLayout.CENTER);
        
        // Set up prediction timer
        predictionTimer = new Timer(PREDICTION_DELAY, e -> updatePredictions());
        predictionTimer.setRepeats(false);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(NAVY_BLUE);
        panel.setPreferredSize(new Dimension(0, 60));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        // Title
        JLabel titleLabel = new JLabel("Smart Semantic Editor");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(WHITE);
        panel.add(titleLabel, BorderLayout.WEST);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        analyzeButton = new JButton("Analyze Text");
        analyzeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        analyzeButton.setForeground(WHITE);
        analyzeButton.setBackground(ROYAL_BLUE);
        analyzeButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)));
        analyzeButton.setFocusPainted(false);
        analyzeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        analyzeButton.addActionListener(e -> analyzeText());
        analyzeButton.setEnabled(false); // Disabled until model is loaded
        
        buttonPanel.add(analyzeButton);
        panel.add(buttonPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WHITE);
        panel.setBorder(createTitledBorder("Editor"));
        
        editorPane = new JTextPane();
        editorPane.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        editorPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add a document listener to update predictions as user types
        editorPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                restartPredictionTimer();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                restartPredictionTimer();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Plain text components don't fire these events
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WHITE);
        panel.setBorder(createTitledBorder("Status"));
        panel.setPreferredSize(new Dimension(0, 150));
        
        statusPane = new JTextPane();
        statusPane.setEditable(false);
        statusPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        // Set up styles for the status pane
        StyledDocument doc = statusPane.getStyledDocument();
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        
        Style regular = doc.addStyle("regular", defaultStyle);
        StyleConstants.setForeground(regular, Color.BLACK);
        
        Style heading = doc.addStyle("heading", regular);
        StyleConstants.setBold(heading, true);
        StyleConstants.setForeground(heading, NAVY_BLUE);
        
        Style emphasis = doc.addStyle("emphasis", regular);
        StyleConstants.setItalic(emphasis, true);
        StyleConstants.setForeground(emphasis, ROYAL_BLUE);
        
        try {
            doc.insertString(0, "Loading semantic model...\n", doc.getStyle("heading"));
            doc.insertString(doc.getLength(), 
                "This editor provides intelligent word predictions based on semantic relationships between words.",
                doc.getStyle("regular"));
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        
        JScrollPane scrollPane = new JScrollPane(statusPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    private Border createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(NAVY_BLUE, 1, true),
                title, javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), NAVY_BLUE);
    }
    
    private JButton createPredictionButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(NAVY_BLUE);
        button.setBackground(WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LIGHT_BLUE, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
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
                button.setBackground(WHITE);
                button.setForeground(NAVY_BLUE);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(LIGHT_BLUE, 1),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            }
        });
        
        // Add action listener to insert the prediction
        button.addActionListener(e -> {
            String prediction = button.getActionCommand();
            if (prediction != null && !prediction.isEmpty()) {
                insertText(prediction);
            }
        });
        
        return button;
    }
    
    private void loadDatasetInBackground() {
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("Loading dataset...");
                
                // Initialize the semantic model
                semanticModel = new SemanticModel();
                
                // Load the dataset
                publish("Processing dataset...");
                semanticModel.loadDataset("src/main/resources/ner_dataset2.csv");
                
                publish("Building prediction models...");
                // Sleep to simulate longer processing time for UI demo purposes
                Thread.sleep(1000);
                
                publish("Model loaded successfully.");
                return null;
            }
            
            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String message = chunks.get(chunks.size() - 1);
                    try {
                        StyledDocument doc = statusPane.getStyledDocument();
                        doc.remove(0, doc.getLength());
                        doc.insertString(0, message + "\n", doc.getStyle("heading"));
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    
                    // Enable UI elements
                    analyzeButton.setEnabled(true);
                    
                    // Update status
                    StyledDocument doc = statusPane.getStyledDocument();
                    try {
                        doc.remove(0, doc.getLength());
                        doc.insertString(0, "Semantic model loaded successfully!\n", doc.getStyle("heading"));
                        doc.insertString(doc.getLength(), 
                                "Start typing to see predictions. Click on a prediction to insert it.", 
                                doc.getStyle("regular"));
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                    
                    // Update the suggestions panel
                    suggestionsPanel.removeAll();
                    
                    // Add section headers
                    JLabel wordHeader = new JLabel("Word Predictions");
                    wordHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    wordHeader.setForeground(NAVY_BLUE);
                    wordHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
                    suggestionsPanel.add(wordHeader);
                    suggestionsPanel.add(Box.createVerticalStrut(5));
                    
                    // Add prediction buttons
                    for (int i = 0; i < 5; i++) {
                        JButton button = predictionButtons.get(i);
                        button.setText("Type to see predictions");
                        button.setEnabled(false);
                        suggestionsPanel.add(button);
                        suggestionsPanel.add(Box.createVerticalStrut(5));
                        button.setVisible(true);
                    }
                    
                    suggestionsPanel.add(Box.createVerticalStrut(15));
                    
                    // Add phrase header
                    JLabel phraseHeader = new JLabel("Phrase Completions");
                    phraseHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    phraseHeader.setForeground(NAVY_BLUE);
                    phraseHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
                    suggestionsPanel.add(phraseHeader);
                    suggestionsPanel.add(Box.createVerticalStrut(5));
                    
                    // Add phrase prediction buttons
                    for (int i = 5; i < 8; i++) {
                        JButton button = predictionButtons.get(i);
                        button.setText("Type to see phrase completions");
                        button.setEnabled(false);
                        suggestionsPanel.add(button);
                        suggestionsPanel.add(Box.createVerticalStrut(5));
                        button.setVisible(true);
                    }
                    
                    suggestionsPanel.revalidate();
                    suggestionsPanel.repaint();
                    
                    // Set focus to editor
                    editorPane.requestFocusInWindow();
                    
                } catch (Exception e) {
                    StyledDocument doc = statusPane.getStyledDocument();
                    try {
                        doc.remove(0, doc.getLength());
                        doc.insertString(0, "Error loading model: " + e.getMessage() + "\n", doc.getStyle("heading"));
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
    
    private void restartPredictionTimer() {
        // Stop the timer if it's running
        if (predictionTimer.isRunning()) {
            predictionTimer.stop();
        }
        
        // Start the timer to update predictions after a delay
        predictionTimer.start();
    }
    
    private void updatePredictions() {
        if (semanticModel == null) {
            return; // Model not loaded yet
        }
        
        String text = editorPane.getText().trim();
        if (text.isEmpty()) {
            // Disable buttons if no text
            for (JButton button : predictionButtons) {
                button.setText("Type to see predictions");
                button.setEnabled(false);
            }
            return;
        }
        
        // Get predictions from the semantic model
        Map<String, Object> predictions = semanticModel.getPredictions(text);
        
        // Update word prediction buttons
        @SuppressWarnings("unchecked")
        Map<String, String> wordPredictions = (Map<String, String>) predictions.get("wordPredictions");
        if (wordPredictions != null) {
            int i = 0;
            for (Map.Entry<String, String> entry : wordPredictions.entrySet()) {
                if (i < 5) { // First 5 buttons are for word predictions
                    JButton button = predictionButtons.get(i);
                    String word = entry.getKey();
                    String source = entry.getValue();
                    
                    button.setText(word + " (" + source + ")");
                    
                    // Store the full prediction text (original text + prediction)
                    String fullText = text + (text.endsWith(" ") ? "" : " ") + word;
                    button.setActionCommand(fullText);
                    button.setEnabled(true);
                    button.setVisible(true);
                    
                    i++;
                }
            }
            
            // Hide unused buttons
            for (int j = i; j < 5; j++) {
                predictionButtons.get(j).setVisible(false);
            }
        }
        
        // Update phrase prediction buttons
        @SuppressWarnings("unchecked")
        List<String> phrasePredictions = (List<String>) predictions.get("phrasePredictions");
        if (phrasePredictions != null) {
            for (int i = 0; i < Math.min(phrasePredictions.size(), 3); i++) {
                JButton button = predictionButtons.get(i + 5); // Start from index 5
                String phrase = phrasePredictions.get(i);
                
                button.setText(phrase);
                button.setActionCommand(phrase);
                button.setEnabled(true);
                button.setVisible(true);
            }
            
            // Hide unused buttons
            for (int i = phrasePredictions.size(); i < 3; i++) {
                predictionButtons.get(i + 5).setVisible(false);
            }
        }
    }
    
    private void insertText(String text) {
        // Replace the text in the editor
        editorPane.setText(text);
        
        // Move caret to the end
        editorPane.setCaretPosition(text.length());
        
        // Set focus back to editor
        editorPane.requestFocusInWindow();
        
        // Update predictions after a short delay
        restartPredictionTimer();
    }
    
    private void analyzeText() {
        String text = editorPane.getText().trim();
        if (text.isEmpty() || semanticModel == null) {
            return;
        }
        
        // Get the current text and tokenize it
        String[] tokens = text.split("\\s+");
        
        // Update status pane with analysis
        StyledDocument doc = statusPane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            doc.insertString(0, "Text Analysis\n\n", doc.getStyle("heading"));
            
            doc.insertString(doc.getLength(), "Words: ", doc.getStyle("emphasis"));
            doc.insertString(doc.getLength(), tokens.length + "\n", doc.getStyle("regular"));
            
            // Analyze word by word
            doc.insertString(doc.getLength(), "\nWord-by-word analysis:\n", doc.getStyle("heading"));
            
            for (String token : tokens) {
                doc.insertString(doc.getLength(), token + ": ", doc.getStyle("emphasis"));
                
                // Get POS tag if available
                String pos = "Unknown";
                if (semanticModel != null) {
                    // Just a placeholder since we're not implementing full analysis
                    doc.insertString(doc.getLength(), pos + "\n", doc.getStyle("regular"));
                } else {
                    doc.insertString(doc.getLength(), "Model not loaded\n", doc.getStyle("regular"));
                }
            }
            
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        // Use system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create and show the application
        SwingUtilities.invokeLater(() -> new SmartEditor());
    }
}