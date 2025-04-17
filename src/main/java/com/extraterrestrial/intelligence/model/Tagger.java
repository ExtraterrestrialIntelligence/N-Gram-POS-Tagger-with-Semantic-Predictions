package com.extraterrestrial.intelligence.model;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;

import java.util.List;

public interface Tagger {
    /**
     * Train the tagger on a set of tagged sentences
     * @param trainingSentences List of sentences with known tags
     */
    void train(List<TaggedSentence> trainingSentences);
    
    /**
     * Predict the tag for a given word based on context
     * @param sentence The sentence containing the word
     * @param position The position of the word in the sentence
     * @return The predicted tag
     */
    String predict(List<TaggerWord> sentence, int position);
    
    /**
     * Tag all words in a sentence
     * @param sentence The sentence to tag
     * @return A new TaggedSentence with predicted tags
     */
    TaggedSentence tagSentence(TaggedSentence sentence);
    
    /**
     * Evaluate tagger performance on test data
     * @param testSentences List of sentences with known tags to evaluate against
     * @return Accuracy as a percentage
     */
    double evaluate(List<TaggedSentence> testSentences);
}
