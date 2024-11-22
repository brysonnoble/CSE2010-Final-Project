/*

Authors (group members): Ashley McKim, Bryson Noble, Calvin Rutherford, Matteo Caruso
Email addresses of group members:
Group name: 34c

Course: CSE2010
Section:

Description of the overall algorithm:


*/
import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.List;

public class SmartWord
{
  final String[] guesses = new String[3];  // 3 guesses from SmartWord

  private final Trie trie;
    
    // Initialize the tree with the words from the file:
    public SmartWord(String wordFile) 
        this.trie = new Trie();
        try (BufferedReader br = new BufferedReader(new FileReader(wordFile))) {
            String word;
            while ((word = br.readLine()) != null) {
                // Normalize the input for consistent matching by putting every word in lower case:
                word = word.toLowerCase().trim();
                if (!word.isEmpty()) {
                    trie.insert(word);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading word file: " + e.getMessage());
        }
    }

  // process old messages from oldMessageFile
  public void processOldMessages (final String oldMessageFile)
  {
    
  }

  // based on a letter typed in by the user, return 3 word guesses in an array
  // letter: letter typed in by the user
  // letterPosition:  position of the letter in the word, starts from 0
  // wordPosition: position of the word in a message, starts from 0
  public String[] guess (final char letter,  final int letterPosition, final int wordPosition)
  {
    final String prefix = ""; // letters typed so far
    final TrieNode node = root;

    // go through trie based off prefix and return empty guesses if no matches found
    for (final char c : prefix.toCharArray()) {
      node = node.children.get(c);
      if (node == null) {
        return new String[3];
      }
    }

    // collect guesses
    final List<String> matches = new ArrayList<>();
    collectWords(node, prefix, matches);

    // take top 3 guesses
    for (int i = 0; i < 3; i++) {
      guesses[i] = i < matches.size() ? matches.get(i) : null;
    }

    return guesses;
  }

  private void collectWords(final TrieNode node, final String prefix, final List<String> matches) {
    // stop at 3 guesses
    if (matches.size() >= 3) {
      return;
    }

    // if less than 3 guesses repeat
    if (node.isWord) {
      matches.add(prefix);
    }
    for (final char c : node.children.keySet()) {
      collectWords(node.children.get(c), prefix + c, matches);
    }
  }

  // feedback on the 3 guesses from the user
  // isCorrectGuess: true if one of the guesses is correct
  // correctWord: 3 cases:
  // a.  correct word if one of the guesses is correct
  // b.  null if none of the guesses is correct, before the user has typed in 
  //            the last letter
  // c.  correct word if none of the guesses is correct, and the user has 
  //            typed in the last letter
  // That is:
  // Case       isCorrectGuess      correctWord   
  // a.         true                correct word
  // b.         false               null
  // c.         false               correct word
  public void feedback (final boolean isCorrectGuess, final String correctWord)        
  {

  }

}
