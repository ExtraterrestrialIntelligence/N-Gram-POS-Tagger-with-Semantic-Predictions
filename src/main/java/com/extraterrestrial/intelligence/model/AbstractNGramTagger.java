package com.extraterrestrial.intelligence.model;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for all n-gram taggers
 */
public abstract class AbstractNGramTagger implements Tagger {
    
    protected Tagger backoffTagger;
    
    public AbstractNGramTagger(Tagger backoffTagger) {
        this.backoffTagger = backoffTagger;
    }
    
    @Override
    public TaggedSentence tagSentence(TaggedSentence sentence) {
        List<TaggerWord> originalWords = sentence.getWords();
        List<TaggerWord> taggedWords = new ArrayList<>();
        
        for (int i = 0; i < originalWords.size(); i++) {
            String predictedTag = predict(originalWords, i);
            taggedWords.add(new TaggerWord(originalWords.get(i).getWord(), predictedTag));
        }
        
        return new TaggedSentence(taggedWords);
    }
    
    @Override
    public double evaluate(List<TaggedSentence> testSentences) {
        int totalWords = 0;
        int correctPredictions = 0;
        
        for (TaggedSentence sentence : testSentences) {
            List<TaggerWord> words = sentence.getWords();
            for (int i = 0; i < words.size(); i++) {
                String actualTag = words.get(i).getTag();
                String predictedTag = predict(words, i);
                
                totalWords++;
                if (actualTag.equals(predictedTag)) {
                    correctPredictions++;
                }
            }
        }
        
        return totalWords > 0 ? (double) correctPredictions / totalWords * 100 : 0;
    }
    
    /**
     * Get the context (previous n-1 words and tags) for a given position
     * @param sentence The sentence
     * @param position The current position
     * @return A context string representing the context
     */
    protected abstract String getContext(List<TaggerWord> sentence, int position);
}
