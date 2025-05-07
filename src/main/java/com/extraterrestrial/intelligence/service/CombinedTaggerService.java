package com.extraterrestrial.intelligence.service;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import com.extraterrestrial.intelligence.model.*;
import com.extraterrestrial.intelligence.repository.DatasetRepository;
import com.extraterrestrial.intelligence.util.WordShapeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced tagger service that combines predictions from multiple models
 * instead of using a simple backoff chain
 */
public class CombinedTaggerService {

    private final DatasetRepository datasetRepository;
    
    // Individual taggers
    private DefaultTagger defaultTagger;
    private UniGramTagger unigramTagger;
    private BiGramTagger bigramTagger;
    private TriGramTagger trigramTagger;
    private QuadGramTagger quadgramTagger;
    
    public CombinedTaggerService(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }
    
    public void trainTaggers(List<TaggedSentence> trainingSentences) {
        // Initialize taggers
        this.defaultTagger = new DefaultTagger();
        
        this.unigramTagger = new UniGramTagger(defaultTagger);
        this.unigramTagger.train(trainingSentences);
        
        this.bigramTagger = new BiGramTagger(unigramTagger);
        this.bigramTagger.train(trainingSentences);
        
        this.trigramTagger = new TriGramTagger(bigramTagger);
        this.trigramTagger.train(trainingSentences);
        
        this.quadgramTagger = new QuadGramTagger(trigramTagger);
        this.quadgramTagger.train(trainingSentences);
    }
    
    /**
     * Tags a sentence using an ensemble of taggers with weighted voting
     */
    public TaggedSentence tagSentence(TaggedSentence sentence) {
        List<TaggerWord> originalWords = sentence.getWords();
        List<TaggerWord> taggedWords = new ArrayList<>();
        
        // Create a working copy that we'll update as we go
        List<TaggerWord> workingWords = new ArrayList<>(originalWords);
        
        // First pass: Get predictions from all taggers
        for (int i = 0; i < workingWords.size(); i++) {
            String currentWord = workingWords.get(i).getWord();
            
            // Apply special rules for different word types first
            
            // Handle punctuation specially
            if (WordShapeUtil.isPunctuation(currentWord)) {
                String predictedTag = "PUNCT";
                taggedWords.add(new TaggerWord(currentWord, predictedTag));
                workingWords.set(i, new TaggerWord(currentWord, predictedTag));
                continue;
            }
            
            // For numbers, use NUM tag
            if (WordShapeUtil.isNumeric(currentWord)) {
                String predictedTag = "NUM";
                taggedWords.add(new TaggerWord(currentWord, predictedTag));
                workingWords.set(i, new TaggerWord(currentWord, predictedTag));
                continue;
            }
            
            // For proper nouns (capitalized words not at start of sentence and not "I")
            if (WordShapeUtil.isCapitalized(currentWord) && i > 0 && !currentWord.toLowerCase().equals("i")) {
                // If it's a name or proper noun
                String predictedTag = "NNP";
                taggedWords.add(new TaggerWord(currentWord, predictedTag));
                workingWords.set(i, new TaggerWord(currentWord, predictedTag));
                continue;
            }
            
            // Now, get predictions from each tagger
            String defaultPrediction = defaultTagger.predict(workingWords, i);
            String unigramPrediction = unigramTagger.predict(workingWords, i);
            String bigramPrediction = bigramTagger.predict(workingWords, i);
            
            // For advanced context strategies, use either trigram or quadgram predictions
            String trigramPrediction = trigramTagger.predict(workingWords, i);
            String quadgramPrediction = quadgramTagger.predict(workingWords, i);
            
            // Create a weighted voting system based on position in the sentence
            Map<String, Integer> votes = new HashMap<>();
            
            // In this dataset, since sentences are often single words, unigram often works better
            votes.put(unigramPrediction, 2); // Increase unigram weight
            
            // Bigrams are strongest for this dataset, as we've seen
            votes.put(bigramPrediction, 3); // Bigram gets highest base weight
            
            // Add weights for higher-order n-grams only if they have enough context
            if (i > 1) { // Need at least 2 previous words for these to be reliable
                votes.put(trigramPrediction, votes.getOrDefault(trigramPrediction, 0) + 1); 
                votes.put(quadgramPrediction, votes.getOrDefault(quadgramPrediction, 0) + 1);
            }
            
            // If we have specific context, give more weight to higher-order n-grams
            if (i > 0) {
                String prevTag = workingWords.get(i - 1).getTag();
                
                // Word after determiner is likely noun or adjective
                if (prevTag.equals("DT")) {
                    if (bigramPrediction.startsWith("NN") || bigramPrediction.equals("JJ")) {
                        votes.put(bigramPrediction, votes.getOrDefault(bigramPrediction, 0) + 2);
                    }
                }
                
                // Word after preposition often noun or determiner
                if (prevTag.equals("IN")) {
                    if (bigramPrediction.startsWith("NN") || bigramPrediction.equals("DT")) {
                        votes.put(bigramPrediction, votes.getOrDefault(bigramPrediction, 0) + 2);
                    }
                }
            }
            
            // 4. Check for suffix-based predictions
            String guessedTag = WordShapeUtil.guessPosFromSuffix(currentWord.toLowerCase());
            if (guessedTag != null) {
                votes.put(guessedTag, votes.getOrDefault(guessedTag, 0) + 2);
            }
            
            // Find the tag with the most votes
            String bestTag = null;
            int maxVotes = 0;
            for (Map.Entry<String, Integer> entry : votes.entrySet()) {
                if (entry.getValue() > maxVotes) {
                    maxVotes = entry.getValue();
                    bestTag = entry.getKey();
                }
            }
            
            // Use the prediction with the most votes
            String finalPrediction = bestTag != null ? bestTag : unigramPrediction;
            taggedWords.add(new TaggerWord(currentWord, finalPrediction));
            workingWords.set(i, new TaggerWord(currentWord, finalPrediction));
        }
        
        return new TaggedSentence(taggedWords);
    }
    
    /**
     * Evaluates the tagger on test data
     */
    public double evaluate(List<TaggedSentence> testSentences) {
        int totalWords = 0;
        int correctPredictions = 0;
        
        for (TaggedSentence sentence : testSentences) {
            TaggedSentence taggedSentence = tagSentence(sentence);
            
            List<TaggerWord> originalWords = sentence.getWords();
            List<TaggerWord> predictedWords = taggedSentence.getWords();
            
            for (int i = 0; i < originalWords.size(); i++) {
                String actualTag = originalWords.get(i).getTag();
                String predictedTag = predictedWords.get(i).getTag();
                
                totalWords++;
                if (actualTag.equals(predictedTag)) {
                    correctPredictions++;
                }
            }
        }
        
        return totalWords > 0 ? (double) correctPredictions / totalWords * 100 : 0;
    }
    
    public void processAndEvaluate() {
        // Load all sentences
        List<TaggedSentence> allSentences = datasetRepository.loadSentences();
        System.out.println("Loaded " + allSentences.size() + " sentences");
        
        // Jackknife evaluation with 1000 sentences per fold
        int foldSize = 1000;
        List<Double> combinedAccuracies = new ArrayList<>();
        List<Double> defaultAccuracies = new ArrayList<>();
        List<Double> unigramAccuracies = new ArrayList<>();
        List<Double> bigramAccuracies = new ArrayList<>();
        List<Double> trigramAccuracies = new ArrayList<>();
        List<Double> quadgramAccuracies = new ArrayList<>();
        
        for (int i = 0; i < allSentences.size(); i += foldSize) {
            // Split data into training and test sets
            List<TaggedSentence> testSentences = new ArrayList<>();
            List<TaggedSentence> trainingSentences = new ArrayList<>(allSentences);
            
            // Extract test sentences (up to foldSize, or remaining sentences)
            int endIndex = Math.min(i + foldSize, allSentences.size());
            for (int j = i; j < endIndex; j++) {
                if (j < allSentences.size()) {
                    testSentences.add(allSentences.get(j));
                    trainingSentences.remove(allSentences.get(j));
                }
            }
            
            System.out.println("\n\nFold " + (i/foldSize + 1) + " - Training on " + trainingSentences.size() + 
                    " sentences, testing on " + testSentences.size() + " sentences");
            
            // Train taggers
            trainTaggers(trainingSentences);
            
            // Evaluate combined tagger
            double combinedAccuracy = evaluate(testSentences);
            combinedAccuracies.add(combinedAccuracy);
            
            // Evaluate individual taggers for comparison
            defaultAccuracies.add(defaultTagger.evaluate(testSentences));
            unigramAccuracies.add(unigramTagger.evaluate(testSentences));
            bigramAccuracies.add(bigramTagger.evaluate(testSentences));
            trigramAccuracies.add(trigramTagger.evaluate(testSentences));
            quadgramAccuracies.add(quadgramTagger.evaluate(testSentences));
            
            // Report results for this fold
            System.out.println("Fold " + (i/foldSize + 1) + " Results:");
            System.out.println("Default Tagger: " + defaultAccuracies.get(defaultAccuracies.size() - 1) + "%");
            System.out.println("Unigram Tagger: " + unigramAccuracies.get(unigramAccuracies.size() - 1) + "%");
            System.out.println("Bigram Tagger: " + bigramAccuracies.get(bigramAccuracies.size() - 1) + "%");
            System.out.println("Trigram Tagger: " + trigramAccuracies.get(trigramAccuracies.size() - 1) + "%");
            System.out.println("Quadgram Tagger: " + quadgramAccuracies.get(quadgramAccuracies.size() - 1) + "%");
            System.out.println("Combined Tagger: " + combinedAccuracy + "%");
        }
        
        // Calculate and display overall results
        System.out.println("\n\nOverall Results (Average Across All Folds):");
        System.out.println("Default Tagger: " + calculateAverage(defaultAccuracies) + "%");
        System.out.println("Unigram Tagger: " + calculateAverage(unigramAccuracies) + "%");
        System.out.println("Bigram Tagger: " + calculateAverage(bigramAccuracies) + "%");
        System.out.println("Trigram Tagger: " + calculateAverage(trigramAccuracies) + "%");
        System.out.println("Quadgram Tagger: " + calculateAverage(quadgramAccuracies) + "%");
        System.out.println("Combined Tagger: " + calculateAverage(combinedAccuracies) + "%");
    }
    
    private double calculateAverage(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        
        double sum = 0;
        for (Double value : values) {
            sum += value;
        }
        
        return sum / values.size();
    }
}