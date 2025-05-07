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
        
        // Create a working copy that will be updated with predicted tags as we go
        List<TaggerWord> workingCopy = new ArrayList<>();
        for (TaggerWord word : originalWords) {
            // Start with empty tags that will be filled as we process the sentence
            workingCopy.add(new TaggerWord(word.getWord(), ""));
        }
        
        // First pass: Special case handling for known patterns
        preprocessSpecialCases(workingCopy);
        
        // Second pass: Process each word in sequence, using previously assigned tags
        for (int i = 0; i < originalWords.size(); i++) {
            // Special handling for punctuation, numbers, and other special cases
            if (isSpecialCase(workingCopy.get(i).getWord())) {
                String specialCaseTag = getSpecialCaseTag(workingCopy.get(i).getWord());
                workingCopy.set(i, new TaggerWord(originalWords.get(i).getWord(), specialCaseTag));
                taggedWords.add(new TaggerWord(originalWords.get(i).getWord(), specialCaseTag));
                continue;
            }
            
            // Use the normal prediction mechanism for regular words
            String predictedTag = predict(workingCopy, i);
            
            // Update the working copy with the predicted tag
            workingCopy.set(i, new TaggerWord(originalWords.get(i).getWord(), predictedTag));
            
            // Add to the final result
            taggedWords.add(new TaggerWord(originalWords.get(i).getWord(), predictedTag));
        }
        
        // Final pass: Apply post-processing rules to fix common patterns and ensure consistency
        postprocessTags(taggedWords);
        
        return new TaggedSentence(taggedWords);
    }
    
    /**
     * Pre-process special cases in the sentence
     */
    protected void preprocessSpecialCases(List<TaggerWord> sentence) {
        // Special handling for common sequences can be implemented by subclasses
    }
    
    /**
     * Post-process tags to fix common patterns and ensure consistency
     */
    protected void postprocessTags(List<TaggerWord> taggedWords) {
        // Can be overridden by subclasses to apply post-processing rules
    }
    
    /**
     * Check if a word is a special case that needs custom handling
     */
    protected boolean isSpecialCase(String word) {
        // Check for punctuation
        if (WordShapeUtil.isPunctuation(word)) {
            return true;
        }
        
        // Check for numbers
        if (WordShapeUtil.isNumeric(word)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get the tag for a special case word
     */
    protected String getSpecialCaseTag(String word) {
        // Handle punctuation
        if (WordShapeUtil.isPunctuation(word)) {
            if (word.equals(".")) return ".";
            if (word.equals(",")) return ",";
            if (word.equals(":")) return ":";
            if (word.equals(";")) return ":";
            if (word.equals("!")) return ".";
            if (word.equals("?")) return ".";
            if (word.equals("(") || word.equals(")")) return "-LRB-";
            if (word.equals("[") || word.equals("]")) return "-LSB-";
            if (word.equals("{") || word.equals("}")) return "-LCB-";
            return "SYM";
        }
        
        // Handle numbers
        if (WordShapeUtil.isNumeric(word)) {
            return "CD";
        }
        
        // Default fallback
        return "NN";
    }
    
    @Override
    public double evaluate(List<TaggedSentence> testSentences) {
        int totalWords = 0;
        int correctPredictions = 0;
        
        for (TaggedSentence sentence : testSentences) {
            // Tag the entire sentence using the tagger's tagSentence method
            // This ensures we're using the predicted tags for context
            TaggedSentence taggedSentence = tagSentence(sentence);
            
            List<TaggerWord> originalWords = sentence.getWords();
            List<TaggerWord> predictedWords = taggedSentence.getWords();
            
            for (int i = 0; i < originalWords.size(); i++) {
                String actualTag = originalWords.get(i).getTag();
                String predictedTag = predictedWords.get(i).getTag();
                
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