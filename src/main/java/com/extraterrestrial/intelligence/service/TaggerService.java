package com.extraterrestrial.intelligence.service;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.model.*;
import com.extraterrestrial.intelligence.repository.DatasetRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple tagger service without Spring dependencies
 */
public class TaggerService {

    private final DatasetRepository datasetRepository;
    
    public TaggerService(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }
    
    public void processAndEvaluate() {
        // Load all sentences
        List<TaggedSentence> allSentences = datasetRepository.loadSentences();
        System.out.println("Loaded " + allSentences.size() + " sentences");
        
        // Jackknife evaluation with 1000 sentences per fold
        int foldSize = 1000;
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
            
            // Build taggers with backoff chain
            DefaultTagger defaultTagger = new DefaultTagger("NN");
            UniGramTagger unigramTagger = new UniGramTagger(defaultTagger);
            BiGramTagger bigramTagger = new BiGramTagger(unigramTagger);
            TriGramTagger trigramTagger = new TriGramTagger(bigramTagger);
            QuadGramTagger quadgramTagger = new QuadGramTagger(trigramTagger);
            
            // Train all taggers
            unigramTagger.train(trainingSentences);
            bigramTagger.train(trainingSentences);
            trigramTagger.train(trainingSentences);
            quadgramTagger.train(trainingSentences);
            
            // Evaluate each tagger
            double unigramAccuracy = unigramTagger.evaluate(testSentences);
            double bigramAccuracy = bigramTagger.evaluate(testSentences);
            double trigramAccuracy = trigramTagger.evaluate(testSentences);
            double quadgramAccuracy = quadgramTagger.evaluate(testSentences);
            
            // Store results
            unigramAccuracies.add(unigramAccuracy);
            bigramAccuracies.add(bigramAccuracy);
            trigramAccuracies.add(trigramAccuracy);
            quadgramAccuracies.add(quadgramAccuracy);
            
            // Report results for this fold
            System.out.println("Fold " + (i/foldSize + 1) + " Results:");
            System.out.println("Default Tagger: " + defaultTagger.evaluate(testSentences) + "%");
            System.out.println("Unigram Tagger: " + unigramAccuracy + "%");
            System.out.println("Bigram Tagger: " + bigramAccuracy + "%");
            System.out.println("Trigram Tagger: " + trigramAccuracy + "%");
            System.out.println("Quadgram Tagger: " + quadgramAccuracy + "%");
        }
        
        // Calculate and display overall results
        System.out.println("\n\nOverall Results (Average Across All Folds):");
        System.out.println("Unigram Tagger: " + calculateAverage(unigramAccuracies) + "%");
        System.out.println("Bigram Tagger: " + calculateAverage(bigramAccuracies) + "%");
        System.out.println("Trigram Tagger: " + calculateAverage(trigramAccuracies) + "%");
        System.out.println("Quadgram Tagger: " + calculateAverage(quadgramAccuracies) + "%");
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