package com.extraterrestrial.intelligence.model;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import com.extraterrestrial.intelligence.util.WordShapeUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unigram tagger that assigns tags based on word frequencies
 */
public class UniGramTagger extends AbstractNGramTagger {
    
    private Map<String, Map<String, Integer>> wordTagFreq;
    private Map<String, String> wordToTagMap;
    
    public UniGramTagger(Tagger backoffTagger) {
        super(backoffTagger);
        this.wordToTagMap = new HashMap<>();
        this.wordTagFreq = new HashMap<>();
    }
    
    @Override
    public void train(List<TaggedSentence> trainingSentences) {
        // Clear previous training data
        wordToTagMap.clear();
        wordTagFreq.clear();
        
        // Count word-tag frequencies
        for (TaggedSentence sentence : trainingSentences) {
            for (TaggerWord word : sentence.getWords()) {
                String wordText = word.getWord().toLowerCase();
                String tag = word.getTag();
                
                wordTagFreq.putIfAbsent(wordText, new HashMap<>());
                Map<String, Integer> tagFreq = wordTagFreq.get(wordText);
                tagFreq.put(tag, tagFreq.getOrDefault(tag, 0) + 1);
            }
        }
        
        // Select most frequent tag for each word
        for (Map.Entry<String, Map<String, Integer>> entry : wordTagFreq.entrySet()) {
            String word = entry.getKey();
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
                wordToTagMap.put(word, mostFrequentTag);
            }
        }
    }
    
    @Override
    public String predict(List<TaggerWord> sentence, int position) {
        String word = sentence.get(position).getWord();
        String wordLower = word.toLowerCase();
        
        // If the word is in our vocabulary, return the most frequent tag
        if (wordToTagMap.containsKey(wordLower)) {
            Map<String, Integer> tagDistribution = wordTagFreq.get(wordLower);
            String unigramTag = wordToTagMap.get(wordLower);
            
            // Check confidence - if very confident, just use the unigram prediction
            if (isHighConfidence(tagDistribution, unigramTag)) {
                return unigramTag;
            }
        }
        
        // Special handling for punctuation
        if (WordShapeUtil.isPunctuation(word)) {
            return "PUNCT";
        }
        
        // Special handling for numbers
        if (WordShapeUtil.isNumeric(word)) {
            return "NUM";
        }
        
        // Try to guess based on word shape and suffixes
        String wordShape = WordShapeUtil.getWordShape(word);
        
        // If capitalized and not at the beginning of the sentence, likely a proper noun
        if (WordShapeUtil.isCapitalized(word) && position > 0) {
            return "NNP";
        }
        
        // Try to guess based on suffixes
        String guessedTag = WordShapeUtil.guessPosFromSuffix(word);
        if (guessedTag != null) {
            return guessedTag;
        }
        
        // Otherwise, backoff to the default tagger
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
        
        // For unigram we want a higher threshold since we have less context
        return totalCount > 0 && (double) predictedCount / totalCount >= 0.85;
    }
    
    @Override
    protected String getContext(List<TaggerWord> sentence, int position) {
        // For unigram tagger, context is just the word itself and its shape
        String word = sentence.get(position).getWord();
        String wordLower = word.toLowerCase();
        String wordShape = WordShapeUtil.getWordShape(word);
        return wordLower + ":" + wordShape;
    }
}