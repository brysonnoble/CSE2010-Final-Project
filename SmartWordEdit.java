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
    private final TernaryTrie trie;
    
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
        trie = new TernaryTrie();
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
    public void processOldMessages (final String oldMessageFile) {
        try (final BufferedReader br = new BufferedReader(new FileReader(oldMessageFile))) {
            String prevWord = null;
            String line;
            while ((line = br.readLine()) != null) {
                final String[] words = line.toLowerCase().split("\\W+");
                for (String word : words) {
                    word = sanitize(word);
                    if (!word.isEmpty()) {
                        trie.insert(word);
                        wordFrequencyMap.put(word, wordFrequencyMap.getOrDefault(word, 0) + 1);

                        // Update bigram frequency
                        if (prevWord != null) {
                            bigramFrequencyMap.putIfAbsent(prevWord, new HashMap<>());
                            bigramFrequencyMap.get(prevWord).put(word, bigramFrequencyMap.get(prevWord).getOrDefault(word, 0) + 1);
                        }
                        prevWord = word;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error processing old messages: " + e.getMessage());
        }
    }  

    // Collects all words from a TrieNode that match the given prefix and they are added to the priority queue in lexicographical order:
    private void collectWords (final TrieNode node, final String prefix, final PriorityQueue<String> matches) {
        if (node.isWord) matches.offer(prefix);
        if (node.left != null)
        {
            collectWords(node.left, prefix, matches);
        }
        if (node.middle != null)
        {
            collectWords(node.middle, prefix + node.c, matches);
        }
        if (node.right != null)
        {
            collectWords(node.right, prefix, matches);
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

        // Consider context of word to make guess
        if (wordPosition > 0)
        {
            String prevWord = currentWordPrefix.toString();
            Map<String, Integer> contextSuggestions = bigramFrequencyMap.getOrDefault(prevWord, new HashMap<>());
            for (Map.Entry<String, Integer> entry : contextSuggestions.entrySet()) {
                if (matches.size() < 3 || wordFrequencyMap.getOrDefault(entry.getKey(), 0) > wordFrequencyMap.getOrDefault(matches.peek(), 0))
                {
                   matches.offer(entry.getKey()); 
                }
            }

        }
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
                    wordFrequencyMap.put(guess, Math.max(0, wordFrequencyMap.getOrDefault(guess, 0) - 1));
                }
            }
        }
    }  

    // Trie and TrieNode classes:
    private static class TernaryTrie {
        private TrieNode root = new TrieNode('\0');

        public void insert (final String word) {
           root = insert(root, word, 0);
        }     
     
        public TrieNode insert(TrieNode node, String word, int index) {
            if (index == word.length())
            {
                if (node == null)
                {
                    node = new TrieNode(word.charAt(index - 1));
                }
                node.isWord = true;
                return node;
            }
            char c = word.charAt(index);
            if (node == null)
            {
                node = new TrieNode(c);
            }
            
            if (c < node.c)
            {
                node.left = insert(node.left, word, index);
            }
            else if (c > node.c)
            {
                node.right = insert(node.right, word, index);
            }
            else
            {
                if (index == word.length() - 1)
                {
                    node.isWord = true;
                }
                else
                {
                    node.middle = insert(node.middle, word, index + 1);
                }
            }
            return node;
        }

        public TrieNode findNode(final String prefix)
        {
            return findNode(root, prefix, 0);
        }

        public TrieNode findNode(TrieNode node, String prefix, int index)
        {
            if (node == null)
            {
                return null;
            }

            char c = prefix.charAt(index);

            if (c < node.c)
            {
                return findNode(node.left, prefix, index);
            }
            else if (c > node.c)
            {
                return findNode(node.right, prefix, index);
            }
            else
            {
                if (index == prefix.length() - 1)
                {
                    return node;
                }
                return findNode(node.middle, prefix, index + 1);
            }
        }
    }

    private static class TrieNode {
        char c;
        boolean isWord;
        TrieNode left, middle, right;

        public TrieNode(char c)
        {
            this.c = c;
            this.isWord = false;
            this.left = this.middle = this.right = null;
        }
    }
  
    private static String sanitize (final String input) {
        return input.toLowerCase().replaceAll("[^a-z]", ""); // Retain only a-z
    } 
}