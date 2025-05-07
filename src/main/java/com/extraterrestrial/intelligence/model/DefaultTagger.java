package com.extraterrestrial.intelligence.model;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import com.extraterrestrial.intelligence.util.WordShapeUtil;

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
        String word = sentence.get(position).getWord();
        
        // Special handling for punctuation
        if (WordShapeUtil.isPunctuation(word)) {
            return "PUNCT";
        }
        
        // Special handling for numbers
        if (WordShapeUtil.isNumeric(word)) {
            return "NUM";
        }
        
        // Try to guess based on suffixes
        String guessedTag = WordShapeUtil.guessPosFromSuffix(word);
        if (guessedTag != null) {
            return guessedTag;
        }
        
        // If capitalized and not at the beginning of the sentence, likely a proper noun
        if (WordShapeUtil.isCapitalized(word) && position > 0) {
            return "NNP";
        }
        
        return defaultTag;
    }
    
    // Use WordShapeUtil instead of local methods
    
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
