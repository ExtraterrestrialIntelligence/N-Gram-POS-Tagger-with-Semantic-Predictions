#!/bin/bash

# Set class path with required libraries
export SRC_DIR="src/main/java"
export OUT_DIR="target/classes"

# Create output directory if it doesn't exist
mkdir -p $OUT_DIR

# Compile all Java files
echo "Compiling Java files..."
find $SRC_DIR -name "*.java" -print | xargs javac -d $OUT_DIR

# Run the jackknife evaluator
echo "Running jackknife evaluation..."
echo "This will evaluate all taggers (uni, bi, tri, and quad-gram) using jackknife cross-validation"
echo "with 1000 sentences per test fold"
echo ""

java -cp $OUT_DIR com.extraterrestrial.intelligence.JackknifeEvaluator