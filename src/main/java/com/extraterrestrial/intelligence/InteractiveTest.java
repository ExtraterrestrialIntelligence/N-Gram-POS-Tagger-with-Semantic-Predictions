package com.extraterrestrial.intelligence;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import com.extraterrestrial.intelligence.model.*;
import com.extraterrestrial.intelligence.repository.CSVDatasetRepository;
import com.extraterrestrial.intelligence.repository.DatasetRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple interactive test class that allows users to input text and see POS tag predictions in real-time.
 * No Spring dependencies required.
 */
public class InteractiveTest {

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
    
    private final Tagger unigramTagger;
    private final Tagger bigramTagger;
    private final Tagger trigramTagger;
    private final Tagger quadgramTagger;
    
    public InteractiveTest() {
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
        
        System.out.println("All taggers trained successfully.");
    }
    
    public void start() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n==== POS Tagger Interactive Test ====");
        System.out.println("Type a sentence and press Enter to see POS tags.");
        System.out.println("Type 'exit' to quit the program.");
        System.out.println("=======================================\n");
        
        while (true) {
            System.out.print("\nEnter a sentence: ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting interactive test.");
                break;
            }
            
            if (input.isEmpty()) {
                continue;
            }
            
            // Process the input
            processInput(input);
        }
        
        scanner.close();
    }
    
    private void processInput(String input) {
        // Tokenize the input into words
        String[] tokens = input.split("\\s+");
        
        // Create a TaggedSentence with empty tags
        TaggedSentence sentence = new TaggedSentence();
        for (String token : tokens) {
            sentence.addWord(new TaggerWord(token, ""));
        }
        
        // Get predictions from all taggers
        System.out.println("\n=== Word-by-word POS Tags ===");
        System.out.printf("%-15s %-8s %-8s %-8s %-8s %-30s\n", 
                "Word", "1-gram", "2-gram", "3-gram", "4-gram", "Description");
        System.out.println("--------------------------------------------------------------------------------");
        
        List<TaggerWord> words = sentence.getWords();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i).getWord();
            
            String unigramTag = unigramTagger.predict(words, i);
            String bigramTag = bigramTagger.predict(words, i);
            String trigramTag = trigramTagger.predict(words, i);
            String quadgramTag = quadgramTagger.predict(words, i);
            
            // Get the description for the 4-gram tag (as it's likely the most accurate)
            String description = POS_TAG_DESCRIPTIONS.getOrDefault(quadgramTag, "");
            
            System.out.printf("%-15s %-8s %-8s %-8s %-8s %-30s\n", 
                    word, unigramTag, bigramTag, trigramTag, quadgramTag, description);
        }
        
        // Now show the complete tagged sentence using the 4-gram tagger
        TaggedSentence taggedSentence = quadgramTagger.tagSentence(sentence);
        
        System.out.println("\n=== Complete Tagged Sentence (4-gram) ===");
        String formattedSentence = taggedSentence.getWords().stream()
                .map(word -> word.getWord() + "/" + word.getTag())
                .collect(Collectors.joining(" "));
        System.out.println(formattedSentence);
    }
    
    public static void main(String[] args) {
        try {
            InteractiveTest test = new InteractiveTest();
            test.start();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}