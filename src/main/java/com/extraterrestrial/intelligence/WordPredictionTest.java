package com.extraterrestrial.intelligence;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import com.extraterrestrial.intelligence.model.*;
import com.extraterrestrial.intelligence.repository.CSVDatasetRepository;
import com.extraterrestrial.intelligence.repository.DatasetRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Word prediction test - shows predictions when user types words
 */
public class WordPredictionTest {

    // Word prediction maps
    private Map<String, List<String>> wordPredictionMap;
    private Map<String, List<String>> taggedWordPredictionMap;
    
    public WordPredictionTest() {
        System.out.println("Loading dataset...");
        
        // Load dataset
        DatasetRepository repository = new CSVDatasetRepository();
        List<TaggedSentence> sentences = repository.loadSentences();
        
        if (sentences.isEmpty()) {
            System.out.println("ERROR: No sentences loaded. Check the dataset file.");
            System.exit(1);
        }
        
        System.out.println("Loaded " + sentences.size() + " sentences from dataset");
        
        // Build prediction models
        System.out.println("Building word prediction models...");
        buildPredictionModels(sentences);
        System.out.println("Models built successfully.");
    }
    
    private void buildPredictionModels(List<TaggedSentence> sentences) {
        wordPredictionMap = new HashMap<>();
        taggedWordPredictionMap = new HashMap<>();
        
        // Build word prediction models based on previous words
        for (TaggedSentence sentence : sentences) {
            List<TaggerWord> words = sentence.getWords();
            
            for (int i = 0; i < words.size() - 1; i++) {
                // Store next word prediction based on current word
                String currentWord = words.get(i).getWord().toLowerCase();
                String nextWord = words.get(i + 1).getWord();
                
                wordPredictionMap.putIfAbsent(currentWord, new ArrayList<>());
                wordPredictionMap.get(currentWord).add(nextWord);
                
                // Store next word prediction based on current word+tag
                String currentTaggedWord = currentWord + "/" + words.get(i).getTag();
                
                taggedWordPredictionMap.putIfAbsent(currentTaggedWord, new ArrayList<>());
                taggedWordPredictionMap.get(currentTaggedWord).add(nextWord);
            }
        }
    }
    
    public void start() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n==== Word Prediction Test ====");
        System.out.println("Type a word and press Enter to see predicted next words.");
        System.out.println("Type 'exit' to quit the program.");
        System.out.println("==============================\n");
        
        while (true) {
            System.out.print("\nEnter a word: ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting word prediction test.");
                break;
            }
            
            if (input.isEmpty()) {
                continue;
            }
            
            // Show predictions
            showWordPredictions(input);
        }
        
        scanner.close();
    }
    
    private void showWordPredictions(String word) {
        System.out.println("\nPredictions for \"" + word + "\":");
        
        // Get predictions based on the word alone
        List<String> predictions = getTopPredictions(word.toLowerCase(), wordPredictionMap, 5);
        
        System.out.println("\n1. Next likely words (based on word alone):");
        if (predictions.isEmpty()) {
            System.out.println("   No predictions available");
        } else {
            for (String prediction : predictions) {
                String[] parts = prediction.split(":");
                String predictedWord = parts[0];
                int frequency = Integer.parseInt(parts[1]);
                
                System.out.printf("   %-15s (frequency: %d)\n", predictedWord, frequency);
            }
        }
        
        // Get predictions for different possible tags of the word
        System.out.println("\n2. Next likely words for each possible POS tag:");
        
        // Check each possible tag for this word
        Set<String> possibleTags = new HashSet<>();
        for (String key : taggedWordPredictionMap.keySet()) {
            if (key.toLowerCase().startsWith(word.toLowerCase() + "/")) {
                String tag = key.substring(key.indexOf('/') + 1);
                possibleTags.add(tag);
            }
        }
        
        if (possibleTags.isEmpty()) {
            System.out.println("   No tag-specific predictions available");
        } else {
            for (String tag : possibleTags) {
                String taggedWord = word.toLowerCase() + "/" + tag;
                List<String> taggedPredictions = getTopPredictions(taggedWord, taggedWordPredictionMap, 3);
                
                if (!taggedPredictions.isEmpty()) {
                    System.out.printf("   As %s (%s):\n", tag, getTagDescription(tag));
                    for (String prediction : taggedPredictions) {
                        String[] parts = prediction.split(":");
                        String predictedWord = parts[0];
                        int frequency = Integer.parseInt(parts[1]);
                        
                        System.out.printf("     %-15s (frequency: %d)\n", predictedWord, frequency);
                    }
                }
            }
        }
    }
    
    private List<String> getTopPredictions(String key, Map<String, List<String>> predictionMap, int limit) {
        if (!predictionMap.containsKey(key)) {
            return new ArrayList<>();
        }
        
        // Count frequencies of predicted words
        Map<String, Integer> freqMap = new HashMap<>();
        for (String nextWord : predictionMap.get(key)) {
            freqMap.put(nextWord, freqMap.getOrDefault(nextWord, 0) + 1);
        }
        
        // Convert to list of word:frequency pairs and sort by frequency
        List<String> predictions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : freqMap.entrySet()) {
            predictions.add(entry.getKey() + ":" + entry.getValue());
        }
        
        // Sort by frequency (descending)
        predictions.sort((a, b) -> {
            int freqA = Integer.parseInt(a.split(":")[1]);
            int freqB = Integer.parseInt(b.split(":")[1]);
            return Integer.compare(freqB, freqA);
        });
        
        // Return top N predictions
        return predictions.size() <= limit ? predictions : predictions.subList(0, limit);
    }
    
    private String getTagDescription(String tag) {
        Map<String, String> tagDescriptions = new HashMap<>();
        tagDescriptions.put("CC", "Coordinating conjunction");
        tagDescriptions.put("CD", "Cardinal number");
        tagDescriptions.put("DT", "Determiner");
        tagDescriptions.put("EX", "Existential there");
        tagDescriptions.put("FW", "Foreign word");
        tagDescriptions.put("IN", "Preposition or subordinating conjunction");
        tagDescriptions.put("JJ", "Adjective");
        tagDescriptions.put("JJR", "Adjective, comparative");
        tagDescriptions.put("JJS", "Adjective, superlative");
        tagDescriptions.put("LS", "List item marker");
        tagDescriptions.put("MD", "Modal");
        tagDescriptions.put("NN", "Noun, singular or mass");
        tagDescriptions.put("NNS", "Noun, plural");
        tagDescriptions.put("NNP", "Proper noun, singular");
        tagDescriptions.put("NNPS", "Proper noun, plural");
        tagDescriptions.put("PDT", "Predeterminer");
        tagDescriptions.put("POS", "Possessive ending");
        tagDescriptions.put("PRP", "Personal pronoun");
        tagDescriptions.put("PRP$", "Possessive pronoun");
        tagDescriptions.put("RB", "Adverb");
        tagDescriptions.put("RBR", "Adverb, comparative");
        tagDescriptions.put("RBS", "Adverb, superlative");
        tagDescriptions.put("RP", "Particle");
        tagDescriptions.put("SYM", "Symbol");
        tagDescriptions.put("TO", "to");
        tagDescriptions.put("UH", "Interjection");
        tagDescriptions.put("VB", "Verb, base form");
        tagDescriptions.put("VBD", "Verb, past tense");
        tagDescriptions.put("VBG", "Verb, gerund or present participle");
        tagDescriptions.put("VBN", "Verb, past participle");
        tagDescriptions.put("VBP", "Verb, non-3rd person singular present");
        tagDescriptions.put("VBZ", "Verb, 3rd person singular present");
        tagDescriptions.put("WDT", "Wh-determiner");
        tagDescriptions.put("WP", "Wh-pronoun");
        tagDescriptions.put("WP$", "Possessive wh-pronoun");
        tagDescriptions.put("WRB", "Wh-adverb");
        
        return tagDescriptions.getOrDefault(tag, "Unknown tag");
    }
    
    public static void main(String[] args) {
        try {
            WordPredictionTest test = new WordPredictionTest();
            test.start();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}