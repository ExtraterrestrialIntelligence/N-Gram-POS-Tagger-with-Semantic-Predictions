package com.extraterrestrial.intelligence.data;

import java.util.ArrayList;
import java.util.List;

public class TaggedSentence {
    private List<TaggerWord> words;

    public TaggedSentence() {
        this.words = new ArrayList<>();
    }

    public TaggedSentence(List<TaggerWord> words) {
        this.words = words;
    }

    public List<TaggerWord> getWords() {
        return words;
    }

    public void addWord(TaggerWord word) {
        this.words.add(word);
    }

    public int size(){
        return words.size();
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (TaggerWord word : words) {
            sb.append(word.toString()).append(" ");
        }
        return sb.toString().trim();
    }
}
