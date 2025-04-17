package com.extraterrestrial.intelligence.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataCleaner {

    private static final String PYTHON_SCRIPT_PATH = "src/main/python/clean_data.py";
    private static final String CLEANED_DATA_PATH = "src/main/resources/cleaned_ner_dataset.csv";

    /**
     * Runs the Python data cleaning script and returns the path to the cleaned data
     * @return Path to the cleaned dataset CSV file
     */
    public static String cleanData() {
        try {
            Path scriptPath = Paths.get(PYTHON_SCRIPT_PATH).toAbsolutePath();
            
            System.out.println("Running data cleaning script: " + scriptPath);
            
            ProcessBuilder processBuilder = new ProcessBuilder("python3", scriptPath.toString());
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // Read and display output from Python script
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Python: " + line);
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("Data cleaning completed successfully.");
                return CLEANED_DATA_PATH;
            } else {
                System.err.println("Data cleaning failed with exit code: " + exitCode);
                return null;
            }
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Error running data cleaning script: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}