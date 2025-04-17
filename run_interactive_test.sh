#!/bin/bash

# Set class path with required libraries
export SRC_DIR="src/main/java"
export OUT_DIR="target/classes"

# Create output directory if it doesn't exist
mkdir -p $OUT_DIR

# Compile all Java files
echo "Compiling Java files..."
find $SRC_DIR -name "*.java" -print | xargs javac -d $OUT_DIR

# Run the interactive test
echo "Running tests..."
echo "Choose which test to run:"
echo "1) POS tagger test (tag full sentences)"
echo "2) Word prediction test (predict next words)"
echo ""
read -p "Enter your choice (1 or 2): " choice

if [ "$choice" == "1" ]; then
    echo "Running POS tagger test..."
    java -cp $OUT_DIR com.extraterrestrial.intelligence.InteractiveTest
elif [ "$choice" == "2" ]; then
    echo "Running word prediction test..."
    java -cp $OUT_DIR com.extraterrestrial.intelligence.WordPredictionTest
else
    echo "Invalid choice. Exiting."
fi