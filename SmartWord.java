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

public class SmartWord {

    private final Trie trie;
    private final Map<String, Integer> wordFrequencyMap = new HashMap<>();
    private final Map<String, Map<String, Integer>> bigramFrequencyMap = new HashMap<>();
    private final Map<String, Map<String, Map<String, Integer>>> trigramFrequencyMap = new HashMap<>();
    private final StringBuilder currentWordPrefix = new StringBuilder();
    private TrieNode currentNode = null;
    private final String[] guesses = new String[3];
    private String lastWord = null;
    private String secondLastWord = null;

    public SmartWord(final String wordFile) {
        trie = new Trie();
        loadVocabulary(wordFile);
    }

    private void loadVocabulary(String wordFile) {
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
    }

    public void processOldMessages(final String oldMessageFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(oldMessageFile))) {
            String previousWord = null;
            String prePreviousWord = null;
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
                            updateFrequencies(word, previousWord, prePreviousWord);
                            prePreviousWord = previousWord;
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

    private void updateFrequencies(String word, String previousWord, String prePreviousWord) {
        wordFrequencyMap.merge(word, 1, Integer::sum);
        trie.insert(word, wordFrequencyMap.get(word));

        if (previousWord != null) {
            bigramFrequencyMap.computeIfAbsent(previousWord, k -> new HashMap<>()).merge(word, 1, Integer::sum);
            limitFrequencyMap(bigramFrequencyMap.get(previousWord));
        }
        if (prePreviousWord != null) {
            trigramFrequencyMap
                .computeIfAbsent(prePreviousWord, k -> new HashMap<>())
                .computeIfAbsent(previousWord, k -> new HashMap<>())
                .merge(word, 1, Integer::sum);
            limitFrequencyMap(trigramFrequencyMap.get(prePreviousWord).get(previousWord));
        }
    }

    private List<String> getContextualSuggestions(List<String> suggestions, String[] previousWords) {
        int nGramLength = Math.min(previousWords.length, 4);
        Map<String, Integer> contextualMap = new HashMap<>();

        for (int i = 0; i < suggestions.size(); i++) {
            String suggestion = suggestions.get(i);
            int score = calculateContextScore(suggestion, previousWords, nGramLength);
            contextualMap.put(suggestion, score);
        }

        return contextualMap.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private int calculateContextScore(String suggestion, String[] previousWords, int nGramLength) {
        int score = 0;
        for (int i = 0; i < nGramLength; i++) {
            score += bigramFrequencyMap.getOrDefault(previousWords[i], new HashMap<>()).getOrDefault(suggestion, 0);
        }
        return score;
    }

    private void limitFrequencyMap(Map<String, Integer> map) {
        if (map.size() > 50) {
            map.entrySet().removeIf(entry -> entry.getValue() < Collections.min(map.values()));
        }
    }

    public String[] guess(final char letter, final int letterPosition, final int wordPosition) {
        if (letterPosition == 0) {
            currentWordPrefix.setLength(0);
            currentNode = trie.root;
        }

        currentWordPrefix.append(letter);

        if (letter < 'a' || letter > 'z' || currentNode == null) {
            Arrays.fill(guesses, null);
            currentNode = null;
            return guesses;
        }

        int index = letter - 'a';
        currentNode = currentNode.children[index];

        if (currentNode == null) {
            Arrays.fill(guesses, null);
            return guesses;
        }

        List<String> suggestions = trie.getSuggestions(
            currentNode,
            currentWordPrefix.toString(),
            10,
            wordFrequencyMap
        );

        if (secondLastWord != null && lastWord != null) {
            suggestions = refineSuggestionsWithContext(suggestions, lastWord, secondLastWord);
        }

        for (int i = 0; i < 3; i++) {
            guesses[i] = i < suggestions.size() ? suggestions.get(i) : null;
        }

        return guesses;
    }

    private List<String> refineSuggestionsWithContext(List<String> suggestions, String lastWord, String secondLastWord) {
        Map<String, Integer> bigramScores = bigramFrequencyMap.getOrDefault(lastWord, new HashMap<>());
        Map<String, Map<String, Integer>> trigramMap = trigramFrequencyMap.getOrDefault(secondLastWord, new HashMap<>());
        Map<String, Integer> trigramScores = trigramMap.getOrDefault(lastWord, new HashMap<>());

        suggestions.sort((a, b) -> {
            int trigramA = bigramScores.getOrDefault(a, 0) + trigramScores.getOrDefault(a, 0);
            int trigramB = bigramScores.getOrDefault(b, 0) + trigramScores.getOrDefault(b, 0);
            return trigramB - trigramA;
        });

        return suggestions;
    }

    public void feedback(final boolean isCorrectGuess, final String correctWord) {
        if (correctWord == null || !correctWord.matches("^[a-z]+$")) return;

        int adjustment = isCorrectGuess ? 50 : -2;
        wordFrequencyMap.put(correctWord, Math.max(0, wordFrequencyMap.getOrDefault(correctWord, 0) + adjustment));
        trie.updateBestSuggestions(correctWord, wordFrequencyMap);

        if (lastWord != null) {
            bigramFrequencyMap
                .computeIfAbsent(lastWord, k -> new HashMap<>())
                .merge(correctWord, 1, Integer::sum);
        }
        if (secondLastWord != null && lastWord != null) {
            trigramFrequencyMap
                .computeIfAbsent(secondLastWord, k -> new HashMap<>())
                .computeIfAbsent(lastWord, k -> new HashMap<>())
                .merge(correctWord, 1, Integer::sum);
        }

        secondLastWord = lastWord;
        lastWord = correctWord;
    }

    // Trie Implementation
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
                    if (node.bestSuggestions.size() > 10) {
                        node.bestSuggestions.sort((a, b) -> wordFrequencyMap.get(b) - wordFrequencyMap.get(a));
                        node.bestSuggestions.remove(node.bestSuggestions.size() - 1);
                    }
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
        private final List<String> bestSuggestions = new ArrayList<>(3);
    }
}
