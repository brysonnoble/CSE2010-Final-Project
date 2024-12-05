import java.io.*;
import java.util.*;

public class SmartWord {

    private final Trie trie;
    private final Map<String, Integer> wordFrequencyMap = new HashMap<>();
    private final Map<String, Map<String, Integer>> bigramFrequencyMap = new HashMap<>();
    private final StringBuilder currentWordPrefix = new StringBuilder();
    private TrieNode currentNode = null;
    private final String[] guesses = new String[3];
    private String lastWord = null;

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
            String previousWord = null;
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

                            if (previousWord != null) {
                                bigramFrequencyMap
                                    .computeIfAbsent(previousWord, k -> new HashMap<>())
                                    .merge(word, 1, Integer::sum);
                            }

                            previousWord = word;
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
            lastWord = null; // Reset last word for new context
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

        List<String> suggestions = trie.getSuggestions(
            currentNode,
            currentWordPrefix.toString(),
            10,
            wordFrequencyMap
        );

        // Enhance suggestions with bigram context
        if (lastWord != null && bigramFrequencyMap.containsKey(lastWord)) {
            Map<String, Integer> nextWordMap = bigramFrequencyMap.get(lastWord);
            suggestions.sort((a, b) -> {
                int bigramA = nextWordMap.getOrDefault(a, 0);
                int bigramB = nextWordMap.getOrDefault(b, 0);
                return bigramB - bigramA;
            });
        }

        for (int i = 0; i < 3; i++) {
            guesses[i] = i < suggestions.size() ? suggestions.get(i) : null;
        }

        return guesses;
    }

    public void feedback(final boolean isCorrectGuess, final String correctWord) {
        if (correctWord == null || !correctWord.matches("^[a-z]+$")) return;

        int adjustment = isCorrectGuess ? 10 : -2;
        wordFrequencyMap.put(correctWord, Math.max(0, wordFrequencyMap.getOrDefault(correctWord, 0) + adjustment));

        trie.updateBestSuggestions(correctWord, wordFrequencyMap);

        if (lastWord != null) {
            bigramFrequencyMap
                .computeIfAbsent(lastWord, k -> new HashMap<>())
                .merge(correctWord, 1, Integer::sum);
        }
        lastWord = correctWord;
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
                node.bestSuggestions.add(prefix);
            }

            for (int i = 0; i < 26; i++) {
                if (node.children[i] != null) {
                    computeBestSuggestions(node.children[i], prefix + (char) ('a' + i), wordFrequencyMap);

                    // Merge suggestions from child nodes
                    TrieNode child = node.children[i];
                    mergeSuggestions(node.bestSuggestions, child.bestSuggestions, wordFrequencyMap);
                }
            }
        }

        public List<String> getSuggestions(TrieNode node, String prefix, int count, Map<String, Integer> wordFrequencyMap) {
            PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(
                (a, b) -> b.getValue() - a.getValue()
            );

            for (String suggestion : node.bestSuggestions) {
                pq.add(Map.entry(suggestion, wordFrequencyMap.getOrDefault(suggestion, 0)));
            }

            List<String> results = new ArrayList<>();
            while (!pq.isEmpty() && results.size() < count) {
                results.add(pq.poll().getKey());
            }

            return results;
        }

        public void updateBestSuggestions(String word, Map<String, Integer> wordFrequencyMap) {
            TrieNode node = root;
            for (char c : word.toCharArray()) {
                int index = c - 'a';
                if (node.children[index] == null) return;
                node = node.children[index];

                if (!node.bestSuggestions.contains(word)) {
                    node.bestSuggestions.add(word);
                }
            }
        }

        private void mergeSuggestions(List<String> parent, List<String> child, Map<String, Integer> wordFrequencyMap) {
            Set<String> merged = new HashSet<>(parent);
            merged.addAll(child);
            parent.clear();

            parent.addAll(merged.stream()
                    .sorted((a, b) -> wordFrequencyMap.getOrDefault(b, 0) - wordFrequencyMap.getOrDefault(a, 0))
                    .limit(10)
                    .toList());
        }
    }

    private static class TrieNode {
        private final TrieNode[] children = new TrieNode[26];
        private boolean isWord;
        private int frequency = 0;
        private final List<String> bestSuggestions = new ArrayList<>();
    }
}
