package com.extraterrestrial.intelligence.model;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import com.extraterrestrial.intelligence.util.WordShapeUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QuadGram tagger that assigns tags based on the previous three words' tags
 */
public class QuadGramTagger extends AbstractNGramTagger {
    
    private Map<String, Map<String, Integer>> contextTagFreq;
    private Map<String, String> contextToTagMap;
    private double lambda1 = 0.7; // Weight for quadgram model
    private double lambda2 = 0.3; // Weight for trigram backoff
    
    public QuadGramTagger(Tagger backoffTagger) {
        super(backoffTagger);
        this.contextToTagMap = new HashMap<>();
        this.contextTagFreq = new HashMap<>();
    }
    
    @Override
    public void train(List<TaggedSentence> trainingSentences) {
        // Clear previous training data
        contextToTagMap.clear();
        contextTagFreq.clear();
        
        // Count context-tag frequencies
        for (TaggedSentence sentence : trainingSentences) {
            List<TaggerWord> words = sentence.getWords();
            
            for (int i = 0; i < words.size(); i++) {
                String context = getContext(words, i);
                String tag = words.get(i).getTag();
                
                contextTagFreq.putIfAbsent(context, new HashMap<>());
                Map<String, Integer> tagFreq = contextTagFreq.get(context);
                tagFreq.put(tag, tagFreq.getOrDefault(tag, 0) + 1);
            }
        }
        
        // Select most frequent tag for each context
        for (Map.Entry<String, Map<String, Integer>> entry : contextTagFreq.entrySet()) {
            String context = entry.getKey();
            Map<String, Integer> tagCounts = entry.getValue();
            
            String mostFrequentTag = null;
            int maxCount = 0;
            
            for (Map.Entry<String, Integer> tagCount : tagCounts.entrySet()) {
                if (tagCount.getValue() > maxCount) {
                    maxCount = tagCount.getValue();
                    mostFrequentTag = tagCount.getKey();
                }
            }
            
            if (mostFrequentTag != null && maxCount >= 2) {  // Minimum frequency threshold
                contextToTagMap.put(context, mostFrequentTag);
            }
        }
    }
    
    @Override
    protected void preprocessSpecialCases(List<TaggerWord> sentence) {
        // Mark proper nouns and closed class words in preprocessing stage
        for (int i = 0; i < sentence.size(); i++) {
            TaggerWord word = sentence.get(i);
            
            // Handle special cases like punctuation
            if (isSpecialCase(word.getWord())) {
                sentence.set(i, new TaggerWord(word.getWord(), getSpecialCaseTag(word.getWord())));
                continue;
            }
            
            // Handle closed class words
            String closedClassTag = WordShapeUtil.getClosedClassTag(word.getWord());
            if (closedClassTag != null) {
                sentence.set(i, new TaggerWord(word.getWord(), closedClassTag));
                continue;
            }
            
            // Handle capitalized words not at sentence start
            if (i > 0 && WordShapeUtil.isCapitalized(word.getWord()) && !word.getWord().equals("I")) {
                if (WordShapeUtil.isCommonProperNoun(word.getWord())) {
                    sentence.set(i, new TaggerWord(word.getWord(), "NNP"));
                    continue;
                }
                
                // Check if preceded by a title
                String prevWord = sentence.get(i-1).getWord().toLowerCase();
                if (prevWord.endsWith(".") && (prevWord.equals("mr.") || prevWord.equals("mrs.") || 
                    prevWord.equals("ms.") || prevWord.equals("dr.") || prevWord.equals("prof."))) {
                    sentence.set(i, new TaggerWord(word.getWord(), "NNP"));
                    continue;
                }
            }
        }
    }
    
    @Override
    protected void postprocessTags(List<TaggerWord> taggedWords) {
        // Apply post-processing rules to fix common patterns
        for (int i = 1; i < taggedWords.size(); i++) {
            // Rules for determiners followed by nouns
            if (taggedWords.get(i-1).getTag().equals("DT")) {
                if (!taggedWords.get(i).getTag().startsWith("NN") && 
                    !taggedWords.get(i).getTag().equals("JJ") &&
                    !taggedWords.get(i).getTag().equals("RB")) {
                    // After a determiner, usually comes noun, adjective, or rarely an adverb
                    taggedWords.set(i, new TaggerWord(taggedWords.get(i).getWord(), "NN"));
                }
            }
            
            // Fix POS in common phrases ("of the", "in the", etc.)
            if (i >= 2 && taggedWords.get(i-1).getWord().equalsIgnoreCase("the")) {
                if (taggedWords.get(i-2).getTag().equals("IN")) {
                    // In prepositional phrases, words after "the" are generally nouns
                    if (!taggedWords.get(i).getTag().startsWith("NN") && 
                        !taggedWords.get(i).getTag().equals("JJ")) {
                        taggedWords.set(i, new TaggerWord(taggedWords.get(i).getWord(), "NN"));
                    }
                }
            }
        }
    }
    
    @Override
    public String predict(List<TaggerWord> sentence, int position) {
        String word = sentence.get(position).getWord();
        
        // First check if this word was preprocessed
        if (!sentence.get(position).getTag().isEmpty()) {
            return sentence.get(position).getTag();
        }
        
        // Check for closed class words (determiners, prepositions, etc.)
        String closedClassTag = WordShapeUtil.getClosedClassTag(word);
        if (closedClassTag != null) {
            return closedClassTag;
        }
        
        // Handle capitalized words
        if (WordShapeUtil.isCapitalized(word) && position > 0 && !word.equals("I")) {
            return "NNP"; // Proper noun
        }
        
        // Get the context for this position
        String context = getContext(sentence, position);
        
        // Combine evidence from multiple sources using a weighted approach
        Map<String, Double> tagScores = new HashMap<>();
        
        // 1. Evidence from quadgram model
        if (contextToTagMap.containsKey(context)) {
            Map<String, Integer> tagDistribution = contextTagFreq.get(context);
            int totalCount = tagDistribution.values().stream().mapToInt(Integer::intValue).sum();
            
            // Convert counts to weighted probabilities
            for (Map.Entry<String, Integer> entry : tagDistribution.entrySet()) {
                double prob = (double)entry.getValue() / totalCount;
                // Quadgram model gets high weight (90%)
                tagScores.put(entry.getKey(), prob * 0.9);
            }
            
            // Get the most likely tag
            String quadgramTag = contextToTagMap.get(context);
            int quadgramCount = tagDistribution.getOrDefault(quadgramTag, 0);
            
            // If there's very high confidence in this prediction, just return it
            if (quadgramCount >= 5 && isHighConfidence(tagDistribution, quadgramTag)) {
                return quadgramTag;
            }
        }
        
        // 2. Evidence from backoff models
        String trigramTag = backoffTagger.predict(sentence, position);
        tagScores.put(trigramTag, tagScores.getOrDefault(trigramTag, 0.0) + 0.6); // 60% weight
        
        // 3. Evidence from linguistic features
        String suffixGuess = WordShapeUtil.guessPosFromSuffix(word);
        if (suffixGuess != null) {
            tagScores.put(suffixGuess, tagScores.getOrDefault(suffixGuess, 0.0) + 0.4); // 40% weight
        }
        
        // 4. Contextual evidence
        if (position > 0) {
            String prevTag = sentence.get(position-1).getTag();
            String prevWord = sentence.get(position-1).getWord().toLowerCase();
            
            // After determiners expect nouns or adjectives
            if (prevTag.equals("DT") || prevWord.equals("the") || prevWord.equals("a") || prevWord.equals("an")) {
                tagScores.put("NN", tagScores.getOrDefault("NN", 0.0) + 0.3);
                tagScores.put("JJ", tagScores.getOrDefault("JJ", 0.0) + 0.2);
            }
            
            // After prepositions expect nouns or determiners
            if (prevTag.equals("IN")) {
                tagScores.put("NN", tagScores.getOrDefault("NN", 0.0) + 0.3);
                tagScores.put("DT", tagScores.getOrDefault("DT", 0.0) + 0.2);
            }
            
            // After possessives expect nouns
            if (prevTag.equals("PRP$") || prevWord.endsWith("'s")) {
                tagScores.put("NN", tagScores.getOrDefault("NN", 0.0) + 0.4);
            }
            
            // After auxiliaries expect verbs
            if (prevTag.equals("MD") || prevWord.equals("to") || 
                prevWord.equals("will") || prevWord.equals("would") || 
                prevWord.equals("could") || prevWord.equals("should")) {
                tagScores.put("VB", tagScores.getOrDefault("VB", 0.0) + 0.4);
            }
        }
        
        // Use the tag with the highest overall score
        String bestTag = trigramTag; // Default to trigram backoff
        double bestScore = tagScores.getOrDefault(trigramTag, 0.0);
        
        for (Map.Entry<String, Double> entry : tagScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestTag = entry.getKey();
            }
        }
        
        return bestTag;
    }
    
    /**
     * Determines if a tag prediction has high confidence based on its distribution
     */
    private boolean isHighConfidence(Map<String, Integer> tagDistribution, String predictedTag) {
        if (tagDistribution == null || tagDistribution.isEmpty()) {
            return false;
        }
        
        int totalCount = tagDistribution.values().stream().mapToInt(Integer::intValue).sum();
        int predictedCount = tagDistribution.getOrDefault(predictedTag, 0);
        
        // For quadgram, we want a higher minimum count since it's more specific
        if (predictedCount < 4) {  // Increased from 3
            return false;
        }
        
        // Higher threshold for confidence (from 0.6 to 0.75) to be more selective
        return totalCount > 0 && (double) predictedCount / totalCount >= 0.75;
    }
    
    /**
     * Calculate the entropy of a distribution to measure uncertainty
     * High entropy = high uncertainty in the distribution
     */
    private double calculateDistributionEntropy(Map<String, Integer> distribution, int totalCount) {
        if (distribution == null || distribution.isEmpty() || totalCount == 0) {
            return 0;
        }
        
        double entropy = 0;
        for (int count : distribution.values()) {
            double probability = (double) count / totalCount;
            if (probability > 0) {
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }
        
        // Normalize by maximum possible entropy for this distribution size
        double maxEntropy = Math.log(distribution.size()) / Math.log(2);
        return maxEntropy > 0 ? entropy / maxEntropy : 0;
    }
    
    @Override
    protected String getContext(List<TaggerWord> sentence, int position) {
        // Create a simpler, more effective context for model training and prediction
        StringBuilder context = new StringBuilder();
        
        String word = sentence.get(position).getWord().toLowerCase();
        
        // Use just three previous tags + current word for efficient context matching
        // This reduces sparsity while retaining useful predictive power
        
        if (position > 2) {
            // Full four-word context: 3 previous tags + current word
            String tag3 = sentence.get(position - 3).getTag();
            String tag2 = sentence.get(position - 2).getTag();
            String tag1 = sentence.get(position - 1).getTag();
            
            // Handle empty tags
            if (tag3.isEmpty()) tag3 = "START";
            if (tag2.isEmpty()) tag2 = "START";
            if (tag1.isEmpty()) tag1 = "START";
            
            context.append(tag3).append("_");
            context.append(tag2).append("_");
            context.append(tag1).append("_");
            context.append(word);
            
            // Include previous word (to handle collocations)
            context.append("_").append(sentence.get(position - 1).getWord().toLowerCase());
            
            return context.toString();
        } else if (position > 1) {
            // 3-word context
            String tag2 = sentence.get(position - 2).getTag();
            String tag1 = sentence.get(position - 1).getTag();
            
            if (tag2.isEmpty()) tag2 = "START";
            if (tag1.isEmpty()) tag1 = "START";
            
            context.append("START_");
            context.append(tag2).append("_");
            context.append(tag1).append("_");
            context.append(word);
            
            // Include previous word
            context.append("_").append(sentence.get(position - 1).getWord().toLowerCase());
            
            return context.toString();
        } else if (position > 0) {
            // 2-word context
            String tag1 = sentence.get(position - 1).getTag();
            if (tag1.isEmpty()) tag1 = "START";
            
            context.append("START_START_");
            context.append(tag1).append("_");
            context.append(word);
            
            // Include previous word
            context.append("_").append(sentence.get(position - 1).getWord().toLowerCase());
            
            return context.toString();
        } else {
            // Single word context (start of sentence)
            context.append("START_START_START_");
            context.append(word);
            
            // Add sentence start marker
            context.append("_START");
            
            return context.toString();
        }
    }
}