package com.extraterrestrial.intelligence.model;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unigram tagger that assigns tags based on word frequencies
 */
public class UniGramTagger extends AbstractNGramTagger {
    
    private Map<String, String> wordToTagMap;
    
    public UniGramTagger(Tagger backoffTagger) {
        super(backoffTagger);
        this.wordToTagMap = new HashMap<>();
    }
    
    @Override
    public void train(List<TaggedSentence> trainingSentences) {
        // Clear previous training data
        wordToTagMap.clear();
        
        // Count word-tag frequencies
        Map<String, Map<String, Integer>> wordTagFreq = new HashMap<>();
        
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
        String word = sentence.get(position).getWord().toLowerCase();
        
        // If the word is in our vocabulary, return the most frequent tag
        if (wordToTagMap.containsKey(word)) {
            return wordToTagMap.get(word);
        }
        
        // Otherwise, backoff to the default tagger
        return backoffTagger.predict(sentence, position);
    }
    
    @Override
    protected String getContext(List<TaggerWord> sentence, int position) {
        // For unigram tagger, context is just the word itself
        return sentence.get(position).getWord().toLowerCase();
    }
}