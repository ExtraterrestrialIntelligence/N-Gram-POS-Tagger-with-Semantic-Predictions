#!/bin/bash

# Set class path with required libraries
export SRC_DIR="src/main/java"
export OUT_DIR="target/classes"

# Create output directory if it doesn't exist
mkdir -p $OUT_DIR

# Compile all Java files
echo "Compiling Java files..."
find $SRC_DIR -name "*.java" -print | xargs javac -d $OUT_DIR

# Run the beautiful blue editor
echo "Launching the Modern Predictive Editor..."
echo "With a beautiful navy blue design and multi-word prediction!"
echo ""

java -cp $OUT_DIR com.extraterrestrial.intelligence.gui.ModernPredictiveEditor