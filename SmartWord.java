import java.io.*;
import java.util.*;

public class SmartWord {

    private final Trie trie;
    private final Map<String, Integer> wordFrequencyMap = new HashMap<>();
    private final StringBuilder currentWordPrefix = new StringBuilder();
    private TrieNode currentNode = null;
    private final String[] guesses = new String[3];

    public SmartWord(final String wordFile) {
        trie = new Trie();
        try (BufferedReader br = new BufferedReader(new FileReader(wordFile))) {
            String word;
            while ((word = br.readLine()) != null) {
                word = word.toLowerCase().trim();
                if (!word.isEmpty()) {
                    wordFrequencyMap.put(word, 1);
                    trie.insert(word, 1);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading word file: " + e.getMessage());
        }
        trie.precomputeBestSuggestions(wordFrequencyMap);
    }

    public void processOldMessages(final String oldMessageFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(oldMessageFile))) {
            StringBuilder wordBuilder = new StringBuilder();
            char[] buffer = new char[1024];
            int read;

            while ((read = br.read(buffer)) != -1) {
                for (int i = 0; i < read; i++) {
                    char c = Character.toLowerCase(buffer[i]);
                    if (Character.isLetter(c)) {
                        wordBuilder.append(c);
                    } else {
                        if (wordBuilder.length() > 0) {
                            String word = wordBuilder.toString();
                            wordBuilder.setLength(0);
                            wordFrequencyMap.merge(word, 1, Integer::sum);
                            trie.insert(word, wordFrequencyMap.get(word));
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error processing old messages: " + e.getMessage());
        }
        trie.precomputeBestSuggestions(wordFrequencyMap);
    }

    public String[] guess(final char letter, final int letterPosition, final int wordPosition) {
        if (letterPosition == 0) {
            currentWordPrefix.setLength(0);
            currentNode = trie.root;
        }

        currentWordPrefix.append(letter);

        if (letter < 'a' || letter > 'z' || currentNode == null) {
            Arrays.fill(guesses, null);
            currentNode = null; // Reset if input is invalid
            return guesses;
        }

        int index = letter - 'a';
        currentNode = currentNode.children[index];

        if (currentNode == null) {
            Arrays.fill(guesses, null);
            return guesses; // No further suggestions available
        }

        // Fetch precomputed best suggestion
        String bestSuggestion = currentNode.bestSuggestion;
        guesses[0] = bestSuggestion;

        // Fill remaining guesses if needed
        guesses[1] = null;
        guesses[2] = null;

        return guesses;
    }

    public void feedback(final boolean isCorrectGuess, final String correctWord) {
        if (correctWord == null || !correctWord.matches("^[a-z]+$")) return;
    
        int adjustment = isCorrectGuess ? 10 : -2;
        wordFrequencyMap.put(correctWord, Math.max(0, wordFrequencyMap.getOrDefault(correctWord, 0) + adjustment));
    
        trie.updateBestSuggestions(correctWord, wordFrequencyMap);
    }    

    private static class Trie {
        private final TrieNode root = new TrieNode();

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

        public void precomputeBestSuggestions(Map<String, Integer> wordFrequencyMap) {
            computeBestSuggestions(root, "", wordFrequencyMap);
        }

        private void computeBestSuggestions(TrieNode node, String prefix, Map<String, Integer> wordFrequencyMap) {
            if (node == null) return;

            if (node.isWord) {
                node.bestSuggestion = prefix;
            }

            for (int i = 0; i < 26; i++) {
                if (node.children[i] != null) {
                    computeBestSuggestions(node.children[i], prefix + (char) ('a' + i), wordFrequencyMap);

                    // Update the best suggestion for the current node
                    TrieNode child = node.children[i];
                    if (child.bestSuggestion != null && 
                        (node.bestSuggestion == null || 
                         wordFrequencyMap.getOrDefault(child.bestSuggestion, 0) > wordFrequencyMap.getOrDefault(node.bestSuggestion, 0))) {
                        node.bestSuggestion = child.bestSuggestion;
                    }
                }
            }
        }

        public void updateBestSuggestions(String word, Map<String, Integer> wordFrequencyMap) {
            TrieNode node = root;
            for (char c : word.toCharArray()) {
                // Validate the character
                if (c < 'a' || c > 'z') {
                    return; // Skip invalid words
                }
                int index = c - 'a';
                if (node.children[index] == null) return;
                node = node.children[index];
        
                if (node.bestSuggestion == null || 
                    wordFrequencyMap.getOrDefault(word, 0) > wordFrequencyMap.getOrDefault(node.bestSuggestion, 0)) {
                    node.bestSuggestion = word;
                }
            }
        }        
    }

    private static class TrieNode {
        private final TrieNode[] children = new TrieNode[26];
        private boolean isWord;
        private int frequency = 0; // Frequency of the word at this node
        private String bestSuggestion = null; // Best suggestion for this prefix
    }
}
