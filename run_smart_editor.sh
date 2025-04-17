#!/bin/bash

# Set class path with required libraries
export SRC_DIR="src/main/java"
export OUT_DIR="target/classes"

# Create output directory if it doesn't exist
mkdir -p $OUT_DIR

# Function for printing colored text
print_colored() {
    local color=$1
    local text=$2
    case $color in
        "blue") echo -e "\e[34m$text\e[0m" ;;
        "green") echo -e "\e[32m$text\e[0m" ;;
        "red") echo -e "\e[31m$text\e[0m" ;;
        "yellow") echo -e "\e[33m$text\e[0m" ;;
        *) echo "$text" ;;
    esac
}

# Compile all Java files
print_colored "blue" "Compiling Java files..."
find $SRC_DIR -name "*.java" -print | xargs javac -d $OUT_DIR

# Header
print_colored "green" "════════════════════════════════════════════════════════"
print_colored "green" "    N-Gram POS Tagger with Semantic Predictions"
print_colored "green" "════════════════════════════════════════════════════════"
echo ""

# Display menu
print_colored "yellow" "Choose an option:"
echo "1) Run Smart Semantic Editor"
echo "2) Run Jackknife Evaluation (n-gram taggers up to quad-gram)"
echo "3) Run Interactive Test (simple command-line interface)"
echo "4) Exit"
echo ""
read -p "Enter your choice (1-4): " choice

case $choice in
    1)
        print_colored "blue" "Launching Smart Semantic Editor..."
        print_colored "blue" "This editor provides meaningful predictions based on semantic relationships!"
        echo ""
        java -cp $OUT_DIR com.extraterrestrial.intelligence.gui.SmartEditor
        ;;
    2)
        print_colored "blue" "Running jackknife evaluation..."
        print_colored "blue" "This will evaluate all taggers (uni, bi, tri, and quad-gram) using jackknife cross-validation"
        print_colored "blue" "with 1000 sentences per test fold"
        echo ""
        java -cp $OUT_DIR com.extraterrestrial.intelligence.JackknifeEvaluator
        ;;
    3)
        print_colored "blue" "Running interactive test..."
        print_colored "blue" "Type sentences to see POS tags"
        echo ""
        java -cp $OUT_DIR com.extraterrestrial.intelligence.InteractiveTest
        ;;
    4)
        print_colored "yellow" "Exiting. Goodbye!"
        exit 0
        ;;
    *)
        print_colored "red" "Invalid choice. Exiting."
        exit 1
        ;;
esac