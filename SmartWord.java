/*

 Note: The previous submission only had the code, here is the file with proper annotations and comments.

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

import java.io.*;
import java.util.*;

public class SmartWord {
    
    // A Trie is an efficient data structure for word prediction:
    private final Trie trie;
    
    // Tracks the frequency of each word:
    private final Map<String, Integer> wordFrequencyMap = new HashMap<>();
    
    // Tracks the frequency of word pairs for context-based predictions:
    private final Map<String, Map<String, Integer>> bigramFrequencyMap = new HashMap<>();
    
    // Stores the current prefix being typed by the user:
    private final StringBuilder currentWordPrefix = new StringBuilder();

    // 3 guesses from SmartWord:
    private final String[] guesses = new String[3];

    // Constructor: Initializes the Trie with words from a file and the words are converted to lowercase and stored in the Trie and frequency map: 
    public SmartWord (final String wordFile) {
        trie = new Trie();
        try (final BufferedReader br = new BufferedReader(new FileReader(wordFile))) {
            String word;
            while ((word = br.readLine()) != null) {
                word = word.toLowerCase().trim();
                if (!word.isEmpty()) {
                    trie.insert(word);
                    wordFrequencyMap.put(word, 1);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading word file: " + e.getMessage());
        }
    }
    
    // Processes an old message file to learn from past user behavior and extracts individual words and word pairs (bigrams) for frequency analysis:
    public void processOldMessages(final String oldMessageFile) {
    try (BufferedReader br = new BufferedReader(new FileReader(oldMessageFile))) {
        String prevWord = null;
        char[] buffer = new char[1024]; // Read chunks for efficiency
        StringBuilder wordBuilder = new StringBuilder();
        int read;

        while ((read = br.read(buffer)) != -1) {
            for (int i = 0; i < read; i++) {
                char c = Character.toLowerCase(buffer[i]);
                if (Character.isLetter(c)) {
                    wordBuilder.append(c);
                } else {
                    if (wordBuilder.length() > 0) {
                        String word = wordBuilder.toString();
                        wordBuilder.setLength(0); // Clear for next word

                        // Insert word into Trie and update frequency maps
                        trie.insert(word);
                        wordFrequencyMap.merge(word, 1, Integer::sum);

                        if (prevWord != null) {
                            bigramFrequencyMap
                                .computeIfAbsent(prevWord, k -> new HashMap<>())
                                .merge(word, 1, Integer::sum);
                        }
                        prevWord = word;
                    }
                    // Reset previous word on newlines or punctuation
                    if (c == '\n') prevWord = null;
                }
            }
        }

        // Handle any trailing word in the buffer
        if (wordBuilder.length() > 0) {
            String word = wordBuilder.toString();
            trie.insert(word);
            wordFrequencyMap.merge(word, 1, Integer::sum);
            if (prevWord != null) {
                bigramFrequencyMap
                    .computeIfAbsent(prevWord, k -> new HashMap<>())
                    .merge(word, 1, Integer::sum);
            }
        }
    } catch (IOException e) {
        System.err.println("Error processing old messages: " + e.getMessage());
    }
}

    

    // Collects all words from a TrieNode that match the given prefix and they are added to the priority queue in lexicographical order:
    private void collectWords (final TrieNode node, final String prefix, final PriorityQueue<String> matches) {
        if (node.isWord) matches.offer(prefix);
        for (int i = 0; i < 26; i++) {
            if (node.children[i] != null) {
                final char nextChar = (char) ('a' + i);
                collectWords(node.children[i], prefix + nextChar, matches);
            }
        }
    }

    // Predicts up to three word suggestions based on the current prefix and the suggestions are ranked by frequency and lexicographical order:
    public String[] guess (final char letter, final int letterPosition, final int wordPosition) {
        if (letterPosition == 0) currentWordPrefix.setLength(0);
        currentWordPrefix.append(letter);

        final TrieNode node = trie.findNode(currentWordPrefix.toString());
        if (node == null) {
            Arrays.fill(guesses, null);
            return guesses;
        }

        // Priority queue to store words sorted by frequency and lexicographical order:
        final PriorityQueue<String> matches = new PriorityQueue<>((a, b) -> {
            final int freqDiff = wordFrequencyMap.getOrDefault(b, 0) - wordFrequencyMap.getOrDefault(a, 0);
            return freqDiff != 0 ? freqDiff : a.compareTo(b);
        });

        collectWords(node, currentWordPrefix.toString(), matches);

        // Populate the guesses array with up to 3 suggestions:
        for (int i = 0; i < 3; i++) {
            guesses[i] = matches.isEmpty() ? null : matches.poll();
        }
        return guesses;
    }  

    // Updates word frequencies based on feedback from the user and rewards or penalizes words to improve future predictions:
    public void feedback (final boolean isCorrectGuess, final String correctWord) {
        if (isCorrectGuess && correctWord != null) {
            wordFrequencyMap.put(correctWord, wordFrequencyMap.getOrDefault(correctWord, 0) + 5);
        } else if (correctWord != null) {
            wordFrequencyMap.put(correctWord, wordFrequencyMap.getOrDefault(correctWord, 0) + 2);
            for (final String guess : guesses) {
                if (guess != null && !guess.equals(correctWord)) {
                    wordFrequencyMap.put(guess, Math.max(0, wordFrequencyMap.get(guess) - 1));
                }
            }
        }
    }  

    // Trie and TrieNode classes:
    private static class Trie {
        private final TrieNode root = new TrieNode();

        public void insert (final String word) {
            TrieNode node = root;
            for (final char c : word.toCharArray()) {
                if (c < 'a' || c > 'z') { // Ignore non-alphabetic characters
                    continue;
                }
                final int index = c - 'a';
                if (node.children[index] == null) {
                    node.children[index] = new TrieNode();
                }
                node = node.children[index];
            }
            node.isWord = true;
        }     
     
        public TrieNode findNode (final String prefix) {
            TrieNode node = root;
            for (final char c : prefix.toCharArray()) {
                if (c < 'a' || c > 'z') { // Ignore invalid characters
                    return null; // Stop searching if an invalid character is found
                }
                final int index = c - 'a';
                node = node.children[index];
                if (node == null) return null;
            }
            return node;
        }
    }

    private static class TrieNode {
        private final TrieNode[] children = new TrieNode[26];
        private boolean isWord;
    }
  
    private static String sanitize (final String input) {
        return input.toLowerCase().replaceAll("[^a-z]", ""); // Retain only a-z
    } 
}
