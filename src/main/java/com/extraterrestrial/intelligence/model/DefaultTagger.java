package com.extraterrestrial.intelligence.model;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;

import java.util.ArrayList;
import java.util.List;

/**
 * Default tagger that always returns a fixed tag (NN - noun)
 */
public class DefaultTagger implements Tagger {
    
    private String defaultTag = "NN";
    
    public DefaultTagger() {}
    
    public DefaultTagger(String defaultTag) {
        this.defaultTag = defaultTag;
    }
    
    @Override
    public void train(List<TaggedSentence> trainingSentences) {
        // No training needed for the default tagger
    }
    
    @Override
    public String predict(List<TaggerWord> sentence, int position) {
        return defaultTag;
    }
    
    @Override
    public TaggedSentence tagSentence(TaggedSentence sentence) {
        List<TaggerWord> originalWords = sentence.getWords();
        List<TaggerWord> taggedWords = new ArrayList<>();
        
        for (TaggerWord word : originalWords) {
            taggedWords.add(new TaggerWord(word.getWord(), defaultTag));
        }
        
        return new TaggedSentence(taggedWords);
    }
    
    @Override
    public double evaluate(List<TaggedSentence> testSentences) {
        int totalWords = 0;
        int correctPredictions = 0;
        
        for (TaggedSentence sentence : testSentences) {
            List<TaggerWord> words = sentence.getWords();
            for (TaggerWord word : words) {
                totalWords++;
                if (word.getTag().equals(defaultTag)) {
                    correctPredictions++;
                }
            }
        }
        
        return totalWords > 0 ? (double) correctPredictions / totalWords * 100 : 0;
    }
}
