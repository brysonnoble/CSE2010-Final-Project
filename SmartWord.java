/*

Authors (group members): Ashley McKim, Bryson Noble, Calvin Rutherford, Matteo Caruso
Email addresses of group members: amckim2022@my.fit.edu, bnoble2023@my.fit.edu, crutherford2023@my.fit.edu, mcaruso2023@my.fit.edu
Group name: 34c

Course: CSE2010
Section: 3 and 4

Description of the overall algorithm: Autofill function that takes old text recommendations in order to guess how to fill
words in new texts, overall speeding up the texting process.

*/

import java.io.*;
import java.util.*;

public class SmartWord {
   private final Trie trie;
   private final Map<String, Integer> wordFrequencyMap = new HashMap<>();
   private final Map<String, Map<String, Integer>> bigramFrequencyMap = new HashMap<>();
   private final StringBuilder currentWordPrefix = new StringBuilder();

   private final String[] guesses = new String[3];

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

  private void collectWords (final TrieNode node, final String prefix, final PriorityQueue<String> matches) {
   if (node.isWord) matches.offer(prefix);
      for (int i = 0; i < 26; i++) {
         if (node.children[i] != null) {
            final char nextChar = (char) ('a' + i);
            collectWords(node.children[i], prefix + nextChar, matches);
         }
      }
   }

   public String[] guess (final char letter, final int letterPosition, final int wordPosition) {
      if (letterPosition == 0) currentWordPrefix.setLength(0);
      currentWordPrefix.append(letter);

      final TrieNode node = trie.findNode(currentWordPrefix.toString());
      if (node == null) {
         Arrays.fill(guesses, null);
         return guesses;
      }

      final PriorityQueue<String> matches = new PriorityQueue<>((a, b) -> {
         final int freqDiff = wordFrequencyMap.getOrDefault(b, 0) - wordFrequencyMap.getOrDefault(a, 0);
         return freqDiff != 0 ? freqDiff : a.compareTo(b);
      });

      collectWords(node, currentWordPrefix.toString(), matches);

      for (int i = 0; i < 3; i++) {
         guesses[i] = matches.isEmpty() ? null : matches.poll();
      }
      return guesses;
   }  

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

   // Trie and TrieNode classes
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
