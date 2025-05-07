package com.extraterrestrial.intelligence.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for word shape features
 */
public class WordShapeUtil {
    
    // Common English suffixes and their likely POS tags
    private static final Map<String, String> SUFFIX_TO_POS_MAP = new HashMap<>();
    
    // Common closed-class words with their POS tags
    private static final Map<String, String> CLOSED_CLASS_WORDS = new HashMap<>();
    
    // Common proper nouns that appear frequently
    private static final Set<String> COMMON_PROPER_NOUNS = new HashSet<>();
    
    static {
        // Precompiled first names list
        addCommonProperNouns();
        
        // Common closed-class words
        addClosedClassWords();
        
        // ===== SUFFIX MAPPINGS =====
        
        // Noun suffixes
        SUFFIX_TO_POS_MAP.put("tion", "NN");
        SUFFIX_TO_POS_MAP.put("sion", "NN");
        SUFFIX_TO_POS_MAP.put("ment", "NN");
        SUFFIX_TO_POS_MAP.put("ness", "NN");
        SUFFIX_TO_POS_MAP.put("ence", "NN");
        SUFFIX_TO_POS_MAP.put("ance", "NN");
        SUFFIX_TO_POS_MAP.put("ship", "NN");
        SUFFIX_TO_POS_MAP.put("hood", "NN");
        SUFFIX_TO_POS_MAP.put("dom", "NN");
        SUFFIX_TO_POS_MAP.put("ity", "NN");
        SUFFIX_TO_POS_MAP.put("ism", "NN");
        SUFFIX_TO_POS_MAP.put("er", "NN");    // person who does something
        SUFFIX_TO_POS_MAP.put("or", "NN");    // person who does something
        SUFFIX_TO_POS_MAP.put("ist", "NN");   // specialist/professional
        SUFFIX_TO_POS_MAP.put("ian", "NN");   // specialist/professional
        SUFFIX_TO_POS_MAP.put("eer", "NN");   // specialist/professional
        SUFFIX_TO_POS_MAP.put("ant", "NN");   // person who does something
        SUFFIX_TO_POS_MAP.put("ent", "NN");   // person who does something
        SUFFIX_TO_POS_MAP.put("age", "NN");   // collective or process
        SUFFIX_TO_POS_MAP.put("al", "NN");    // action or process
        SUFFIX_TO_POS_MAP.put("ary", "NN");   // thing belonging to
        SUFFIX_TO_POS_MAP.put("ery", "NN");   // state or condition
        SUFFIX_TO_POS_MAP.put("acy", "NN");   // state or quality
        
        // Plural noun suffixes
        SUFFIX_TO_POS_MAP.put("s", "NNS");    // Regular plural
        SUFFIX_TO_POS_MAP.put("es", "NNS");   // Regular plural
        SUFFIX_TO_POS_MAP.put("ies", "NNS");  // Plural for words ending in y
        SUFFIX_TO_POS_MAP.put("ves", "NNS");  // Plural for words ending in f/fe
        
        // Proper noun common endings
        SUFFIX_TO_POS_MAP.put("ton", "NNP");  // Common in place names (Washington, Boston)
        SUFFIX_TO_POS_MAP.put("land", "NNP"); // Common in place names (England, Finland)
        
        // Verb suffixes - expanded list
        SUFFIX_TO_POS_MAP.put("ize", "VB");   // Create/make into
        SUFFIX_TO_POS_MAP.put("ise", "VB");   // British spelling variant
        SUFFIX_TO_POS_MAP.put("ate", "VB");   // Make/cause to be
        SUFFIX_TO_POS_MAP.put("ify", "VB");   // Make/turn into
        SUFFIX_TO_POS_MAP.put("en", "VB");    // Make/cause to be
        
        // Verb forms
        SUFFIX_TO_POS_MAP.put("ing", "VBG");  // Gerund or present participle
        SUFFIX_TO_POS_MAP.put("ed", "VBD");   // Simple past
        SUFFIX_TO_POS_MAP.put("eed", "VBD");  // Irregular past
        SUFFIX_TO_POS_MAP.put("ought", "VBD");// Irregular past (bought, thought)
        SUFFIX_TO_POS_MAP.put("aught", "VBD");// Irregular past (caught, taught)
        SUFFIX_TO_POS_MAP.put("ew", "VBD");   // Irregular past (grew, flew)
        SUFFIX_TO_POS_MAP.put("ame", "VBD");  // Irregular past (came, became)
        SUFFIX_TO_POS_MAP.put("ode", "VBD");  // Irregular past (rode)
        
        SUFFIX_TO_POS_MAP.put("en", "VBN");   // Past participle
        SUFFIX_TO_POS_MAP.put("own", "VBN");  // Irregular past participle (shown, known)
        SUFFIX_TO_POS_MAP.put("aken", "VBN"); // Irregular past participle (taken)
        SUFFIX_TO_POS_MAP.put("itten", "VBN");// Irregular past participle (written)
        
        SUFFIX_TO_POS_MAP.put("s", "VBZ");    // 3rd person singular present
        SUFFIX_TO_POS_MAP.put("es", "VBZ");   // 3rd person singular present
        
        // Adjective suffixes
        SUFFIX_TO_POS_MAP.put("able", "JJ");  // Capable of being
        SUFFIX_TO_POS_MAP.put("ible", "JJ");  // Capable of being
        SUFFIX_TO_POS_MAP.put("ful", "JJ");   // Full of/characterized by
        SUFFIX_TO_POS_MAP.put("less", "JJ");  // Without/lacking
        SUFFIX_TO_POS_MAP.put("ous", "JJ");   // Full of/characterized by
        SUFFIX_TO_POS_MAP.put("ious", "JJ");  // Full of/characterized by
        SUFFIX_TO_POS_MAP.put("eous", "JJ");  // Full of/characterized by
        SUFFIX_TO_POS_MAP.put("ive", "JJ");   // Tending to/having the quality of
        SUFFIX_TO_POS_MAP.put("ative", "JJ"); // Tending to/having the quality of
        SUFFIX_TO_POS_MAP.put("ic", "JJ");    // Having qualities of
        SUFFIX_TO_POS_MAP.put("ical", "JJ");  // Relating to
        SUFFIX_TO_POS_MAP.put("al", "JJ");    // Relating to
        SUFFIX_TO_POS_MAP.put("ial", "JJ");   // Relating to
        SUFFIX_TO_POS_MAP.put("an", "JJ");    // Relating to/belonging to
        SUFFIX_TO_POS_MAP.put("ian", "JJ");   // Relating to/belonging to
        SUFFIX_TO_POS_MAP.put("ish", "JJ");   // Having quality of
        SUFFIX_TO_POS_MAP.put("ese", "JJ");   // Relating to a place
        SUFFIX_TO_POS_MAP.put("esque", "JJ"); // In the style of
        SUFFIX_TO_POS_MAP.put("ent", "JJ");   // Being or doing something
        SUFFIX_TO_POS_MAP.put("ant", "JJ");   // Being or doing something
        
        // Comparative and superlative forms
        SUFFIX_TO_POS_MAP.put("er", "JJR");   // Comparative adjective
        SUFFIX_TO_POS_MAP.put("est", "JJS");  // Superlative adjective
        
        // Adverb suffixes
        SUFFIX_TO_POS_MAP.put("ly", "RB");    // In the manner of
        SUFFIX_TO_POS_MAP.put("ward", "RB");  // In the direction of
        SUFFIX_TO_POS_MAP.put("wards", "RB"); // In the direction of
        SUFFIX_TO_POS_MAP.put("wise", "RB");  // In the manner of
    }
    
    /**
     * Initialize the common proper nouns set
     */
    private static void addCommonProperNouns() {
        // Common countries
        COMMON_PROPER_NOUNS.add("America");
        COMMON_PROPER_NOUNS.add("England");
        COMMON_PROPER_NOUNS.add("France");
        COMMON_PROPER_NOUNS.add("Germany");
        COMMON_PROPER_NOUNS.add("China");
        COMMON_PROPER_NOUNS.add("Japan");
        COMMON_PROPER_NOUNS.add("India");
        COMMON_PROPER_NOUNS.add("Russia");
        COMMON_PROPER_NOUNS.add("Canada");
        COMMON_PROPER_NOUNS.add("Australia");
        
        // Major cities
        COMMON_PROPER_NOUNS.add("London");
        COMMON_PROPER_NOUNS.add("Paris");
        COMMON_PROPER_NOUNS.add("New York");
        COMMON_PROPER_NOUNS.add("Tokyo");
        COMMON_PROPER_NOUNS.add("Beijing");
        COMMON_PROPER_NOUNS.add("Moscow");
        COMMON_PROPER_NOUNS.add("Berlin");
        COMMON_PROPER_NOUNS.add("Rome");
        
        // Common first names
        COMMON_PROPER_NOUNS.add("John");
        COMMON_PROPER_NOUNS.add("Mary");
        COMMON_PROPER_NOUNS.add("James");
        COMMON_PROPER_NOUNS.add("Robert");
        COMMON_PROPER_NOUNS.add("Michael");
        COMMON_PROPER_NOUNS.add("William");
        COMMON_PROPER_NOUNS.add("David");
        COMMON_PROPER_NOUNS.add("Richard");
        COMMON_PROPER_NOUNS.add("Joseph");
        COMMON_PROPER_NOUNS.add("Thomas");
        COMMON_PROPER_NOUNS.add("Sarah");
        COMMON_PROPER_NOUNS.add("Jennifer");
        COMMON_PROPER_NOUNS.add("Elizabeth");
        
        // Common organizations
        COMMON_PROPER_NOUNS.add("Google");
        COMMON_PROPER_NOUNS.add("Microsoft");
        COMMON_PROPER_NOUNS.add("Apple");
        COMMON_PROPER_NOUNS.add("Facebook");
        COMMON_PROPER_NOUNS.add("Amazon");
        COMMON_PROPER_NOUNS.add("Twitter");
        COMMON_PROPER_NOUNS.add("UN");
        COMMON_PROPER_NOUNS.add("WHO");
        COMMON_PROPER_NOUNS.add("NASA");
        COMMON_PROPER_NOUNS.add("FBI");
        COMMON_PROPER_NOUNS.add("CIA");
    }
    
    /**
     * Initialize closed class words
     */
    private static void addClosedClassWords() {
        // Determiners
        CLOSED_CLASS_WORDS.put("the", "DT");
        CLOSED_CLASS_WORDS.put("a", "DT");
        CLOSED_CLASS_WORDS.put("an", "DT");
        CLOSED_CLASS_WORDS.put("this", "DT");
        CLOSED_CLASS_WORDS.put("that", "DT");
        CLOSED_CLASS_WORDS.put("these", "DT");
        CLOSED_CLASS_WORDS.put("those", "DT");
        CLOSED_CLASS_WORDS.put("some", "DT");
        CLOSED_CLASS_WORDS.put("any", "DT");
        CLOSED_CLASS_WORDS.put("each", "DT");
        CLOSED_CLASS_WORDS.put("every", "DT");
        CLOSED_CLASS_WORDS.put("no", "DT");
        CLOSED_CLASS_WORDS.put("another", "DT");
        
        // Prepositions
        CLOSED_CLASS_WORDS.put("in", "IN");
        CLOSED_CLASS_WORDS.put("on", "IN");
        CLOSED_CLASS_WORDS.put("at", "IN");
        CLOSED_CLASS_WORDS.put("by", "IN");
        CLOSED_CLASS_WORDS.put("for", "IN");
        CLOSED_CLASS_WORDS.put("with", "IN");
        CLOSED_CLASS_WORDS.put("about", "IN");
        CLOSED_CLASS_WORDS.put("against", "IN");
        CLOSED_CLASS_WORDS.put("between", "IN");
        CLOSED_CLASS_WORDS.put("into", "IN");
        CLOSED_CLASS_WORDS.put("through", "IN");
        CLOSED_CLASS_WORDS.put("during", "IN");
        CLOSED_CLASS_WORDS.put("before", "IN");
        CLOSED_CLASS_WORDS.put("after", "IN");
        CLOSED_CLASS_WORDS.put("above", "IN");
        CLOSED_CLASS_WORDS.put("below", "IN");
        CLOSED_CLASS_WORDS.put("from", "IN");
        CLOSED_CLASS_WORDS.put("of", "IN");
        CLOSED_CLASS_WORDS.put("to", "TO"); // Special case - 'to' is usually tagged as TO
        
        // Coordinating conjunctions
        CLOSED_CLASS_WORDS.put("and", "CC");
        CLOSED_CLASS_WORDS.put("or", "CC");
        CLOSED_CLASS_WORDS.put("but", "CC");
        CLOSED_CLASS_WORDS.put("nor", "CC");
        CLOSED_CLASS_WORDS.put("so", "CC");
        CLOSED_CLASS_WORDS.put("yet", "CC");
        
        // Subordinating conjunctions
        CLOSED_CLASS_WORDS.put("although", "IN");
        CLOSED_CLASS_WORDS.put("because", "IN");
        CLOSED_CLASS_WORDS.put("since", "IN");
        CLOSED_CLASS_WORDS.put("unless", "IN");
        CLOSED_CLASS_WORDS.put("whereas", "IN");
        CLOSED_CLASS_WORDS.put("while", "IN");
        
        // Personal pronouns
        CLOSED_CLASS_WORDS.put("i", "PRP");
        CLOSED_CLASS_WORDS.put("you", "PRP");
        CLOSED_CLASS_WORDS.put("he", "PRP");
        CLOSED_CLASS_WORDS.put("she", "PRP");
        CLOSED_CLASS_WORDS.put("it", "PRP");
        CLOSED_CLASS_WORDS.put("we", "PRP");
        CLOSED_CLASS_WORDS.put("they", "PRP");
        CLOSED_CLASS_WORDS.put("me", "PRP");
        CLOSED_CLASS_WORDS.put("him", "PRP");
        CLOSED_CLASS_WORDS.put("her", "PRP");
        CLOSED_CLASS_WORDS.put("us", "PRP");
        CLOSED_CLASS_WORDS.put("them", "PRP");
        
        // Possessive pronouns
        CLOSED_CLASS_WORDS.put("my", "PRP$");
        CLOSED_CLASS_WORDS.put("your", "PRP$");
        CLOSED_CLASS_WORDS.put("his", "PRP$");
        CLOSED_CLASS_WORDS.put("her", "PRP$");
        CLOSED_CLASS_WORDS.put("its", "PRP$");
        CLOSED_CLASS_WORDS.put("our", "PRP$");
        CLOSED_CLASS_WORDS.put("their", "PRP$");
        
        // Be verbs
        CLOSED_CLASS_WORDS.put("am", "VBP");
        CLOSED_CLASS_WORDS.put("is", "VBZ");
        CLOSED_CLASS_WORDS.put("are", "VBP");
        CLOSED_CLASS_WORDS.put("was", "VBD");
        CLOSED_CLASS_WORDS.put("were", "VBD");
        CLOSED_CLASS_WORDS.put("be", "VB");
        CLOSED_CLASS_WORDS.put("being", "VBG");
        CLOSED_CLASS_WORDS.put("been", "VBN");
        
        // Common modal verbs
        CLOSED_CLASS_WORDS.put("can", "MD");
        CLOSED_CLASS_WORDS.put("could", "MD");
        CLOSED_CLASS_WORDS.put("may", "MD");
        CLOSED_CLASS_WORDS.put("might", "MD");
        CLOSED_CLASS_WORDS.put("must", "MD");
        CLOSED_CLASS_WORDS.put("shall", "MD");
        CLOSED_CLASS_WORDS.put("should", "MD");
        CLOSED_CLASS_WORDS.put("will", "MD");
        CLOSED_CLASS_WORDS.put("would", "MD");
        
        // Common adverbs
        CLOSED_CLASS_WORDS.put("very", "RB");
        CLOSED_CLASS_WORDS.put("too", "RB");
        CLOSED_CLASS_WORDS.put("quite", "RB");
        CLOSED_CLASS_WORDS.put("rather", "RB");
        CLOSED_CLASS_WORDS.put("almost", "RB");
        CLOSED_CLASS_WORDS.put("always", "RB");
        CLOSED_CLASS_WORDS.put("never", "RB");
        CLOSED_CLASS_WORDS.put("often", "RB");
        CLOSED_CLASS_WORDS.put("sometimes", "RB");
        CLOSED_CLASS_WORDS.put("rarely", "RB");
        CLOSED_CLASS_WORDS.put("seldom", "RB");
        CLOSED_CLASS_WORDS.put("really", "RB");
        CLOSED_CLASS_WORDS.put("generally", "RB");
        CLOSED_CLASS_WORDS.put("usually", "RB");
        CLOSED_CLASS_WORDS.put("just", "RB");
    }
    
    /**
     * Get the shape of a word (punctuation, number, capitalization, etc.)
     */
    public static String getWordShape(String word) {
        if (word == null || word.isEmpty()) return "";
        
        if (isPunctuation(word)) {
            return "PUNCT";
        }
        
        if (isNumeric(word)) {
            // Enhanced numeric classification
            if (word.contains(".") || word.contains(",")) {
                return "DECIMAL";
            }
            if (word.length() == 4 && Integer.parseInt(word) >= 1900 && Integer.parseInt(word) <= 2100) {
                return "YEAR";  // Likely a year
            }
            return "NUM";
        }
        
        // Check for hyphenated words
        if (word.contains("-")) {
            return "HYPHEN";
        }
        
        // Check for mixed case with numbers (often usernames, IDs, codes)
        if (word.matches(".*\\d+.*") && !word.matches("\\d+.*")) {
            return "ALPHANUMERIC";
        }
        
        if (isAllCaps(word)) {
            return "ALLCAPS";
        }
        
        if (isCapitalized(word)) {
            if (word.length() <= 2) {
                return "CAP_SHORT";  // Short capitalized words often initials
            }
            return "CAP";
        }
        
        // Check for camelCase or PascalCase
        if (isCamelCase(word)) {
            return "CAMEL";
        }
        
        // Special cases for common word types
        if (word.endsWith("ly") && word.length() > 4) {
            return "ADVERB";  // Likely adverb
        }
        
        if (word.endsWith("ed") && word.length() > 4) {
            return "PAST";  // Likely past tense
        }
        
        if (word.endsWith("ing") && word.length() > 5) {
            return "GERUND";  // Likely gerund/present participle
        }
        
        // Default word
        return "WORD";
    }
    
    /**
     * Check if a word uses camelCase or PascalCase
     */
    public static boolean isCamelCase(String word) {
        return word.matches("[a-zA-Z]*[a-z]+[A-Z]+[a-zA-Z]*");
    }
    
    /**
     * Check if a word consists entirely of punctuation
     */
    public static boolean isPunctuation(String word) {
        return word.matches("[\\p{Punct}]+");
    }
    
    /**
     * Check if a word is numeric (integer or decimal)
     */
    public static boolean isNumeric(String word) {
        return word.matches("\\d+") || word.matches("\\d+[.,]\\d+");
    }
    
    /**
     * Check if a word is all capital letters
     */
    public static boolean isAllCaps(String word) {
        return word.matches("[A-Z]+");
    }
    
    /**
     * Check if a word is capitalized (first letter uppercase)
     */
    public static boolean isCapitalized(String word) {
        return word.length() > 0 && Character.isUpperCase(word.charAt(0));
    }
    
    /**
     * Guess the POS tag of a word based on its suffix
     * @param word The word to analyze
     * @return The most likely POS tag based on suffix, or null if no match
     */
    /**
     * Check if word is a common proper noun
     */
    public static boolean isCommonProperNoun(String word) {
        return word != null && COMMON_PROPER_NOUNS.contains(word);
    }
    
    /**
     * Get POS for closed class words
     */
    public static String getClosedClassTag(String word) {
        if (word == null || word.isEmpty()) return null;
        return CLOSED_CLASS_WORDS.get(word.toLowerCase());
    }
    
    /**
     * Guess the POS tag of a word based on various features
     */
    public static String guessPosFromWord(String word) {
        if (word == null || word.isEmpty()) return null;
        
        // First check if it's a closed class word
        String closedClassTag = getClosedClassTag(word);
        if (closedClassTag != null) {
            return closedClassTag;
        }
        
        // Check if it's a proper noun (capitalized not at sentence beginning)
        if (isCapitalized(word) && !word.equals("I")) {
            if (isCommonProperNoun(word)) {
                return "NNP"; // Known proper noun
            }
            // This looks like a proper noun
            return "NNP";
        }
        
        // Try suffix-based matching for regular words
        return guessPosFromSuffix(word);
    }
    
    /**
     * Guess the POS tag of a word based on its suffix
     */
    public static String guessPosFromSuffix(String word) {
        if (word == null || word.isEmpty()) return null;
        
        String lowerWord = word.toLowerCase();
        
        // Check for closed class words first
        String closedClassTag = getClosedClassTag(lowerWord);
        if (closedClassTag != null) {
            return closedClassTag;
        }
        
        // Sort suffixes by length (longest first) to prioritize longer matches
        List<Map.Entry<String, String>> suffixEntries = new ArrayList<>(SUFFIX_TO_POS_MAP.entrySet());
        suffixEntries.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        
        // Try all suffixes, prioritizing longer ones
        for (Map.Entry<String, String> entry : suffixEntries) {
            String suffix = entry.getKey();
            if (lowerWord.endsWith(suffix) && lowerWord.length() > suffix.length()) {
                return entry.getValue();
            }
        }
        
        // Additional heuristic rules for when no suffix matches
        
        // Very short words
        if (lowerWord.length() <= 2) {
            if (lowerWord.equals("mr") || lowerWord.equals("dr") || lowerWord.equals("ms")) {
                return "NNP"; // Titles
            }
            return "FW";  // Short words are often foreign words or abbreviations
        }
        
        // Words that look like plural nouns
        if (lowerWord.endsWith("s") && !lowerWord.endsWith("ss") && !lowerWord.endsWith("is") && 
            !lowerWord.endsWith("us") && !lowerWord.endsWith("os")) {
            return "NNS";  // Plural noun (not ending in ss/is/us/os)
        }
        
        // Potential verb forms
        if (lowerWord.endsWith("e") && lowerWord.length() > 3) {
            return "VB";  // Many base form verbs end in 'e'
        }
        
        // Default to NN for unknown words as a fallback
        return "NN";  // Most unknown words are likely to be nouns
    }
}