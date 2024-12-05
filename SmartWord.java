import java.io.*;
import java.util.*;

public class SmartWord {
    
    private final Trie trie;
    private final Map<String, Integer> wordFrequencyMap = new HashMap<>();
    private final StringBuilder currentWordPrefix = new StringBuilder();
    private TrieNode currentNode = null; // Keep track of current Trie node during typing.
    private final String[] guesses = new String[3];

    public SmartWord(final String wordFile) {
        trie = new Trie();
        try (final BufferedReader br = new BufferedReader(new FileReader(wordFile))) {
            String word;
            while ((word = br.readLine()) != null) {
                word = word.toLowerCase().trim();
                if (!word.isEmpty()) {
                    wordFrequencyMap.put(word, 1);
                    trie.insert(word, 1, wordFrequencyMap);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading word file: " + e.getMessage());
        }
    }

    public void processOldMessages(final String oldMessageFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(oldMessageFile))) {
            String prevWord = null;
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
                            trie.insert(word, wordFrequencyMap.get(word), wordFrequencyMap);

                            if (prevWord != null) {
                                prevWord = word;
                            }
                        }
                        if (c == '\n') prevWord = null;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error processing old messages: " + e.getMessage());
        }
    }

    public String[] guess(final char letter, final int letterPosition, final int wordPosition) {
        if (letterPosition == 0) {
            currentWordPrefix.setLength(0);
            currentNode = trie.root;
        }
    
        // Append letter to the prefix
        currentWordPrefix.append(letter);
    
        // Validate that the letter is between 'a' and 'z'
        if (letter < 'a' || letter > 'z') {
            Arrays.fill(guesses, null);
            currentNode = null; // Reset current node for invalid input
            return guesses;
        }
    
        int index = letter - 'a';
        if (currentNode == null || currentNode.children[index] == null) {
            Arrays.fill(guesses, null);
            currentNode = null; // Reset current node for invalid input
            return guesses;
        }
    
        currentNode = currentNode.children[index];
    
        // Use a priority queue to store matches based on frequency and lexicographical order
        PriorityQueue<String> matches = new PriorityQueue<>((a, b) -> {
            int freqDiff = wordFrequencyMap.getOrDefault(b, 0) - wordFrequencyMap.getOrDefault(a, 0);
            return freqDiff != 0 ? freqDiff : a.compareTo(b);
        });
    
        // Add best suggestion if available
        if (currentNode.bestSuggestion != null) {
            matches.offer(currentNode.bestSuggestion);
        }
    
        // Populate guesses with up to 3 suggestions
        for (int i = 0; i < 3; i++) {
            guesses[i] = matches.isEmpty() ? null : matches.poll();
        }
        return guesses;
    }
    
    public void feedback(final boolean isCorrectGuess, final String correctWord) {
        if (correctWord == null) return;
    
        // Update the frequency of the correct word
        wordFrequencyMap.put(correctWord, wordFrequencyMap.getOrDefault(correctWord, 0) + (isCorrectGuess ? 5 : 2));
    
        // Update the bestSuggestion fields in the Trie
        TrieNode node = trie.root;
        for (char c : correctWord.toCharArray()) {
            // Validate that the character is between 'a' and 'z'
            if (c < 'a' || c > 'z') {
                continue; // Skip invalid characters
            }
            int index = c - 'a';
            if (node == null || node.children[index] == null) {
                return; // Stop updating if the path does not exist
            }
            node = node.children[index];
            if (node.bestSuggestion == null || wordFrequencyMap.get(correctWord) > wordFrequencyMap.getOrDefault(node.bestSuggestion, 0)) {
                node.bestSuggestion = correctWord;
            }
        }
    }    

    private static class Trie {
        private final TrieNode root = new TrieNode();

        public void insert(final String word, final int frequency, final Map<String, Integer> wordFrequencyMap) {
            TrieNode node = root;
            for (char c : word.toCharArray()) {
                if (c < 'a' || c > 'z') continue;
                int index = c - 'a';
                if (node.children[index] == null) {
                    node.children[index] = new TrieNode();
                }
                node = node.children[index];
                if (node.bestSuggestion == null || frequency > wordFrequencyMap.getOrDefault(node.bestSuggestion, 0)) {
                    node.bestSuggestion = word;
                }
            }
            node.isWord = true;
        }
    }

    private static class TrieNode {
        private final TrieNode[] children = new TrieNode[26];
        private boolean isWord;
        private String bestSuggestion;
    }
}
