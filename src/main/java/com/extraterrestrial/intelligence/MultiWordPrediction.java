package com.extraterrestrial.intelligence;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import com.extraterrestrial.intelligence.model.*;
import com.extraterrestrial.intelligence.repository.CSVDatasetRepository;
import com.extraterrestrial.intelligence.repository.DatasetRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Multi-word prediction test - predicts entire phrases as you type
 */
public class MultiWordPrediction {

    // n-gram word sequence prediction maps (1-gram to 4-gram)
    private Map<String, List<String>> unigramWordMap;
    private Map<String, List<String>> bigramWordMap;
    private Map<String, List<String>> trigramWordMap;
    private Map<String, List<String>> quadgramWordMap;
    
    // POS taggers for context-aware prediction
    private Tagger unigramTagger;
    private Tagger bigramTagger;
    private Tagger trigramTagger;
    private Tagger quadgramTagger;
    
    public MultiWordPrediction() {
        System.out.println("Loading dataset...");
        
        // Load dataset
        DatasetRepository repository = new CSVDatasetRepository();
        List<TaggedSentence> sentences = repository.loadSentences();
        
        if (sentences.isEmpty()) {
            System.out.println("ERROR: No sentences loaded. Check the dataset file.");
            System.exit(1);
        }
        
        System.out.println("Loaded " + sentences.size() + " sentences from dataset");
        
        // Split data for training
        int splitPoint = (int) (sentences.size() * 0.9);
        List<TaggedSentence> trainingSentences = sentences.subList(0, splitPoint);
        
        // Build language models
        System.out.println("Building n-gram word prediction models...");
        buildWordSequenceModels(trainingSentences);
        
        // Train POS taggers
        System.out.println("Training POS taggers...");
        trainTaggers(trainingSentences);
        
        System.out.println("All models built successfully.");
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
        
        System.out.println("Unigram model size: " + unigramWordMap.size());
        System.out.println("Bigram model size: " + bigramWordMap.size());
        System.out.println("Trigram model size: " + trigramWordMap.size());
        System.out.println("Quadgram model size: " + quadgramWordMap.size());
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
    
    public void start() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n==== Multi-Word Prediction Test ====");
        System.out.println("Type the beginning of a sentence and press Enter to see multiple word predictions.");
        System.out.println("Type 'exit' to quit the program.");
        System.out.println("=====================================\n");
        
        while (true) {
            System.out.print("\nEnter text: ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting multi-word prediction test.");
                break;
            }
            
            if (input.isEmpty()) {
                continue;
            }
            
            // Show predictions
            predictNextWords(input);
        }
        
        scanner.close();
    }
    
    private void predictNextWords(String text) {
        System.out.println("\nPredictions for: \"" + text + "\"");
        System.out.println("-".repeat(40));
        
        // Tokenize input
        String[] tokens = text.toLowerCase().split("\\s+");
        int numTokens = tokens.length;
        
        // Use appropriate n-gram model based on input length
        if (numTokens >= 4) {
            // Use quadgram model (4 previous words)
            String context = tokens[numTokens-4] + " " + tokens[numTokens-3] + " " + 
                    tokens[numTokens-2] + " " + tokens[numTokens-1];
            List<String> predictions = getTopPredictions(context, quadgramWordMap, 5);
            System.out.println("4-gram predictions (based on last 4 words):");
            showCompletions(text, predictions, tokens[numTokens-1]);
        }
        
        if (numTokens >= 3) {
            // Use trigram model (3 previous words)
            String context = tokens[numTokens-3] + " " + tokens[numTokens-2] + " " + tokens[numTokens-1];
            List<String> predictions = getTopPredictions(context, trigramWordMap, 5);
            System.out.println("\n3-gram predictions (based on last 3 words):");
            showCompletions(text, predictions, tokens[numTokens-1]);
        }
        
        if (numTokens >= 2) {
            // Use bigram model (2 previous words)
            String context = tokens[numTokens-2] + " " + tokens[numTokens-1];
            List<String> predictions = getTopPredictions(context, bigramWordMap, 5);
            System.out.println("\n2-gram predictions (based on last 2 words):");
            showCompletions(text, predictions, tokens[numTokens-1]);
        }
        
        if (numTokens >= 1) {
            // Use unigram model (1 previous word)
            String context = tokens[numTokens-1];
            List<String> predictions = getTopPredictions(context, unigramWordMap, 5);
            System.out.println("\n1-gram predictions (based on last word):");
            showCompletions(text, predictions, tokens[numTokens-1]);
        }
        
        // Generate longer phrases by chaining predictions
        System.out.println("\nComplete phrase predictions:");
        List<String> phrasePredictions = generatePhrases(text, 5, 5);
        for (int i = 0; i < phrasePredictions.size(); i++) {
            System.out.println((i+1) + ". " + phrasePredictions.get(i));
        }
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
    
    private void showCompletions(String text, List<String> predictions, String lastWord) {
        if (predictions.isEmpty()) {
            System.out.println("  No predictions available");
            return;
        }
        
        for (int i = 0; i < predictions.size(); i++) {
            String nextWord = predictions.get(i);
            String completion = text + " " + nextWord;
            System.out.println((i+1) + ". " + completion);
            
            // Tag the completion to show POS tags
            TaggedSentence taggedSentence = new TaggedSentence();
            for (String word : (text + " " + nextWord).split("\\s+")) {
                taggedSentence.addWord(new TaggerWord(word, ""));
            }
            
            TaggedSentence result = quadgramTagger.tagSentence(taggedSentence);
            String taggedText = result.getWords().stream()
                    .map(w -> w.getWord() + "/" + w.getTag())
                    .collect(Collectors.joining(" "));
            System.out.println("   Tags: " + taggedText);
        }
    }
    
    private List<String> generatePhrases(String text, int wordsToAdd, int numPhrases) {
        List<String> phrases = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        
        String[] tokens = text.toLowerCase().split("\\s+");
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
            String currentPhrase = baseText + " " + nextWord;
            
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
            if (!seen.contains(currentPhrase)) {
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
        try {
            MultiWordPrediction test = new MultiWordPrediction();
            test.start();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}