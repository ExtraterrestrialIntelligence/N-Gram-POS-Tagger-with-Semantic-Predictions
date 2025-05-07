package com.extraterrestrial.intelligence.model;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import com.extraterrestrial.intelligence.util.WordShapeUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trigram tagger that assigns tags based on the previous two words' tags
 */
public class TriGramTagger extends AbstractNGramTagger {
    
    private Map<String, Map<String, Integer>> contextTagFreq;
    private Map<String, String> contextToTagMap;
    private double lambda1 = 0.7; // Weight for trigram model
    private double lambda2 = 0.2; // Weight for bigram backoff
    
    public TriGramTagger(Tagger backoffTagger) {
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
        String context = getContext(sentence, position);
        
        // If we have a trigram match
        if (contextToTagMap.containsKey(context)) {
            // Get the tag distribution for this context
            Map<String, Integer> tagDistribution = contextTagFreq.get(context);
            
            // Get the most likely tag from the trigram model
            String trigramTag = contextToTagMap.get(context);
            
            // Get tag from backoff model (bigram)
            String backoffTag = backoffTagger.predict(sentence, position);
            
            // If high confidence in the trigram prediction, use it
            if (isHighConfidence(tagDistribution, trigramTag)) {
                return trigramTag;
            }
            
            // Make this model more sophisticated than the bigram by handling specific patterns
            String wordLower = sentence.get(position).getWord().toLowerCase();
            String word = sentence.get(position).getWord();
            
            // 1. Handle capitalized words (likely proper nouns)
            if (WordShapeUtil.isCapitalized(word) && position > 0 && !wordLower.equals("i")) {
                if (trigramTag.equals("NNP") || trigramTag.equals("NNPS")) {
                    return trigramTag;
                }
            }
            
            // 2. Special handling for punctuation
            if (WordShapeUtil.isPunctuation(word)) {
                return "PUNCT";
            }
            
            // 3. Check for common part of speech patterns
            if (position > 0) {
                String prevWord = sentence.get(position - 1).getWord().toLowerCase();
                String prevTag = sentence.get(position - 1).getTag();
                
                // After determiners, expect nouns or adjectives
                if (prevTag.equals("DT")) {
                    if (trigramTag.startsWith("NN") || trigramTag.equals("JJ")) {
                        return trigramTag;
                    }
                }
                
                // After prepositions, expect noun phrases
                if (prevTag.equals("IN")) {
                    if (trigramTag.startsWith("NN") || trigramTag.equals("DT") || trigramTag.equals("JJ")) {
                        return trigramTag;
                    }
                }
                
                // After possessives, expect nouns
                if (prevTag.equals("PRP$") || prevWord.endsWith("'s")) {
                    if (trigramTag.startsWith("NN")) {
                        return trigramTag;
                    }
                }
            }
            
            // 4. If the tags agree, definitely use that tag
            if (trigramTag.equals(backoffTag)) {
                return trigramTag;
            }
            
            // 5. Check for word suffixes that strongly indicate POS
            String posFromSuffix = WordShapeUtil.guessPosFromSuffix(wordLower);
            if (posFromSuffix != null && posFromSuffix.equals(trigramTag)) {
                return trigramTag;
            }
            
            // 6. Identify verbs by looking at surrounding context
            if (position > 0 && position < sentence.size() - 1) {
                String prevTag = sentence.get(position - 1).getTag();
                
                // After subjects (nouns, pronouns), often expect verbs
                if ((prevTag.startsWith("NN") || prevTag.equals("PRP")) && 
                    trigramTag.startsWith("VB")) {
                    return trigramTag;
                }
            }
            
            // For the trigram model, be more aggressive with predictions
            // since we have more context, use a lower confidence threshold
            int trigramCount = tagDistribution.getOrDefault(trigramTag, 0);
            int totalCount = tagDistribution.values().stream().mapToInt(Integer::intValue).sum();
            double trigramConfidence = totalCount > 0 ? (double) trigramCount / totalCount : 0;
            
            // Use a confidence threshold of 0.5 (more aggressive than bigram but less than quadgram)
            if (trigramConfidence >= 0.5) {
                return trigramTag;
            }
            
            // More sophisticated interpolation for trigram model
            int totalCount = tagDistribution.values().stream().mapToInt(Integer::intValue).sum();
            int trigramCount = tagDistribution.getOrDefault(trigramTag, 0);
            
            // Check if this is a rare context (few observations)
            if (totalCount < 5 || trigramCount < 3) {
                // For rare contexts, trust the backoff more
                return backoffTag;
            }
            
            // For common contexts with divided opinions (no clear winner)
            double entropy = calculateDistributionEntropy(tagDistribution, totalCount);
            if (entropy > 0.7) {  // High entropy = uncertain distribution
                return backoffTag;
            }
            
            // If the model still isn't confident, backoff to the bigram tagger
            return (trigramCount > totalCount / 3) ? trigramTag : backoffTag;
        }
        
        // If no trigram context match, fully backoff to the bigram tagger
        return backoffTagger.predict(sentence, position);
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
        
        // Require both a higher percentage AND a minimum count for higher confidence
        return totalCount > 0 && predictedCount >= 3 && 
               (double) predictedCount / totalCount >= 0.7;  // Increased from 0.65
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
        // Enhanced context for trigram tagger 
        String word = sentence.get(position).getWord();
        String wordLower = word.toLowerCase();
        String wordShape = WordShapeUtil.getWordShape(word);
        
        // Add suffix information to enhance the context
        String suffix = "";
        if (wordLower.length() > 3) {
            suffix = wordLower.substring(wordLower.length() - 3);
        }
        
        StringBuilder context = new StringBuilder();
        
        if (position > 1) {
            String prevTag2 = sentence.get(position - 2).getTag();
            String prevTag1 = sentence.get(position - 1).getTag();
            
            // Handle empty tags during the tagging process
            if (prevTag2.isEmpty()) prevTag2 = "START";
            if (prevTag1.isEmpty()) prevTag1 = "START";
            
            // Also include the previous words to provide lexical context
            String prevWord1 = sentence.get(position - 1).getWord().toLowerCase();
            String prevWord2 = sentence.get(position - 2).getWord().toLowerCase();
            
            // Build a more distinctive trigram context
            context.append("TRI:");  // Add model type prefix
            context.append(prevTag2).append(":").append(prevTag1).append(":");
            context.append(prevWord2.substring(0, Math.min(3, prevWord2.length()))).append(":");
            context.append(prevWord1).append(":");
            context.append(wordLower).append(":");
            context.append(wordShape).append(":");
            context.append(suffix);
            
            // Add next word shape if available for lookahead
            if (position < sentence.size() - 1) {
                String nextWordShape = WordShapeUtil.getWordShape(sentence.get(position + 1).getWord());
                context.append(":").append(nextWordShape);
            }
            
            return context.toString();
        } else if (position > 0) {
            // If only one previous word exists
            String prevTag = sentence.get(position - 1).getTag();
            if (prevTag.isEmpty()) prevTag = "START";
            
            String prevWord = sentence.get(position - 1).getWord().toLowerCase();
            
            context.append("TRI:");  // Add model type prefix
            context.append("START:").append(prevTag).append(":");
            context.append(prevWord).append(":");
            context.append(wordLower).append(":");
            context.append(wordShape).append(":");
            context.append(suffix);
            
            return context.toString();
        } else {
            // If at the beginning of the sentence
            context.append("TRI:");  // Add model type prefix
            context.append("START:START:START:");
            context.append(wordLower).append(":");
            context.append(wordShape).append(":");
            context.append(suffix);
            
            return context.toString();
        }
    }
}