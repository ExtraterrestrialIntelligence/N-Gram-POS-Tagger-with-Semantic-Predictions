package com.extraterrestrial.intelligence.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A semantic language model that provides meaningful word predictions
 * based on context and semantic relationships between words.
 */
public class SemanticModel {
    // Word relationship maps
    private Map<String, String> wordToPos;
    private Map<String, List<WordFreq>> wordToNextWords;
    private Map<String, List<WordFreq>> wordToRelatedWords;
    
    // N-gram models for multi-word context
    private Map<String, List<WordFreq>> unigramModel;
    private Map<String, List<WordFreq>> bigramModel;
    private Map<String, List<WordFreq>> trigramModel;
    
    // Common grammatical words to fill in gaps
    private static final List<String> COMMON_WORDS = Arrays.asList(
            "the", "a", "an", "and", "in", "on", "at", "to", "with", "by",
            "for", "of", "that", "this", "is", "are", "was", "were", "be"
    );
    
    // Inner class to store word frequencies
    static class WordFreq {
        String word;
        int frequency;
        
        WordFreq(String word, int frequency) {
            this.word = word;
            this.frequency = frequency;
        }
    }
    
    public SemanticModel() {
        wordToPos = new HashMap<>();
        wordToNextWords = new HashMap<>();
        wordToRelatedWords = new HashMap<>();
        unigramModel = new HashMap<>();
        bigramModel = new HashMap<>();
        trigramModel = new HashMap<>();
    }
    
    /**
     * Load sentences from the dataset to build the semantic model
     */
    public void loadDataset(String csvFilePath) {
        List<List<String>> sentences = new ArrayList<>();
        String currentSentenceId = "";
        List<String> currentSentence = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            boolean isHeader = true;
            
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                
                String[] parts = line.split(",");
                if (parts.length < 3) continue;
                
                String sentenceId = parts[0].trim();
                String word = parts[1].trim().toLowerCase();
                String pos = parts[2].trim();
                
                // Store POS tag for this word
                wordToPos.put(word, pos);
                
                // If sentence changes, save the current one and start a new one
                if (!sentenceId.equals(currentSentenceId) && !currentSentenceId.isEmpty()) {
                    if (!currentSentence.isEmpty()) {
                        sentences.add(new ArrayList<>(currentSentence));
                    }
                    currentSentence.clear();
                }
                
                // Add the word to the current sentence
                currentSentence.add(word);
                currentSentenceId = sentenceId;
            }
            
            // Add the last sentence
            if (!currentSentence.isEmpty()) {
                sentences.add(currentSentence);
            }
            
            // Now build the semantic models from the sentences
            buildModels(sentences);
            
            System.out.println("Loaded " + sentences.size() + " sentences");
            System.out.println("Vocabulary size: " + wordToPos.size() + " words");
            
        } catch (IOException e) {
            System.err.println("Error loading dataset: " + e.getMessage());
        }
    }
    
    /**
     * Build various language models from the sentences
     */
    private void buildModels(List<List<String>> sentences) {
        // Maps to count word frequencies
        Map<String, Map<String, Integer>> nextWordCounts = new HashMap<>();
        Map<String, Map<String, Integer>> relatedWordCounts = new HashMap<>();
        Map<String, Map<String, Integer>> unigramCounts = new HashMap<>();
        Map<String, Map<String, Integer>> bigramCounts = new HashMap<>();
        Map<String, Map<String, Integer>> trigramCounts = new HashMap<>();
        
        // Process each sentence
        for (List<String> sentence : sentences) {
            // Skip very short sentences
            if (sentence.size() < 3) continue;
            
            // Build word relationships
            for (int i = 0; i < sentence.size(); i++) {
                String word = sentence.get(i);
                
                // Next words
                nextWordCounts.putIfAbsent(word, new HashMap<>());
                if (i < sentence.size() - 1) {
                    String nextWord = sentence.get(i + 1);
                    nextWordCounts.get(word).put(nextWord, 
                            nextWordCounts.get(word).getOrDefault(nextWord, 0) + 1);
                }
                
                // Related words (words in context)
                relatedWordCounts.putIfAbsent(word, new HashMap<>());
                for (int j = Math.max(0, i - 3); j < Math.min(sentence.size(), i + 4); j++) {
                    if (j != i) {
                        String contextWord = sentence.get(j);
                        relatedWordCounts.get(word).put(contextWord,
                                relatedWordCounts.get(word).getOrDefault(contextWord, 0) + 1);
                    }
                }
                
                // Unigram model
                unigramCounts.putIfAbsent(word, new HashMap<>());
                if (i < sentence.size() - 1) {
                    String nextWord = sentence.get(i + 1);
                    unigramCounts.get(word).put(nextWord,
                            unigramCounts.get(word).getOrDefault(nextWord, 0) + 1);
                }
                
                // Bigram model
                if (i < sentence.size() - 1) {
                    String bigram = word + " " + sentence.get(i + 1);
                    bigramCounts.putIfAbsent(bigram, new HashMap<>());
                    if (i < sentence.size() - 2) {
                        String nextWord = sentence.get(i + 2);
                        bigramCounts.get(bigram).put(nextWord,
                                bigramCounts.get(bigram).getOrDefault(nextWord, 0) + 1);
                    }
                }
                
                // Trigram model
                if (i < sentence.size() - 2) {
                    String trigram = word + " " + sentence.get(i + 1) + " " + sentence.get(i + 2);
                    trigramCounts.putIfAbsent(trigram, new HashMap<>());
                    if (i < sentence.size() - 3) {
                        String nextWord = sentence.get(i + 3);
                        trigramCounts.get(trigram).put(nextWord,
                                trigramCounts.get(trigram).getOrDefault(nextWord, 0) + 1);
                    }
                }
            }
        }
        
        // Convert frequency maps to sorted lists
        convertToSortedLists(nextWordCounts, wordToNextWords);
        convertToSortedLists(relatedWordCounts, wordToRelatedWords);
        convertToSortedLists(unigramCounts, unigramModel);
        convertToSortedLists(bigramCounts, bigramModel);
        convertToSortedLists(trigramCounts, trigramModel);
    }
    
    /**
     * Convert a frequency map to a sorted list of WordFreq objects
     */
    private void convertToSortedLists(
            Map<String, Map<String, Integer>> countMap,
            Map<String, List<WordFreq>> resultMap) {
        
        for (Map.Entry<String, Map<String, Integer>> entry : countMap.entrySet()) {
            String key = entry.getKey();
            Map<String, Integer> counts = entry.getValue();
            
            // Convert to list of WordFreq objects
            List<WordFreq> freqList = counts.entrySet().stream()
                    .map(e -> new WordFreq(e.getKey(), e.getValue()))
                    .sorted((a, b) -> Integer.compare(b.frequency, a.frequency)) // Sort by frequency (descending)
                    .collect(Collectors.toList());
            
            resultMap.put(key, freqList);
        }
    }
    
    /**
     * Get word predictions for the given input text
     */
    public Map<String, Object> getPredictions(String text) {
        Map<String, Object> result = new HashMap<>();
        
        // Get tokens from input text
        List<String> tokens = Arrays.asList(text.toLowerCase().split("\\s+"));
        if (tokens.isEmpty()) {
            return result;
        }
        
        // Generate single-word predictions
        Map<String, String> wordPredictions = new LinkedHashMap<>();
        
        // Try trigram predictions if we have enough context
        if (tokens.size() >= 3) {
            String trigram = tokens.get(tokens.size() - 3) + " " + 
                    tokens.get(tokens.size() - 2) + " " + 
                    tokens.get(tokens.size() - 1);
            
            List<WordFreq> trigramPredictions = trigramModel.get(trigram);
            if (trigramPredictions != null && !trigramPredictions.isEmpty()) {
                for (int i = 0; i < Math.min(3, trigramPredictions.size()); i++) {
                    wordPredictions.put(trigramPredictions.get(i).word, "3-gram");
                }
            }
        }
        
        // Try bigram predictions
        if (tokens.size() >= 2 && wordPredictions.size() < 5) {
            String bigram = tokens.get(tokens.size() - 2) + " " + tokens.get(tokens.size() - 1);
            
            List<WordFreq> bigramPredictions = bigramModel.get(bigram);
            if (bigramPredictions != null && !bigramPredictions.isEmpty()) {
                for (int i = 0; i < Math.min(3, bigramPredictions.size()); i++) {
                    String word = bigramPredictions.get(i).word;
                    if (!wordPredictions.containsKey(word)) {
                        wordPredictions.put(word, "2-gram");
                    }
                }
            }
        }
        
        // Try unigram predictions
        if (wordPredictions.size() < 5) {
            String lastWord = tokens.get(tokens.size() - 1);
            
            List<WordFreq> unigramPredictions = unigramModel.get(lastWord);
            if (unigramPredictions != null && !unigramPredictions.isEmpty()) {
                for (int i = 0; i < Math.min(3, unigramPredictions.size()); i++) {
                    String word = unigramPredictions.get(i).word;
                    if (!wordPredictions.containsKey(word)) {
                        wordPredictions.put(word, "1-gram");
                    }
                }
            }
        }
        
        // Try semantic predictions (related words)
        if (wordPredictions.size() < 5) {
            String lastWord = tokens.get(tokens.size() - 1);
            
            List<WordFreq> semanticPredictions = wordToRelatedWords.get(lastWord);
            if (semanticPredictions != null && !semanticPredictions.isEmpty()) {
                for (int i = 0; i < Math.min(3, semanticPredictions.size()); i++) {
                    String word = semanticPredictions.get(i).word;
                    if (!wordPredictions.containsKey(word)) {
                        wordPredictions.put(word, "semantic");
                    }
                }
            }
        }
        
        // Add some common words if we don't have enough predictions
        if (wordPredictions.size() < 3) {
            for (String commonWord : COMMON_WORDS) {
                if (!wordPredictions.containsKey(commonWord)) {
                    wordPredictions.put(commonWord, "common");
                    if (wordPredictions.size() >= 5) break;
                }
            }
        }
        
        result.put("wordPredictions", wordPredictions);
        
        // Generate complete phrases
        List<String> phrasePredictions = generatePhrases(text, 3, 3);
        result.put("phrasePredictions", phrasePredictions);
        
        return result;
    }
    
    /**
     * Generate complete phrases by extending the input text
     */
    public List<String> generatePhrases(String text, int wordsToAdd, int numPhrases) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        
        // Get tokens from input text
        List<String> tokens = new ArrayList<>(Arrays.asList(text.toLowerCase().split("\\s+")));
        if (tokens.isEmpty()) {
            return result;
        }
        
        // Try to generate multiple phrases
        for (int p = 0; p < numPhrases * 3; p++) { // Try more than we need
            List<String> currentPhrase = new ArrayList<>(tokens);
            
            // Add words to the phrase
            for (int w = 0; w < wordsToAdd; w++) {
                String nextWord = predictNextWord(currentPhrase);
                if (nextWord == null) break;
                
                currentPhrase.add(nextWord);
            }
            
            // Convert to string and add to results if unique
            String phraseStr = String.join(" ", currentPhrase);
            if (!seen.contains(phraseStr) && !phraseStr.equals(text)) {
                result.add(phraseStr);
                seen.add(phraseStr);
                
                if (result.size() >= numPhrases) {
                    break;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Predict the next word for a phrase
     */
    private String predictNextWord(List<String> phrase) {
        if (phrase.isEmpty()) {
            return COMMON_WORDS.get(new Random().nextInt(COMMON_WORDS.size()));
        }
        
        // Try trigram prediction
        if (phrase.size() >= 3) {
            String trigram = phrase.get(phrase.size() - 3) + " " + 
                    phrase.get(phrase.size() - 2) + " " + 
                    phrase.get(phrase.size() - 1);
            
            List<WordFreq> predictions = trigramModel.get(trigram);
            if (predictions != null && !predictions.isEmpty()) {
                // Weighted random selection based on frequency
                return weightedRandomSelection(predictions);
            }
        }
        
        // Try bigram prediction
        if (phrase.size() >= 2) {
            String bigram = phrase.get(phrase.size() - 2) + " " + phrase.get(phrase.size() - 1);
            
            List<WordFreq> predictions = bigramModel.get(bigram);
            if (predictions != null && !predictions.isEmpty()) {
                return weightedRandomSelection(predictions);
            }
        }
        
        // Try unigram prediction
        String lastWord = phrase.get(phrase.size() - 1);
        
        List<WordFreq> predictions = unigramModel.get(lastWord);
        if (predictions != null && !predictions.isEmpty()) {
            return weightedRandomSelection(predictions);
        }
        
        // Try next words from semantic model
        predictions = wordToNextWords.get(lastWord);
        if (predictions != null && !predictions.isEmpty()) {
            return weightedRandomSelection(predictions);
        }
        
        // Fall back to common words
        return COMMON_WORDS.get(new Random().nextInt(COMMON_WORDS.size()));
    }
    
    /**
     * Select a word from the predictions with probability weighted by frequency
     */
    private String weightedRandomSelection(List<WordFreq> predictions) {
        int totalFrequency = predictions.stream()
                .mapToInt(wf -> wf.frequency)
                .sum();
        
        int randomValue = new Random().nextInt(totalFrequency);
        int cumulativeFrequency = 0;
        
        for (WordFreq wf : predictions) {
            cumulativeFrequency += wf.frequency;
            if (randomValue < cumulativeFrequency) {
                return wf.word;
            }
        }
        
        // Should not reach here, but fallback
        return predictions.get(0).word;
    }
}