#!/bin/bash
# Script to run the improved high-accuracy tagger

echo "Running High-Accuracy POS Tagger (99% Target)"
echo "=============================================="

# Create target directory if it doesn't exist
mkdir -p target/classes

# Compile the code in the correct order to handle dependencies
echo "Compiling Java classes..."
javac -d target/classes src/main/java/com/extraterrestrial/intelligence/data/*.java
javac -d target/classes -cp target/classes src/main/java/com/extraterrestrial/intelligence/util/*.java
javac -d target/classes -cp target/classes src/main/java/com/extraterrestrial/intelligence/model/*.java
javac -d target/classes -cp target/classes src/main/java/com/extraterrestrial/intelligence/repository/*.java
javac -d target/classes -cp target/classes src/main/java/com/extraterrestrial/intelligence/*.java

# Check compilation status
if [ $? -eq 0 ]; then
    echo "Compilation successful."
    
    # Run the jackknife evaluator
    echo "Running evaluation..."
    java -cp target/classes:src/main/resources com.extraterrestrial.intelligence.JackknifeEvaluator
    
    echo "Evaluation complete!"
else
    echo "Compilation failed. Please check for errors."
fi