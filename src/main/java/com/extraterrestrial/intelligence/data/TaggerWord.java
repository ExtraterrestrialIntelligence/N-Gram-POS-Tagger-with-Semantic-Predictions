package com.extraterrestrial.intelligence.data;

public class TaggerWord {
    private String word;
    private String tag;

    public TaggerWord(String word, String tag) {
        this.word = word;
        this.tag = tag;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String toString() {
        return word + "/" + tag;
    }
}
