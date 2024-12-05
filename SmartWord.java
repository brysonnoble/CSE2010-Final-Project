/*

 Authors (group members): Ashley McKim, Bryson Noble, Calvin Rutherford, Matteo Caruso
 Email addresses of group members: amckim2022@my.fit.edu, bnoble2023@my.fit.edu, crutherford2023@my.fit.edu, mcaruso2023@my.fit.edu
 
 Group name: 34c

 Course: CSE2010
 
 Section: 3 and 4

 Description of the overall algorithm: Autofill function that takes old text recommendations in order to guess how to fill
 words in new texts, overall speeding up the texting process.
 
 Algorithm Division:
 - Matteo: SmartWord Initialization: Initializes a list of English words to use in predictions.
 - Calvin: Old Messages Processing: Processes old messages to adapt suggestions to a user’s past behavior.
 - Bryson: Guess Function: Provides up to 3 word suggestions as a user types.
 - Ashley: Feedback Mechanism: Incorporates feedback to improve the accuracy of future guesses.

*/

import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

// A class representing a smart word suggestion system using a Trie and n-gram frequency maps.
public class SmartWord {

    // Trie data structure for storing and querying words efficiently.
    private final Trie trie;

    // Map to store the frequency of individual words.
    private final Map<String, Integer> wordFrequencyMap = new HashMap<>();

    // Map to store bigram (pair of words) frequencies.
    private final Map<String, Map<String, Integer>> bigramFrequencyMap = new HashMap<>();

    // Map to store trigram (three consecutive words) frequencies.
    private final Map<String, Map<String, Map<String, Integer>>> trigramFrequencyMap = new HashMap<>();

    // StringBuilder to hold the current word prefix being processed.
    private final StringBuilder currentWordPrefix = new StringBuilder();

    // Reference to the current node in the Trie, used during word construction.
    private TrieNode currentNode = null;

    // Array to hold up to three word suggestions.
    private final String[] guesses = new String[3];

    // Variables to track the last and second-to-last words processed.
    private String lastWord = null;
    private String secondLastWord = null;

    // Constructor that initializes the Trie and loads vocabulary from a given file.
    public SmartWord(final String wordFile) {
        trie = new Trie();
        loadVocabulary(wordFile);
    }

    // Method to load vocabulary from a specified file.
    private void loadVocabulary(String wordFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(wordFile))) {
            String word;

            // Read words line by line from the file.
            while ((word = br.readLine()) != null) {
                word = word.toLowerCase().trim();

                // If the word is not empty, add it to the frequency map and Trie.
                if (!word.isEmpty()) {
                    wordFrequencyMap.put(word, 1);
                    trie.insert(word, 1);
                }
            }
        } catch (IOException e) {
            // Handle exceptions during file reading.
            System.err.println("Error reading word file: " + e.getMessage());
        }
    }

    // Method to process a file of old messages to update bigram and trigram frequencies.
    public void processOldMessages(final String oldMessageFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(oldMessageFile))) {
            String previousWord = null; 
            String prePreviousWord = null; 
            StringBuilder wordBuilder = new StringBuilder();
            char[] buffer = new char[1024];
            int read;

            // Read the file in chunks.
            while ((read = br.read(buffer)) != -1) {
                for (int i = 0; i < read; i++) {
                    char c = Character.toLowerCase(buffer[i]);

                    // If the character is a letter, append it to the current word being constructed.
                    if (Character.isLetter(c)) {
                        wordBuilder.append(c);
                    } else {
                        // When a non-letter character is encountered, finalize the current word.
                        if (wordBuilder.length() > 0) {
                            String word = wordBuilder.toString();
                            wordBuilder.setLength(0);
                         
                            // Update frequency maps for the current word and its context.
                            updateFrequencies(word, previousWord, prePreviousWord);

                            // Update the word context.
                            prePreviousWord = previousWord;
                            previousWord = word;
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Handle exceptions during file reading.
            System.err.println("Error processing old messages: " + e.getMessage());
        }

 // Updates the frequency maps for the given word and its context (previous and pre-previous words).
private void updateFrequencies(String word, String previousWord, String prePreviousWord) {
    // Update the frequency of the word in the word frequency map.
    wordFrequencyMap.merge(word, 1, Integer::sum);

    // Insert the word into the Trie with its updated frequency.
    trie.insert(word, wordFrequencyMap.get(word));

    // Update the bigram frequency map if there is a previous word.
    if (previousWord != null) {
        // Get or create a map of frequencies for the previous word and merge the current word's count.
        bigramFrequencyMap.computeIfAbsent(previousWord, k -> new HashMap<>())
            .merge(word, 1, Integer::sum);

        // Limit the size of the bigram map to prevent memory overuse.
        limitFrequencyMap(bigramFrequencyMap.get(previousWord));
    }

    // Update the trigram frequency map if there are two previous words.
    if (prePreviousWord != null) {
        // Get or create a nested map for the trigram structure and update the frequency.
        trigramFrequencyMap
            .computeIfAbsent(prePreviousWord, k -> new HashMap<>())
            .computeIfAbsent(previousWord, k -> new HashMap<>())
            .merge(word, 1, Integer::sum);

        // Limit the size of the trigram map for memory efficiency.
        limitFrequencyMap(trigramFrequencyMap.get(prePreviousWord).get(previousWord));
    }
}

// Provides contextual word suggestions based on the previous words.
private List<String> getContextualSuggestions(List<String> suggestions, String[] previousWords) {
    // Determine the n-gram length (up to 4-gram) for context scoring.
    int nGramLength = Math.min(previousWords.length, 4);
    Map<String, Integer> contextualMap = new HashMap<>();

    // Calculate a context-based score for each suggestion.
    for (String suggestion : suggestions) {
        int score = calculateContextScore(suggestion, previousWords, nGramLength);
        contextualMap.put(suggestion, score); 
    }

    // Sort suggestions by their context scores in descending order and return the sorted list.
    return contextualMap.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
}

// Calculates a score for a suggestion based on its presence in the bigram frequency map.
private int calculateContextScore(String suggestion, String[] previousWords, int nGramLength) {
    int score = 0;

    // Add scores from bigram frequency maps for up to n-gram context length.
    for (int i = 0; i < nGramLength; i++) {
        score += bigramFrequencyMap
            .getOrDefault(previousWords[i], new HashMap<>())
            .getOrDefault(suggestion, 0);
    }

    return score; // Return the aggregated score for the suggestion.
}

// Limits the size of a frequency map to a maximum of 50 entries by removing low-frequency entries.
private void limitFrequencyMap(Map<String, Integer> map) {
    if (map.size() > 50) {
        // Remove entries with the lowest frequency to reduce the map size.
        map.entrySet().removeIf(entry -> entry.getValue() < Collections.min(map.values()));
    }
}

// Generates guesses for the current word being typed based on the Trie and context.
public String[] guess(final char letter, final int letterPosition, final int wordPosition) {
    // Reset the prefix and Trie traversal if it's the start of a new word.
    if (letterPosition == 0) {
        currentWordPrefix.setLength(0);
        currentNode = trie.root;
    }

    // Append the letter to the current word prefix.
    currentWordPrefix.append(letter);

    // Validate the letter and update the current Trie node.
    if (letter < 'a' || letter > 'z' || currentNode == null) {
        Arrays.fill(guesses, null); 
        currentNode = null;
        return guesses;
    }

    // Move to the child node corresponding to the current letter.
    int index = letter - 'a';
    currentNode = currentNode.children[index];

    // If the current node is null, no further suggestions are possible.
    if (currentNode == null) {
        Arrays.fill(guesses, null);
        return guesses;
    }

    // Get a list of suggestions from the Trie based on the current prefix.
    List<String> suggestions = trie.getSuggestions(
        currentNode,
        currentWordPrefix.toString(),
        10, 
        wordFrequencyMap
    );

    // Refine suggestions using bigram and trigram context if available.
    if (secondLastWord != null && lastWord != null) {
        suggestions = refineSuggestionsWithContext(suggestions, lastWord, secondLastWord);
    }

    // Populate the guesses array with the top 3 suggestions.
    for (int i = 0; i < 3; i++) {
        guesses[i] = i < suggestions.size() ? suggestions.get(i) : null;
    }

    return guesses; // Return the array of guesses.
}


    // Refines a list of suggestions based on bigram and trigram context scores.
private List<String> refineSuggestionsWithContext(List<String> suggestions, String lastWord, String secondLastWord) {
    // Retrieve bigram scores for the last word.
    Map<String, Integer> bigramScores = bigramFrequencyMap.getOrDefault(lastWord, new HashMap<>());

    // Retrieve trigram scores for the combination of the second-to-last and last words.
    Map<String, Map<String, Integer>> trigramMap = trigramFrequencyMap.getOrDefault(secondLastWord, new HashMap<>());
    Map<String, Integer> trigramScores = trigramMap.getOrDefault(lastWord, new HashMap<>());

    // Sort suggestions based on the combined scores from the bigram and trigram maps.
    suggestions.sort((a, b) -> {
        int trigramA = bigramScores.getOrDefault(a, 0) + trigramScores.getOrDefault(a, 0);
        int trigramB = bigramScores.getOrDefault(b, 0) + trigramScores.getOrDefault(b, 0);
        return trigramB - trigramA;
    });

    return suggestions; // Return the sorted suggestions.
}

// Updates system feedback based on user input and correct word selection.
public void feedback(final boolean isCorrectGuess, final String correctWord) {
    // Validate the correct word; it must be non-null and consist of only lowercase letters.
    if (correctWord == null || !correctWord.matches("^[a-z]+$")) return;

    // Adjust word frequency based on whether the guess was correct.
    int adjustment = isCorrectGuess ? 50 : -2; 
    wordFrequencyMap.put(correctWord, Math.max(0, wordFrequencyMap.getOrDefault(correctWord, 0) + adjustment));

    // Update the Trie to reflect the new word frequency.
    trie.updateBestSuggestions(correctWord, wordFrequencyMap);

    // Update the bigram frequency map using the last word as context.
    if (lastWord != null) {
        bigramFrequencyMap
            .computeIfAbsent(lastWord, k -> new HashMap<>())
            .merge(correctWord, 1, Integer::sum);
    }

    // Update the trigram frequency map using the last two words as context.
    if (secondLastWord != null && lastWord != null) {
        trigramFrequencyMap
            .computeIfAbsent(secondLastWord, k -> new HashMap<>())
            .computeIfAbsent(lastWord, k -> new HashMap<>())
            .merge(correctWord, 1, Integer::sum);
    }

    // Update the context for future guesses.
    secondLastWord = lastWord;
    lastWord = correctWord;
}

// Trie implementation for storing and querying words.
private static class Trie {
    private final TrieNode root = new TrieNode(); 
    // Inserts a word into the Trie along with its frequency.
    public void insert(final String word, final int frequency) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int index = c - 'a'; 
            if (node.children[index] == null) {
                node.children[index] = new TrieNode(); 
            }
            node = node.children[index];
        }
        node.isWord = true; 
        node.frequency = frequency; 
    }

    // Precomputes the best suggestions for each node in the Trie.
    public void precomputeBestSuggestions(Map<String, Integer> wordFrequencyMap) {
        computeBestSuggestions(root, "", wordFrequencyMap);
    }

    // Recursively computes the best suggestions for a Trie node.
    private void computeBestSuggestions(TrieNode node, String prefix, Map<String, Integer> wordFrequencyMap) {
        if (node == null) return;

        // Add the current word to suggestions if it's a complete word.
        if (node.isWord) {
            node.bestSuggestions.add(prefix);
        }

        // Recursively process child nodes and merge their suggestions.
        for (int i = 0; i < 26; i++) {
            if (node.children[i] != null) {
                computeBestSuggestions(node.children[i], prefix + (char) ('a' + i), wordFrequencyMap);

                TrieNode child = node.children[i];
                mergeSuggestions(node.bestSuggestions, child.bestSuggestions, wordFrequencyMap);
            }
        }
    }

    // Retrieves a list of suggestions for a given prefix from the Trie.
    public List<String> getSuggestions(TrieNode node, String prefix, int count, Map<String, Integer> wordFrequencyMap) {
        // Use a priority queue to rank suggestions by frequency.
        PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(
            (a, b) -> b.getValue() - a.getValue()
        );

        // Add suggestions from the node's best suggestions list.
        for (String suggestion : node.bestSuggestions) {
            pq.add(Map.entry(suggestion, wordFrequencyMap.getOrDefault(suggestion, 0)));
        }

        // Collect up to 'count' suggestions in sorted order.
        List<String> results = new ArrayList<>();
        while (!pq.isEmpty() && results.size() < count) {
            results.add(pq.poll().getKey());
        }

        return results;
    }

    // Updates the best suggestions in the Trie for a given word.
    public void updateBestSuggestions(String word, Map<String, Integer> wordFrequencyMap) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int index = c - 'a'; 
            if (node.children[index] == null) return;
            node = node.children[index];

            // Add the word to the node's best suggestions if not already present.
            if (!node.bestSuggestions.contains(word)) {
                node.bestSuggestions.add(word);

                // Maintain a maximum of 10 suggestions, sorted by frequency.
                if (node.bestSuggestions.size() > 10) {
                    node.bestSuggestions.sort((a, b) -> wordFrequencyMap.get(b) - wordFrequencyMap.get(a));
                    node.bestSuggestions.remove(node.bestSuggestions.size() - 1);
                }
            }
        }
    }

    // Merges suggestions from a child node into the parent node's list.
    private void mergeSuggestions(List<String> parent, List<String> child, Map<String, Integer> wordFrequencyMap) {
        Set<String> merged = new HashSet<>(parent); 
        merged.addAll(child);

        parent.clear();
        parent.addAll(
            merged.stream()
                .sorted((a, b) -> wordFrequencyMap.getOrDefault(b, 0) - wordFrequencyMap.getOrDefault(a, 0))
                .limit(10) 
                .toList()
        );
    }
}

// Trie node structure for storing children and metadata about words.
private static class TrieNode {
    private final TrieNode[] children = new TrieNode[26]; 
    private boolean isWord; 
    private int frequency = 0;
    private final List<String> bestSuggestions = new ArrayList<>(3); 
}
