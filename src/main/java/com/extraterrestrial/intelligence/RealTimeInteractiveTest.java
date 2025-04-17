package com.extraterrestrial.intelligence;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import com.extraterrestrial.intelligence.model.*;
import com.extraterrestrial.intelligence.repository.CSVDatasetRepository;
import com.extraterrestrial.intelligence.repository.DatasetRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Real-time interactive test that shows predictions as the user types each character
 */
public class RealTimeInteractiveTest {

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
    
    // Word completion map - stores most common next words for a given prefix
    private Map<String, List<String>> wordCompletionMap;
    
    private final Tagger unigramTagger;
    private final Tagger bigramTagger;
    private final Tagger trigramTagger;
    private final Tagger quadgramTagger;
    
    public RealTimeInteractiveTest() {
        System.out.println("Loading and training taggers...");
        
        // Load dataset
        DatasetRepository repository = new CSVDatasetRepository();
        List<TaggedSentence> sentences = repository.loadSentences();
        
        if (sentences.isEmpty()) {
            System.out.println("ERROR: No sentences loaded. Check the dataset file.");
            System.exit(1);
        }
        
        System.out.println("Loaded " + sentences.size() + " sentences from dataset");
        
        // Split into training (80%) and testing (20%) sets
        int splitPoint = (int) (sentences.size() * 0.8);
        List<TaggedSentence> trainingSentences = sentences.subList(0, splitPoint);
        
        System.out.println("Training on " + trainingSentences.size() + " sentences");
        
        // Initialize and train taggers
        DefaultTagger defaultTagger = new DefaultTagger();
        
        System.out.println("Training Unigram tagger...");
        unigramTagger = new UniGramTagger(defaultTagger);
        unigramTagger.train(trainingSentences);
        
        System.out.println("Training Bigram tagger...");
        bigramTagger = new BiGramTagger(unigramTagger);
        bigramTagger.train(trainingSentences);
        
        System.out.println("Training Trigram tagger...");
        trigramTagger = new TriGramTagger(bigramTagger);
        trigramTagger.train(trainingSentences);
        
        System.out.println("Training Quadgram tagger...");
        quadgramTagger = new QuadGramTagger(trigramTagger);
        quadgramTagger.train(trainingSentences);
        
        System.out.println("Building word prediction model...");
        buildWordCompletionMap(trainingSentences);
        
        System.out.println("All models trained successfully.");
    }
    
    private void buildWordCompletionMap(List<TaggedSentence> sentences) {
        wordCompletionMap = new HashMap<>();
        
        // Build a map of word prefixes to possible completions
        Map<String, Integer> wordFrequency = new HashMap<>();
        
        // First, count word frequencies
        for (TaggedSentence sentence : sentences) {
            for (TaggerWord word : sentence.getWords()) {
                String wordText = word.getWord().toLowerCase();
                wordFrequency.put(wordText, wordFrequency.getOrDefault(wordText, 0) + 1);
            }
        }
        
        // Now build prefix -> completions map
        for (String word : wordFrequency.keySet()) {
            int freq = wordFrequency.get(word);
            
            // For each prefix of the word, add this word as a possible completion
            for (int i = 1; i <= word.length(); i++) {
                String prefix = word.substring(0, i);
                
                wordCompletionMap.putIfAbsent(prefix, new ArrayList<>());
                List<String> completions = wordCompletionMap.get(prefix);
                
                // Add the word and its frequency as a pair
                completions.add(word + ":" + freq);
                
                // Sort and keep top 5 completions by frequency
                if (completions.size() > 5) {
                    completions.sort((a, b) -> {
                        int freqA = Integer.parseInt(a.split(":")[1]);
                        int freqB = Integer.parseInt(b.split(":")[1]);
                        return Integer.compare(freqB, freqA); // Sort in descending order
                    });
                    
                    wordCompletionMap.put(prefix, completions.subList(0, 5));
                }
            }
        }
    }
    
    private List<String> getWordCompletions(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return new ArrayList<>();
        }
        
        prefix = prefix.toLowerCase();
        List<String> completions = wordCompletionMap.getOrDefault(prefix, new ArrayList<>());
        
        // Extract just the words, not the frequencies
        return completions.stream()
                .map(s -> s.split(":")[0])
                .collect(Collectors.toList());
    }
    
    public void start() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        System.out.println("\n==== POS Tagger Real-Time Interactive Test ====");
        System.out.println("Type a word or sentence and see predictions as you type.");
        System.out.println("Press Enter after each word to see POS tags.");
        System.out.println("Type 'exit' to quit the program.");
        System.out.println("=================================================\n");
        
        StringBuilder currentText = new StringBuilder();
        StringBuilder currentWord = new StringBuilder();
        List<String> currentWords = new ArrayList<>();
        
        try {
            while (true) {
                System.out.print("> " + currentText);
                
                // Read a single character
                int c = System.in.read();
                char ch = (char) c;
                
                // Exit condition
                if (currentText.toString().trim().equals("exit")) {
                    System.out.println("\nExiting real-time test.");
                    break;
                }
                
                // Handle different input characters
                if (c == -1) {
                    // End of input
                    break;
                } else if (ch == '\n' || ch == '\r') {
                    // Process the completed input on Enter key
                    if (currentWord.length() > 0) {
                        currentWords.add(currentWord.toString());
                        currentWord.setLength(0);
                    }
                    
                    if (!currentWords.isEmpty()) {
                        System.out.println("\nAnalyzing: " + String.join(" ", currentWords));
                        processWords(currentWords);
                        currentWords.clear();
                        currentText.setLength(0);
                        System.out.println();
                    }
                } else if (ch == ' ') {
                    // Space - end of current word
                    if (currentWord.length() > 0) {
                        currentWords.add(currentWord.toString());
                        currentWord.setLength(0);
                    }
                    currentText.append(ch);
                } else if (ch == 8 || ch == 127) {
                    // Backspace/Delete - remove last character
                    if (currentWord.length() > 0) {
                        currentWord.deleteCharAt(currentWord.length() - 1);
                    } else if (currentText.length() > 0) {
                        currentText.deleteCharAt(currentText.length() - 1);
                    }
                    
                    // Clear the line and reprint
                    System.out.print("\r                                                          \r");
                } else {
                    // Add character to current word
                    currentWord.append(ch);
                    currentText.append(ch);
                    
                    // Show word completions for the current word
                    List<String> completions = getWordCompletions(currentWord.toString());
                    if (!completions.isEmpty()) {
                        System.out.print("\r                                                          \r");
                        System.out.print("> " + currentText);
                        System.out.print(" [Suggestions: " + String.join(", ", completions) + "]");
                    }
                }
                
                // Clear any extra characters from the input buffer (especially for newlines)
                if (c == '\r' || c == '\n') {
                    while (reader.ready() && (reader.read() == '\n' || reader.read() == '\r')) {
                        // Skip newline characters
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
    }
    
    private void processWords(List<String> words) {
        // Create a TaggedSentence with empty tags
        TaggedSentence sentence = new TaggedSentence();
        for (String word : words) {
            sentence.addWord(new TaggerWord(word, ""));
        }
        
        // Get predictions from all taggers
        System.out.println("\n=== Word-by-word POS Tags ===");
        System.out.printf("%-15s %-8s %-8s %-8s %-8s %-30s\n", 
                "Word", "1-gram", "2-gram", "3-gram", "4-gram", "Description");
        System.out.println("--------------------------------------------------------------------------------");
        
        List<TaggerWord> taggedWords = sentence.getWords();
        for (int i = 0; i < taggedWords.size(); i++) {
            String word = taggedWords.get(i).getWord();
            
            String unigramTag = unigramTagger.predict(taggedWords, i);
            String bigramTag = bigramTagger.predict(taggedWords, i);
            String trigramTag = trigramTagger.predict(taggedWords, i);
            String quadgramTag = quadgramTagger.predict(taggedWords, i);
            
            // Set the tag in the word object for subsequent predictions
            taggedWords.get(i).setTag(quadgramTag);
            
            // Get the description for the 4-gram tag (as it's likely the most accurate)
            String description = POS_TAG_DESCRIPTIONS.getOrDefault(quadgramTag, "");
            
            System.out.printf("%-15s %-8s %-8s %-8s %-8s %-30s\n", 
                    word, unigramTag, bigramTag, trigramTag, quadgramTag, description);
        }
        
        // Show the complete tagged sentence
        System.out.println("\n=== Complete Tagged Sentence ===");
        String formattedSentence = taggedWords.stream()
                .map(word -> word.getWord() + "/" + word.getTag())
                .collect(Collectors.joining(" "));
        System.out.println(formattedSentence);
    }
    
    public static void main(String[] args) {
        try {
            RealTimeInteractiveTest test = new RealTimeInteractiveTest();
            test.start();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}