package com.extraterrestrial.intelligence.model;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTaggerTest {

    private DefaultTagger tagger;
    private List<TaggedSentence> testSentences;

    @BeforeEach
    void setUp() {
        // Initialize the tagger with a default tag "NN"
        tagger = new DefaultTagger("NN");
        
        // Create some test sentences
        testSentences = new ArrayList<>();
        
        // First sentence: "The cat sat on the mat"
        List<TaggerWord> words1 = Arrays.asList(
            new TaggerWord("The", "DT"),
            new TaggerWord("cat", "NN"),
            new TaggerWord("sat", "VBD"),
            new TaggerWord("on", "IN"),
            new TaggerWord("the", "DT"),
            new TaggerWord("mat", "NN")
        );
        testSentences.add(new TaggedSentence(words1));
        
        // Second sentence: "I love programming"
        List<TaggerWord> words2 = Arrays.asList(
            new TaggerWord("I", "PRP"),
            new TaggerWord("love", "VBP"),
            new TaggerWord("programming", "NN")
        );
        testSentences.add(new TaggedSentence(words2));
    }

    @Test
    void testPredict() {
        List<TaggerWord> sentence = testSentences.get(0).getWords();
        
        // DefaultTagger should always return the default tag
        assertEquals("NN", tagger.predict(sentence, 0));
        assertEquals("NN", tagger.predict(sentence, 1));
        assertEquals("NN", tagger.predict(sentence, 2));
    }

    @Test
    void testTagSentence() {
        TaggedSentence originalSentence = testSentences.get(0);
        TaggedSentence taggedSentence = tagger.tagSentence(originalSentence);
        
        // Check that the words are the same but with default tags
        List<TaggerWord> originalWords = originalSentence.getWords();
        List<TaggerWord> taggedWords = taggedSentence.getWords();
        
        assertEquals(originalWords.size(), taggedWords.size());
        
        for (int i = 0; i < originalWords.size(); i++) {
            assertEquals(originalWords.get(i).getWord(), taggedWords.get(i).getWord());
            assertEquals("NN", taggedWords.get(i).getTag());
        }
    }

    @Test
    void testEvaluate() {
        // In the first sentence, 2 out of 6 words are "NN"
        // In the second sentence, 1 out of 3 words are "NN"
        // Total: 3 out of 9 words are correctly tagged as "NN"
        // Expected accuracy: (3/9) * 100 = 33.33%
        
        double accuracy = tagger.evaluate(testSentences);
        assertEquals(33.33, accuracy, 0.01);
    }
}