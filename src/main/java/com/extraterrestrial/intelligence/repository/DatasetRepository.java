package com.extraterrestrial.intelligence.repository;

import com.extraterrestrial.intelligence.data.TaggedSentence;

import java.util.List;

public interface DatasetRepository {
    List<TaggedSentence> loadSentences();
}
