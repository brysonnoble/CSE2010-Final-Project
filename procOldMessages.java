import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

public class procOldMessages
{
   /* public static hashmap to store data as such:
   Words in alphabetical order, with an occurence factor (how many times does it appear in old texts?) for use of "priority",
   and has a feature to add words and/or characters to further reduce the time it takes to autofill.
   */ 
   public static TreeMap<String, Integer> wordList = new TreeMap<>();

   public static void main(String args[])
   {
      // Reads input from old text files
      try (BufferedReader oldInput = new BufferedReader(new FileReader(args[0])))
      {
         String line;
         int count = 1;
         while ((line = oldInput.readLine()) != null)
         {
            String[] words = line.split(" ");
            for (String word: words)
            {
               word = word.toLowerCase();
               String charactersToRemove = "[,.!?|;:/()\\[\\]_#@\"-]+";
               word = word.replaceAll(charactersToRemove, "");
               if (!checkWordList(word))
               {
                  wordList.put(word.toLowerCase(), count);
               }

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
}