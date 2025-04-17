package com.extraterrestrial.intelligence.model;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trigram tagger that assigns tags based on the previous two words' tags
 */
public class TriGramTagger extends AbstractNGramTagger {
    
    private Map<String, String> contextToTagMap;
    
    public TriGramTagger(Tagger backoffTagger) {
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
        
        // Otherwise, backoff to the bigram tagger
        return backoffTagger.predict(sentence, position);
    }
    
    @Override
    protected String getContext(List<TaggerWord> sentence, int position) {
        // For a trigram tagger, context is the previous two words' tags and current word
        if (position > 1) {
            String prevTag2 = sentence.get(position - 2).getTag();
            String prevTag1 = sentence.get(position - 1).getTag();
            String word = sentence.get(position).getWord().toLowerCase();
            return prevTag2 + ":" + prevTag1 + ":" + word;
        } else if (position > 0) {
            // If only one previous word exists
            String prevTag = sentence.get(position - 1).getTag();
            String word = sentence.get(position).getWord().toLowerCase();
            return "START:" + prevTag + ":" + word;
        } else {
            // If at the beginning of the sentence
            return "START:START:" + sentence.get(position).getWord().toLowerCase();
        }
    }
}
