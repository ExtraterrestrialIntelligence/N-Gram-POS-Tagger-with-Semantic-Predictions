#!/bin/bash

echo "Compiling Java files..."
javac -d target/classes -cp target/classes src/main/java/com/extraterrestrial/intelligence/CombinedTaggerEvaluation.java src/main/java/com/extraterrestrial/intelligence/service/CombinedTaggerService.java

if [ $? -eq 0 ]; then
    echo "Running combined tagger evaluation..."
    echo "This will evaluate a combined tagger model that uses weighted voting from all n-gram models"
    echo ""
    
    java -cp target/classes com.extraterrestrial.intelligence.CombinedTaggerEvaluation
else
    echo "Compilation failed, please check your code."
fi