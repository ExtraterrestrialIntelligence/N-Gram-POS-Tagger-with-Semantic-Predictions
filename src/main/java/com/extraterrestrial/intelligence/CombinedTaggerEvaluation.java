package com.extraterrestrial.intelligence;

import com.extraterrestrial.intelligence.repository.CSVDatasetRepository;
import com.extraterrestrial.intelligence.repository.DatasetRepository;
import com.extraterrestrial.intelligence.service.CombinedTaggerService;

/**
 * Evaluation program for combined tagger approach
 */
public class CombinedTaggerEvaluation {

    public static void main(String[] args) {
        System.out.println("Advanced N-Gram POS Tagger Evaluation with Combined Model");
        System.out.println("========================================================\n");
        
        // Load repository
        DatasetRepository repository = new CSVDatasetRepository();
        
        // Create combined tagger service
        CombinedTaggerService service = new CombinedTaggerService(repository);
        
        // Run evaluation
        service.processAndEvaluate();
    }
}