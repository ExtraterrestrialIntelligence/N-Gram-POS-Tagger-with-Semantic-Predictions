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
        
        // Create taggers, but don't chain them for backoff. We'll evaluate them independently
        DefaultTagger defaultTagger = new DefaultTagger();
        
        // Unigram with Default backoff
        UniGramTagger unigramTagger = new UniGramTagger(defaultTagger);
        unigramTagger.train(trainingSentences);
        
        // Create separate tagger instances to evaluate each n-gram model independently
        DefaultTagger defaultTagger2 = new DefaultTagger();
        UniGramTagger unigramBackoff = new UniGramTagger(defaultTagger2);
        unigramBackoff.train(trainingSentences);
        
        // Bigram with proper Unigram backoff
        BiGramTagger bigramTagger = new BiGramTagger(unigramBackoff);
        bigramTagger.train(trainingSentences);
        
        // Create another instance for Trigram's backoff
        DefaultTagger defaultTagger3 = new DefaultTagger();
        UniGramTagger unigramBackoff2 = new UniGramTagger(defaultTagger3);
        unigramBackoff2.train(trainingSentences);
        BiGramTagger bigramBackoff = new BiGramTagger(unigramBackoff2);
        bigramBackoff.train(trainingSentences);
        
        // Trigram with proper Bigram backoff
        TriGramTagger trigramTagger = new TriGramTagger(bigramBackoff);
        trigramTagger.train(trainingSentences);
        
        // Create another instance for Quadgram's backoff
        DefaultTagger defaultTagger4 = new DefaultTagger();
        UniGramTagger unigramBackoff3 = new UniGramTagger(defaultTagger4);
        unigramBackoff3.train(trainingSentences);
        BiGramTagger bigramBackoff2 = new BiGramTagger(unigramBackoff3);
        bigramBackoff2.train(trainingSentences);
        TriGramTagger trigramBackoff = new TriGramTagger(bigramBackoff2);
        trigramBackoff.train(trainingSentences);
        
        // Quadgram with proper Trigram backoff
        QuadGramTagger quadgramTagger = new QuadGramTagger(trigramBackoff);
        quadgramTagger.train(trainingSentences);
        
        // For comparative analysis, also track higher-order cases directly triggered vs backed-off
        // Evaluate each tagger
        System.out.println("  Evaluating taggers on test set...");
        
        List<TaggedSentence> defaultTagged = tagTestSentences(defaultTagger, testSentences);
        List<TaggedSentence> unigramTagged = tagTestSentences(unigramTagger, testSentences);
        List<TaggedSentence> bigramTagged = tagTestSentences(bigramTagger, testSentences);
        List<TaggedSentence> trigramTagged = tagTestSentences(trigramTagger, testSentences);
        List<TaggedSentence> quadgramTagged = tagTestSentences(quadgramTagger, testSentences);
        
        double defaultAccuracy = calculateAccuracy(testSentences, defaultTagged);
        double unigramAccuracy = calculateAccuracy(testSentences, unigramTagged);
        double bigramAccuracy = calculateAccuracy(testSentences, bigramTagged);
        double trigramAccuracy = calculateAccuracy(testSentences, trigramTagged);
        double quadgramAccuracy = calculateAccuracy(testSentences, quadgramTagged);
        
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
        
        // Analyze cases where higher order n-grams differ (actual improvement)
        int biImprovements = 0;
        int biErrors = 0;
        int triImprovements = 0;
        int triErrors = 0;
        int quadImprovements = 0;
        int quadErrors = 0;
        
        int totalWords = 0;
        for (int i = 0; i < testSentences.size(); i++) {
            TaggedSentence gold = testSentences.get(i);
            TaggedSentence uniTagged = unigramTagged.get(i);
            TaggedSentence biTagged = bigramTagged.get(i);
            TaggedSentence triTagged = trigramTagged.get(i);
            TaggedSentence quadTagged = quadgramTagged.get(i);
            
            for (int j = 0; j < gold.getWords().size(); j++) {
                totalWords++;
                String goldTag = gold.getWords().get(j).getTag();
                String uniTag = uniTagged.getWords().get(j).getTag();
                String biTag = biTagged.getWords().get(j).getTag();
                String triTag = triTagged.getWords().get(j).getTag();
                String quadTag = quadTagged.getWords().get(j).getTag();
                
                // Where Bigram differs from Unigram
                if (!biTag.equals(uniTag)) {
                    if (biTag.equals(goldTag) && !uniTag.equals(goldTag)) {
                        biImprovements++;
                    } else if (!biTag.equals(goldTag) && uniTag.equals(goldTag)) {
                        biErrors++;
                    }
                }
                
                // Where Trigram differs from Bigram
                if (!triTag.equals(biTag)) {
                    if (triTag.equals(goldTag) && !biTag.equals(goldTag)) {
                        triImprovements++;
                    } else if (!triTag.equals(goldTag) && biTag.equals(goldTag)) {
                        triErrors++;
                    }
                }
                
                // Where Quadgram differs from Trigram
                if (!quadTag.equals(triTag)) {
                    if (quadTag.equals(goldTag) && !triTag.equals(goldTag)) {
                        quadImprovements++;
                    } else if (!quadTag.equals(goldTag) && triTag.equals(goldTag)) {
                        quadErrors++;
                    }
                }
            }
        }
        
        System.out.printf("  Detailed comparison for %d words:\n", totalWords);
        System.out.printf("    Bigram vs. Unigram:   +%.2f%% (improved: %d, errors: %d)\n", 
               100.0 * (biImprovements - biErrors) / totalWords, biImprovements, biErrors);
        System.out.printf("    Trigram vs. Bigram:   +%.2f%% (improved: %d, errors: %d)\n", 
               100.0 * (triImprovements - triErrors) / totalWords, triImprovements, triErrors);
        System.out.printf("    Quadgram vs. Trigram: +%.2f%% (improved: %d, errors: %d)\n", 
               100.0 * (quadImprovements - quadErrors) / totalWords, quadImprovements, quadErrors);
        System.out.println();
        
        // Calculate some statistics on tag frequencies in this fold
        calculateTagStats(testSentences);
    }
    
    private static List<TaggedSentence> tagTestSentences(Tagger tagger, List<TaggedSentence> testSentences) {
        List<TaggedSentence> taggedSentences = new ArrayList<>();
        for (TaggedSentence sentence : testSentences) {
            taggedSentences.add(tagger.tagSentence(sentence));
        }
        return taggedSentences;
    }
    
    private static double calculateAccuracy(List<TaggedSentence> goldSentences, List<TaggedSentence> predictedSentences) {
        int totalWords = 0;
        int correctPredictions = 0;
        
        for (int i = 0; i < goldSentences.size(); i++) {
            List<TaggerWord> goldWords = goldSentences.get(i).getWords();
            List<TaggerWord> predictedWords = predictedSentences.get(i).getWords();
            
            for (int j = 0; j < goldWords.size(); j++) {
                String goldTag = goldWords.get(j).getTag();
                String predictedTag = predictedWords.get(j).getTag();
                
                totalWords++;
                if (goldTag.equals(predictedTag)) {
                    correctPredictions++;
                }
            }
        }
        
        return totalWords > 0 ? (double) correctPredictions / totalWords * 100 : 0;
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