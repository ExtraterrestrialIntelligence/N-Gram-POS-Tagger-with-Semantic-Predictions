package com.extraterrestrial.intelligence;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import com.extraterrestrial.intelligence.model.*;
import com.extraterrestrial.intelligence.repository.CSVDatasetRepository;
import com.extraterrestrial.intelligence.repository.DatasetRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates the taggers using a jackknife procedure that systematically
 * removes 1000 sentences at a time as a testing set
 */
public class JackknifeEvaluator {

    public static void main(String[] args) {
        System.out.println("N-Gram POS Tagger Jackknife Evaluation");
        System.out.println("======================================\n");
        
        // Load dataset
        System.out.println("Loading dataset...");
        DatasetRepository repository = new CSVDatasetRepository();
        List<TaggedSentence> allSentences = repository.loadSentences();
        
        if (allSentences.isEmpty()) {
            System.out.println("Error: No sentences loaded from dataset.");
            return;
        }
        
        System.out.println("Loaded " + allSentences.size() + " sentences.\n");
        
        // Jackknife evaluation with 1000 sentences per fold
        int foldSize = 1000;
        
        // Lists to store accuracy for each tagger across folds
        List<Double> defaultAccuracies = new ArrayList<>();
        List<Double> unigramAccuracies = new ArrayList<>();
        List<Double> bigramAccuracies = new ArrayList<>();
        List<Double> trigramAccuracies = new ArrayList<>();
        List<Double> quadgramAccuracies = new ArrayList<>();
        
        int numFolds = (int) Math.ceil((double) allSentences.size() / foldSize);
        System.out.println("Performing jackknife evaluation with " + numFolds + " folds (1000 sentences per fold)...\n");
        
        for (int i = 0; i < allSentences.size(); i += foldSize) {
            // Create test set of up to 1000 sentences
            List<TaggedSentence> testSentences = new ArrayList<>();
            int endIndex = Math.min(i + foldSize, allSentences.size());
            
            // Create a test set
            for (int j = i; j < endIndex; j++) {
                testSentences.add(allSentences.get(j));
            }
            
            // Create training set (all sentences except test set)
            List<TaggedSentence> trainingSentences = new ArrayList<>(allSentences);
            trainingSentences.removeAll(testSentences);
            
            System.out.println("Fold " + (i/foldSize + 1) + "/" + numFolds);
            System.out.println("  Training on " + trainingSentences.size() + " sentences");
            System.out.println("  Testing on " + testSentences.size() + " sentences");
            
            // Build and evaluate taggers
            evaluateFold(trainingSentences, testSentences, 
                    defaultAccuracies, unigramAccuracies, bigramAccuracies, 
                    trigramAccuracies, quadgramAccuracies);
        }
        
        // Calculate and report overall results
        System.out.println("\n\nOverall Results (Average Across All Folds):");
        System.out.println("------------------------------------------------");
        System.out.printf("Default Tagger:  %.2f%%\n", calculateAverage(defaultAccuracies));
        System.out.printf("Unigram Tagger:  %.2f%%\n", calculateAverage(unigramAccuracies));
        System.out.printf("Bigram Tagger:   %.2f%%\n", calculateAverage(bigramAccuracies));
        System.out.printf("Trigram Tagger:  %.2f%%\n", calculateAverage(trigramAccuracies));
        System.out.printf("Quadgram Tagger: %.2f%%\n", calculateAverage(quadgramAccuracies));
        
        // Report improvement over baseline
        double defaultAvg = calculateAverage(defaultAccuracies);
        System.out.println("\nImprovement over baseline (Default Tagger):");
        System.out.printf("Unigram Tagger:  %.2f%%\n", calculateAverage(unigramAccuracies) - defaultAvg);
        System.out.printf("Bigram Tagger:   %.2f%%\n", calculateAverage(bigramAccuracies) - defaultAvg);
        System.out.printf("Trigram Tagger:  %.2f%%\n", calculateAverage(trigramAccuracies) - defaultAvg);
        System.out.printf("Quadgram Tagger: %.2f%%\n", calculateAverage(quadgramAccuracies) - defaultAvg);
    }
    
    private static void evaluateFold(
            List<TaggedSentence> trainingSentences, 
            List<TaggedSentence> testSentences,
            List<Double> defaultAccuracies,
            List<Double> unigramAccuracies,
            List<Double> bigramAccuracies,
            List<Double> trigramAccuracies,
            List<Double> quadgramAccuracies) {
        
        // Build taggers with backoff chain
        System.out.println("  Building and training taggers...");
        
        DefaultTagger defaultTagger = new DefaultTagger();
        
        UniGramTagger unigramTagger = new UniGramTagger(defaultTagger);
        unigramTagger.train(trainingSentences);
        
        BiGramTagger bigramTagger = new BiGramTagger(unigramTagger);
        bigramTagger.train(trainingSentences);
        
        TriGramTagger trigramTagger = new TriGramTagger(bigramTagger);
        trigramTagger.train(trainingSentences);
        
        QuadGramTagger quadgramTagger = new QuadGramTagger(trigramTagger);
        quadgramTagger.train(trainingSentences);
        
        // Evaluate each tagger
        System.out.println("  Evaluating taggers on test set...");
        
        double defaultAccuracy = defaultTagger.evaluate(testSentences);
        double unigramAccuracy = unigramTagger.evaluate(testSentences);
        double bigramAccuracy = bigramTagger.evaluate(testSentences);
        double trigramAccuracy = trigramTagger.evaluate(testSentences);
        double quadgramAccuracy = quadgramTagger.evaluate(testSentences);
        
        // Store results for this fold
        defaultAccuracies.add(defaultAccuracy);
        unigramAccuracies.add(unigramAccuracy);
        bigramAccuracies.add(bigramAccuracy);
        trigramAccuracies.add(trigramAccuracy);
        quadgramAccuracies.add(quadgramAccuracy);
        
        // Report results for this fold
        System.out.println("  Results for this fold:");
        System.out.printf("    Default Tagger:  %.2f%%\n", defaultAccuracy);
        System.out.printf("    Unigram Tagger:  %.2f%%\n", unigramAccuracy);
        System.out.printf("    Bigram Tagger:   %.2f%%\n", bigramAccuracy);
        System.out.printf("    Trigram Tagger:  %.2f%%\n", trigramAccuracy);
        System.out.printf("    Quadgram Tagger: %.2f%%\n", quadgramAccuracy);
        System.out.println();
        
        // Calculate some statistics on tag frequencies in this fold
        calculateTagStats(testSentences);
    }
    
    private static void calculateTagStats(List<TaggedSentence> sentences) {
        // Count tag frequencies
        java.util.Map<String, Integer> tagCounts = new java.util.HashMap<>();
        int totalWords = 0;
        
        for (TaggedSentence sentence : sentences) {
            for (TaggerWord word : sentence.getWords()) {
                String tag = word.getTag();
                tagCounts.put(tag, tagCounts.getOrDefault(tag, 0) + 1);
                totalWords++;
            }
        }
        
        // Sort by frequency
        List<java.util.Map.Entry<String, Integer>> sortedCounts = new ArrayList<>(tagCounts.entrySet());
        sortedCounts.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Show top 5 most frequent tags
        System.out.println("  Most frequent POS tags in this fold:");
        for (int i = 0; i < Math.min(5, sortedCounts.size()); i++) {
            java.util.Map.Entry<String, Integer> entry = sortedCounts.get(i);
            double percentage = (double) entry.getValue() / totalWords * 100;
            System.out.printf("    %-6s: %5d occurrences (%.2f%%)\n", 
                    entry.getKey(), entry.getValue(), percentage);
        }
    }
    
    private static double calculateAverage(List<Double> values) {
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