# N-Gram POS Tagger with Semantic Predictions

A Part-of-Speech (POS) tagging system using n-gram models with backoff and semantic-aware prediction capabilities.

## Features

- **Multiple Tagging Models**:
  - Default Tagger (baseline)
  - Unigram Tagger (based on word frequencies)
  - Bigram Tagger (considers previous word's tag)
  - Trigram Tagger (considers previous two words' tags)
  - Quadgram Tagger (considers previous three words' tags)

- **Backoff Architecture**:
  - Each tagger uses the previous level as a fallback
  - Creates a robust tagging system that handles unseen contexts

- **Semantic Prediction**:
  - Analyzes word relationships in context
  - Provides meaningful multi-word predictions
  - Considers word frequencies and co-occurrence patterns

- **Multiple Interfaces**:
  - Smart GUI Editor with real-time predictions
  - Simple command-line interactive tester
  - Comprehensive jackknife evaluation tool

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 11 or higher

### Running the Application

Run the main script with:

```bash
./run_smart_editor.sh
```

This will provide several options:

1. **Smart Semantic Editor**: A GUI editor with real-time predictions
2. **Jackknife Evaluation**: Performance analysis of all tagging models
3. **Interactive Test**: Command-line interface for testing the taggers
4. **Exit**: Exit the program

## Components

### Data Model

- `TaggedSentence`: Represents a sentence with tagged words
- `TaggerWord`: Represents a word with its POS tag

### Tagging Models

- `Tagger`: Interface for all tagging models
- `DefaultTagger`: Always returns a default tag (baseline)
- `UniGramTagger`: Uses word frequencies
- `BiGramTagger`: Uses previous word's tag + current word
- `TriGramTagger`: Uses previous two words' tags + current word
- `QuadGramTagger`: Uses previous three words' tags + current word

### Semantic Model

- `SemanticModel`: Analyzes word relationships for prediction
- Builds n-gram and semantic relationship models
- Provides weighted random prediction for natural language generation

### User Interfaces

- `SmartEditor`: GUI editor with semantic predictions
- `InteractiveTest`: Command-line interface for testing
- `JackknifeEvaluator`: Performance evaluation tool

## Evaluation

The system is evaluated using a jackknife procedure:
- Systematically removes 1000 sentences at a time as test sets
- Trains on the remaining sentences
- Reports accuracy for each model
- Calculates average performance across all folds

## Dataset

The system uses the NER dataset (ner_dataset2.csv) which contains:
- Sentences with their words and POS tags
- Spans multiple domains and topics
- Contains a wide variety of grammatical structures

## Implementation Notes

- N-gram models with backoff follow the NLTK implementation pattern
- Semantic model uses frequency-weighted co-occurrence statistics
- GUI design uses navy blue and royal blue color scheme for visual appeal