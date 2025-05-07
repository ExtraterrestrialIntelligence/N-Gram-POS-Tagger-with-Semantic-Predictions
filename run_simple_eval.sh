#!/bin/bash
# Simple evaluation runner script

echo "Running N-Gram POS Tagger Evaluation..."
echo "======================================="

# Ensure target directory exists
mkdir -p target/classes

# Compile the Java files
echo "Compiling Java classes..."
javac -d target/classes -cp src/main/java src/main/java/com/extraterrestrial/intelligence/data/*.java
javac -d target/classes -cp src/main/java:target/classes src/main/java/com/extraterrestrial/intelligence/util/*.java
javac -d target/classes -cp src/main/java:target/classes src/main/java/com/extraterrestrial/intelligence/model/*.java
javac -d target/classes -cp src/main/java:target/classes src/main/java/com/extraterrestrial/intelligence/repository/*.java
javac -d target/classes -cp src/main/java:target/classes src/main/java/com/extraterrestrial/intelligence/*.java

# Run the jackknife evaluator
echo "Running evaluation..."
java -cp target/classes:src/main/resources com.extraterrestrial.intelligence.JackknifeEvaluator

echo "Evaluation complete!"