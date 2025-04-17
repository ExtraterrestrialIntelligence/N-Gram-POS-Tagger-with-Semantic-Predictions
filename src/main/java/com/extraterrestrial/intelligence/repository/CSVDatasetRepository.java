package com.extraterrestrial.intelligence.repository;

import com.extraterrestrial.intelligence.data.TaggedSentence;
import com.extraterrestrial.intelligence.data.TaggerWord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple CSV repository implementation without dependencies
 */
public class CSVDatasetRepository implements DatasetRepository {
    private static final String CSV_FILE_PATH = "src/main/resources/ner_dataset2.csv";

    @Override
    public List<TaggedSentence> loadSentences() {
        List<TaggedSentence> sentences = new ArrayList<>();
        TaggedSentence currentSentence = new TaggedSentence();
        String prevSentenceId = "";
        
        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE_PATH))) {
            String line;
            boolean isFirstLine = true;
            
            // Process each line in the CSV file
            while ((line = reader.readLine()) != null) {
                // Skip header line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                // Parse CSV line (simple split by comma, could be improved)
                String[] parts = line.split(",", 4);
                if (parts.length < 3) {
                    continue; // Skip invalid lines
                }
                
                String sentenceId = parts[0].trim();
                String word = parts[1].trim();
                String tag = parts[2].trim();
                
                // If sentence ID changes, start a new sentence
                if (!sentenceId.equals(prevSentenceId) && !prevSentenceId.isEmpty()) {
                    sentences.add(currentSentence);
                    currentSentence = new TaggedSentence();
                }
                
                currentSentence.addWord(new TaggerWord(word, tag));
                prevSentenceId = sentenceId;
            }
            
            // Add the last sentence
            if (!currentSentence.getWords().isEmpty()) {
                sentences.add(currentSentence);
            }
            
            System.out.println("Loaded " + sentences.size() + " sentences from CSV file: " + CSV_FILE_PATH);
            return sentences;
            
        } catch (IOException e) {
            System.err.println("Error loading CSV file: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}

