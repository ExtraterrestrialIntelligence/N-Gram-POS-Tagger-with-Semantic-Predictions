package com.extraterrestrial.intelligence.model;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bigram tagger that assigns tags based on the previous word's tag
 */
public class BiGramTagger extends AbstractNGramTagger {
    
    private Map<String, String> contextToTagMap;
    
    public BiGramTagger(Tagger backoffTagger) {
        super(backoffTagger);
        this.contextToTagMap = new HashMap<>();
    }
    
    @Override
    public void train(List<TaggedSentence> trainingSentences) {
        // Clear previous training data
        contextToTagMap.clear();
        
        // Count context-tag frequencies
        Map<String, Map<String, Integer>> contextTagFreq = new HashMap<>();
        
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
        
        // If we have a tag for this context, return it
        if (contextToTagMap.containsKey(context)) {
            return contextToTagMap.get(context);
        }
        
        // Otherwise, backoff to the unigram tagger
        return backoffTagger.predict(sentence, position);
    }
    
    @Override
    protected String getContext(List<TaggerWord> sentence, int position) {
        // For a bigram tagger, context is the previous word's tag and current word
        if (position > 0) {
            String prevTag = sentence.get(position - 1).getTag();
            String word = sentence.get(position).getWord().toLowerCase();
            return prevTag + ":" + word;
        } else {
            // If at the beginning of the sentence, use special START tag
            return "START:" + sentence.get(position).getWord().toLowerCase();
        }
    }
}
