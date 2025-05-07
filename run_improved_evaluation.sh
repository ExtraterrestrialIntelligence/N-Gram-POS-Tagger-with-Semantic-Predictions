#!/bin/bash
# Run the improved n-gram tagger evaluation

echo "Compiling and running improved tagger evaluation..."
echo "=================================="

# Use direct Java commands instead of Maven
echo "Compiling Java classes..."
javac -d target/classes -cp src/main/java src/main/java/com/extraterrestrial/intelligence/data/*.java src/main/java/com/extraterrestrial/intelligence/util/*.java src/main/java/com/extraterrestrial/intelligence/model/*.java src/main/java/com/extraterrestrial/intelligence/repository/*.java src/main/java/com/extraterrestrial/intelligence/*.java

# Run the evaluation
echo "Running evaluation..."
java -cp target/classes:src/main/resources com.extraterrestrial.intelligence.JackknifeEvaluator

echo "Evaluation complete!"