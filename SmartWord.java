/*

Authors (group members): Ashley McKim, Bryson Noble, Calvin Rutherford, Matteo Caruso
Email addresses of group members: , , crutherford2023@my.fit.edu
Group name: 34c

Course: CSE2010
Section: 3 and 4

Description of the overall algorithm: Autofill function that takes old text recommendations in order to guess how to fill
words in new texts, overall speeding up the texting process.


*/
import java.io.*;
import java.util.*;

public class SmartWord
{
  final String[] guesses = new String[3];  // 3 guesses from SmartWord
  public static TreeMap<String, Integer> wordList = new TreeMap<>();

  private final Trie trie;
    
    // Initialize the tree with the words from the file:
    public SmartWord(String wordFile) 
    {
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
    // Reads input from old text files
    try (BufferedReader oldInput = new BufferedReader(new FileReader(oldMessageFile)))
      {
        String line;
        int count = 1;
        while ((line = oldInput.readLine()) != null)
          {
            String[] words = line.split(" ");
            for (String word: words)
              {
                // Allows every word to appear in the list with no weird punctuation and capitalization errors
                word = word.toLowerCase();
                String charactersToRemove = "[,.!?|;:/()\\[\\]_#@\"-]+";
                word = word.replaceAll(charactersToRemove, "");
                // Check if the word has already been added to list
                if (!checkWordList(word))
                {
                  wordList.put(word.toLowerCase(), count);
                }
                // If the word exists within the list, update its count
                else if (checkWordList(word))
                {
                  count = wordList.get(word);
                  int update = count + 1;
                  wordList.replace(word, update);
                }
              }
          }
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
  }

  public static boolean checkWordList(String word)
  {
    return wordList.containsKey(word.toLowerCase());
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
  public void feedback (final boolean isCorrectGuess, final String correctWord) {
    // a. if the guess was correct, we increase the frequency for the correct word
    if (isCorrectGuess) {
        increaseFrequency(correctWord);
    } else {
        // b. null if none of the guesses is correct, before the user has typed in last letter
        if (correctWord == null) {
            System.out.println("no correct guess yet");
            // c. correct word if none of the guesses is correct, and the user has typed in the last letter
        } else {
            increaseFrequency(correctWord);
            // but decrease frequency of the incorrect guess/
            for (String guess : guesses) {
                if (guess != null && !guess.equals(correctWord)) {
                    decreaseFrequency(guess);
                }
            }
        }
    }
}
    // increasing the weight of the correct word
    // Added: ones that are constantly use or recognized to be used previously,bump up by two
    private void increaseFrequency(String word) {
        int currentFrequency = wordFrequencyMap.getOrDefault(word, 0);
        int increment = currentFrequency > 0 ? 2 : 1;  // increase by 1, if used before, up by 2
        wordFrequencyMap.put(word, currentFrequency + increment);
}
    // added decreasefrequency 
    private void decreaseFrequency(String word) {
        int currentFrequency = wordFrequencyMap.getOrDefault(word,0);
        // decrease frequency only if it is greater than 0
        if (currentFrequency > 0) {
            wordFrequencyMap.put(word, currentFrequency - 1);
        }
    }
    // get the frequency of a word
    public int getWordFrequency(String word) {
        return wordFrequencyMap.getOrDefault(word, 0);
    }
}
