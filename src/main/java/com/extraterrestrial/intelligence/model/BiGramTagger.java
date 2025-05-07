package com.extraterrestrial.intelligence.model;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import com.extraterrestrial.intelligence.util.WordShapeUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bigram tagger that assigns tags based on the previous word's tag
 */
public class BiGramTagger extends AbstractNGramTagger {
    
    private Map<String, Map<String, Integer>> contextTagFreq;
    private Map<String, String> contextToTagMap;
    private double lambda = 0.8; // Interpolation weight
    
    public BiGramTagger(Tagger backoffTagger) {
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
            
            if (mostFrequentTag != null) {
                contextToTagMap.put(context, mostFrequentTag);
            }
        }
    }
    
    @Override
    public String predict(List<TaggerWord> sentence, int position) {
        String word = sentence.get(position).getWord();
        
        // First check for special cases and closed class words
        String closedClassTag = WordShapeUtil.getClosedClassTag(word);
        if (closedClassTag != null) {
            return closedClassTag; // Use fixed tag for closed class words
        }
        
        // Special handling for capitalized words (likely proper nouns)
        if (WordShapeUtil.isCapitalized(word) && position > 0 && !word.toLowerCase().equals("i")) {
            // Known proper nouns from our list
            if (WordShapeUtil.isCommonProperNoun(word)) {
                return "NNP";
            }
            
            // If preceding word suggests proper name (Mr., Dr., Ms., etc.)
            String prevWord = sentence.get(position - 1).getWord().toLowerCase();
            if (prevWord.equals("mr.") || prevWord.equals("ms.") || 
                prevWord.equals("mrs.") || prevWord.equals("dr.") || 
                prevWord.equals("prof.")) {
                return "NNP";
            }
        }
        
        // Get context and check if it exists in our trained model
        String context = getContext(sentence, position);
        
        // Use the full power of modern statistical NLP: evidence combination
        Map<String, Double> tagScores = new HashMap<>();
        
        // 1. Add score from bigram model if available
        if (contextToTagMap.containsKey(context)) {
            Map<String, Integer> tagDistribution = contextTagFreq.get(context);
            int totalCount = tagDistribution.values().stream().mapToInt(Integer::intValue).sum();
            
            // Convert counts to probabilities and add to scores
            for (Map.Entry<String, Integer> tagCount : tagDistribution.entrySet()) {
                double probability = (double) tagCount.getValue() / totalCount;
                tagScores.put(tagCount.getKey(), probability * 0.8); // Bigram model gets 80% weight
            }
            
            // Get the most likely tag from bigram model
            String bigramTag = contextToTagMap.get(context);
            
            // If very high confidence, just use this tag
            if (isHighConfidence(tagDistribution, bigramTag)) {
                return bigramTag;
            }
        }
        
        // 2. Add score from backoff model (unigram)
        String backoffTag = backoffTagger.predict(sentence, position);
        tagScores.put(backoffTag, tagScores.getOrDefault(backoffTag, 0.0) + 0.5); // Unigram gets 50% weight
        
        // 3. Add score from suffix analysis
        String suffixTag = WordShapeUtil.guessPosFromSuffix(word);
        if (suffixTag != null) {
            tagScores.put(suffixTag, tagScores.getOrDefault(suffixTag, 0.0) + 0.3); // Suffix gets 30% weight
        }
        
        // 4. Special contextual patterns get extra weight
        if (position > 0) {
            // Words after determiners are likely nouns or adjectives
            if (sentence.get(position - 1).getTag().equals("DT")) {
                tagScores.put("NN", tagScores.getOrDefault("NN", 0.0) + 0.2);
                tagScores.put("JJ", tagScores.getOrDefault("JJ", 0.0) + 0.1);
            }
            
            // Words after prepositions are likely nouns or determiners
            if (sentence.get(position - 1).getTag().equals("IN")) {
                tagScores.put("NN", tagScores.getOrDefault("NN", 0.0) + 0.2);
                tagScores.put("DT", tagScores.getOrDefault("DT", 0.0) + 0.1);
            }
        }
        
        // Find the tag with the highest score
        String bestTag = backoffTag; // Default to backoff
        double maxScore = tagScores.getOrDefault(backoffTag, 0.0);
        
        for (Map.Entry<String, Double> entry : tagScores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
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
        
        // If the predicted tag occurs more than 80% of the time, consider it high confidence
        return totalCount > 0 && (double) predictedCount / totalCount >= 0.8;
    }
    
    @Override
    protected String getContext(List<TaggerWord> sentence, int position) {
        // For a bigram tagger, context is the previous word's tag and current word
        String word = sentence.get(position).getWord();
        String wordLower = word.toLowerCase();
        
        // Include the word shape as part of the context
        String wordShape = WordShapeUtil.getWordShape(word);
        
        if (position > 0) {
            String prevTag = sentence.get(position - 1).getTag();
            // Only use the tag if it's not empty
            if (prevTag.isEmpty()) {
                return "START:" + wordLower + ":" + wordShape;
            }
            return prevTag + ":" + wordLower + ":" + wordShape;
        } else {
            // If at the beginning of the sentence, use special START tag
            return "START:" + wordLower + ":" + wordShape;
        }
    }
}