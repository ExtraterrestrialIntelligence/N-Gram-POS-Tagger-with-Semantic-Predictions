package com.extraterrestrial.intelligence.model;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UniGramTaggerTest {

    private UniGramTagger tagger;
    private DefaultTagger defaultTagger;
    private List<TaggedSentence> trainingSentences;
    private List<TaggedSentence> testSentences;

    @BeforeEach
    void setUp() {
        // Initialize the taggers
        defaultTagger = new DefaultTagger("UNK");
        tagger = new UniGramTagger(defaultTagger);
        
        // Create training data
        trainingSentences = new ArrayList<>();
        
        // Training sentence 1: "The cat sat on the mat"
        List<TaggerWord> trainingWords1 = Arrays.asList(
            new TaggerWord("The", "DT"),
            new TaggerWord("cat", "NN"),
            new TaggerWord("sat", "VBD"),
            new TaggerWord("on", "IN"),
            new TaggerWord("the", "DT"),
            new TaggerWord("mat", "NN")
        );
        trainingSentences.add(new TaggedSentence(trainingWords1));
        
        // Training sentence 2: "A dog chased the cat"
        List<TaggerWord> trainingWords2 = Arrays.asList(
            new TaggerWord("A", "DT"),
            new TaggerWord("dog", "NN"),
            new TaggerWord("chased", "VBD"),
            new TaggerWord("the", "DT"),
            new TaggerWord("cat", "NN")
        );
        trainingSentences.add(new TaggedSentence(trainingWords2));
        
        // Train the tagger
        tagger.train(trainingSentences);
        
        // Create test data
        testSentences = new ArrayList<>();
        
        // Test sentence: "The dog sat on the floor"
        List<TaggerWord> testWords = Arrays.asList(
            new TaggerWord("The", "DT"),
            new TaggerWord("dog", "NN"),
            new TaggerWord("sat", "VBD"),
            new TaggerWord("on", "IN"),
            new TaggerWord("the", "DT"),
            new TaggerWord("floor", "NN")
        );
        testSentences.add(new TaggedSentence(testWords));
    }

    @Test
    void testTrainAndPredict() {
        // Test prediction for words seen in training
        List<TaggerWord> sentence = testSentences.get(0).getWords();
        
        // "The" appeared as "DT" in training
        assertEquals("DT", tagger.predict(sentence, 0));
        // "dog" appeared as "NN" in training
        assertEquals("NN", tagger.predict(sentence, 1));
        // "sat" appeared as "VBD" in training
        assertEquals("VBD", tagger.predict(sentence, 2));
        // "on" appeared as "IN" in training
        assertEquals("IN", tagger.predict(sentence, 3));
        // "the" appeared as "DT" in training
        assertEquals("DT", tagger.predict(sentence, 4));
        // "floor" didn't appear in training, should backoff to default tag
        assertEquals("UNK", tagger.predict(sentence, 5));
    }

    @Test
    void testTagSentence() {
        TaggedSentence originalSentence = testSentences.get(0);
        TaggedSentence taggedSentence = tagger.tagSentence(originalSentence);
        
        // Check that the words are tagged correctly
        List<TaggerWord> taggedWords = taggedSentence.getWords();
        
        assertEquals("DT", taggedWords.get(0).getTag()); // "The"
        assertEquals("NN", taggedWords.get(1).getTag()); // "dog"
        assertEquals("VBD", taggedWords.get(2).getTag()); // "sat"
        assertEquals("IN", taggedWords.get(3).getTag()); // "on"
        assertEquals("DT", taggedWords.get(4).getTag()); // "the"
        assertEquals("UNK", taggedWords.get(5).getTag()); // "floor" (unseen)
    }

    @Test
    void testEvaluate() {
        // 5 out of 6 words should be tagged correctly
        // Expected accuracy: (5/6) * 100 = 83.33%
        double accuracy = tagger.evaluate(testSentences);
        assertEquals(83.33, accuracy, 0.01);
    }
    
    @Test
    void testCaseInsensitivity() {
        // Create a sentence with different case
        List<TaggerWord> words = Arrays.asList(
            new TaggerWord("THE", "DT"),
            new TaggerWord("DOG", "NN")
        );
        TaggedSentence sentence = new TaggedSentence(words);
        
        // Test that prediction is case-insensitive
        assertEquals("DT", tagger.predict(words, 0)); // "THE" should match "The"
        assertEquals("NN", tagger.predict(words, 1)); // "DOG" should match "dog"
    }
}