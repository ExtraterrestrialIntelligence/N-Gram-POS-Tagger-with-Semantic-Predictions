import pandas as pd
import re
import os
import string
import nltk
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
from collections import Counter

# Download necessary NLTK data (uncomment first time)
# nltk.download('punkt')
# nltk.download('stopwords')

# Input and output file paths
input_file = '../resources/ner_dataset2.csv'
output_file = '../resources/cleaned_ner_dataset.csv'
semantic_data_file = '../resources/semantic_data.csv'

def clean_data():
    print(f"Loading data from {input_file}...")
    df = pd.read_csv(input_file)
    
    # Make a copy of the original data
    df_cleaned = df.copy()
    
    # Clean Word column
    print("Cleaning text data...")
    df_cleaned['Word'] = df_cleaned['Word'].apply(lambda x: clean_text(x) if pd.notnull(x) else x)
    
    # Remove rows with missing values or empty strings
    print("Removing rows with missing or empty values...")
    df_cleaned = df_cleaned.dropna()
    df_cleaned = df_cleaned[df_cleaned['Word'].str.strip() != '']
    
    # Fix POS tags (ensure consistency)
    df_cleaned['POS'] = df_cleaned['POS'].apply(standardize_pos_tag)
    
    # Add some semantic information based on word co-occurrences
    print("Building semantic relationships...")
    semantic_df = build_semantic_data(df_cleaned)
    
    # Save cleaned data
    print(f"Saving cleaned data to {output_file}...")
    df_cleaned.to_csv(output_file, index=False)
    
    # Save semantic data
    print(f"Saving semantic data to {semantic_data_file}...")
    semantic_df.to_csv(semantic_data_file, index=False)
    
    print("Data cleaning completed!")
    
    # Print statistics
    print(f"Original rows: {len(df)}")
    print(f"Cleaned rows: {len(df_cleaned)}")
    print(f"Rows removed: {len(df) - len(df_cleaned)}")
    print(f"Unique words: {df_cleaned['Word'].nunique()}")
    print(f"Unique POS tags: {df_cleaned['POS'].nunique()}")
    
    # Print some sample semantic relationships
    print("\nSample semantic relationships:")
    sample_semantics = semantic_df.head(10)
    for _, row in sample_semantics.iterrows():
        print(f"{row['Word']} -> {row['Related_Words']}")

def clean_text(text):
    # Convert to string if not already
    text = str(text).lower()
    
    # Remove extra whitespace
    text = re.sub(r'\s+', ' ', text).strip()
    
    # Remove special characters but keep important punctuation
    text = re.sub(r'[^\w\s.,;!?-]', '', text)
    
    # Normalize punctuation spacing
    text = re.sub(r'\s([.,;!?])', r'\1', text)
    
    return text

def standardize_pos_tag(tag):
    # Standardize common POS tags to ensure consistency
    tag_map = {
        # Map any variations to standard Penn Treebank tags
        'NOUN': 'NN',
        'VERB': 'VB',
        'ADJ': 'JJ',
        'ADV': 'RB',
        'ADP': 'IN',
        'DET': 'DT',
        'PRON': 'PRP',
        'CONJ': 'CC',
        'NUM': 'CD',
        'PART': 'RP',
        'INTJ': 'UH'
    }
    
    return tag_map.get(tag, tag)  # Return original if not in map

def build_semantic_data(df):
    # Get all sentences and their words with POS tags
    sentences = []
    current_sentence = []
    current_sentence_id = ""
    
    for _, row in df.iterrows():
        sentence_id = row['Sentence #']
        word = row['Word']
        pos = row['POS']
        
        if sentence_id != current_sentence_id and current_sentence_id != "":
            if current_sentence:
                sentences.append(current_sentence)
            current_sentence = []
        
        current_sentence.append((word, pos))
        current_sentence_id = sentence_id
    
    # Add the last sentence
    if current_sentence:
        sentences.append(current_sentence)
    
    # Find word co-occurrences and semantic relationships
    word_relationships = {}
    
    for sentence in sentences:
        words = [word[0] for word in sentence]
        pos_tags = [word[1] for word in sentence]
        
        # Skip very short sentences
        if len(words) < 3:
            continue
        
        # Process each word in context
        for i, (word, pos) in enumerate(zip(words, pos_tags)):
            if word not in word_relationships:
                word_relationships[word] = {
                    'pos': [],
                    'next_words': [],
                    'prev_words': [],
                    'context': []
                }
            
            # Add the POS tag
            word_relationships[word]['pos'].append(pos)
            
            # Add next word if available
            if i < len(words) - 1:
                word_relationships[word]['next_words'].append(words[i+1])
            
            # Add previous word if available
            if i > 0:
                word_relationships[word]['prev_words'].append(words[i-1])
            
            # Add context words (within a window of 3)
            context = []
            for j in range(max(0, i-3), min(len(words), i+4)):
                if j != i:
                    context.append(words[j])
            
            word_relationships[word]['context'].extend(context)
    
    # Convert to DataFrame format
    semantic_data = []
    
    for word, data in word_relationships.items():
        # Get most common POS tag
        most_common_pos = Counter(data['pos']).most_common(1)[0][0] if data['pos'] else 'UNK'
        
        # Get most common next words
        next_words = Counter(data['next_words']).most_common(5)
        next_words_str = ','.join([f"{w}:{c}" for w, c in next_words]) if next_words else ''
        
        # Get most common previous words
        prev_words = Counter(data['prev_words']).most_common(5)
        prev_words_str = ','.join([f"{w}:{c}" for w, c in prev_words]) if prev_words else ''
        
        # Get most common context words (semantic relationship)
        context_words = Counter(data['context']).most_common(10)
        context_words_str = ','.join([f"{w}:{c}" for w, c in context_words]) if context_words else ''
        
        semantic_data.append({
            'Word': word,
            'Common_POS': most_common_pos,
            'Next_Words': next_words_str,
            'Prev_Words': prev_words_str,
            'Related_Words': context_words_str
        })
    
    return pd.DataFrame(semantic_data)

if __name__ == "__main__":
    # Ensure the paths are relative to the script location
    script_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(script_dir)
    
    clean_data()