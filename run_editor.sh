#!/bin/bash

# Set class path with required libraries
export SRC_DIR="src/main/java"
export OUT_DIR="target/classes"

# Create output directory if it doesn't exist
mkdir -p $OUT_DIR

# Compile all Java files
echo "Compiling Java files..."
find $SRC_DIR -name "*.java" -print | xargs javac -d $OUT_DIR

# Run the GUI editor
echo "Launching predictive text editor..."
java -cp $OUT_DIR com.extraterrestrial.intelligence.gui.PredictiveEditor