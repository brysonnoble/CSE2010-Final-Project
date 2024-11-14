<h1 align="center">
  <a href="https://github.com/brysonnoble/CSE2010-Final-Project/blob/main/termProject.pdf">Full Project Outline</a>
</h1>
<i align="center"><b>Quick Breakdown of what has to be done and can be used:</b></i>

Key Functional Requirements:

    SmartWord Initialization: Initializes a list of English words to use in predictions.
    Old Messages Processing: Processes old messages to adapt suggestions to a user’s past behavior.
    Guess Function: Provides up to 3 word suggestions as a user types.
    Feedback Mechanism: Incorporates feedback to improve the accuracy of future guesses.

Suggested Methods and Structures:

    Data Structures
        Trie (Prefix Tree): Efficient for storing words and generating predictions based on prefix inputs, with quick lookups and memory efficiency. Each node represents a character, with child nodes for subsequent characters.
        Hash Map/Dictionary: Maps each unique word or prefix to its frequency or a score to rank suggestions.
        Linked List/Queue for Cache: Caches recent predictions, enhancing performance based on user patterns.

    Methods
        SmartWord Constructor:
            Initializes the Trie with words from words.txt.
            Optimizes memory by removing duplicates and possibly discarding less common words if memory is an issue.
        procOldMessages():
            Updates word frequency or other scoring in the Trie or Hash Map based on old messages.
            Helps adapt the model to user-specific language patterns.
        guess():
            Takes a partial input (prefix) and returns up to 3 guesses by:
                Traversing the Trie to find words with the given prefix.
                Ranking based on frequency or relevance (from Hash Map scoring if available).
        feedback():
            Updates scoring mechanisms based on user selection, reinforcing accurate predictions.

Step-by-Step Implementation Outline

    Initialize Word List (SmartWord.java)
        Read and load the word list into a Trie structure for efficient prefix searching.
        Optionally, maintain a Hash Map with word frequencies if prioritizing guesses by usage.

    Process Old Messages (procOldMessages)
        Parse old messages line-by-line.
        Update word frequency or pattern data in the Trie or associated Hash Map for better personalization.

    Implement Guessing Logic (guess)
        On each letter input:
            Search for matching prefixes in the Trie.
            Retrieve potential words up to a specified number (e.g., 3).
            Rank results based on frequency or relevance (using Hash Map values).

    Feedback Mechanism (feedback)
        Adjust frequency or relevance scores based on user feedback.
        Store this feedback in a Hash Map or within Trie nodes for real-time adjustments.

    Performance Measurement (EvalSmartWord.java)
        Run tests to measure:
            Accuracy: Calculate skipped letters to measure typing efficiency.
            Time: Track average guess time to ensure responsiveness.
            Memory Usage: Minimize memory footprint while maintaining performance.

    Optimization and Testing
        Test with varying word lists to balance accuracy and memory.
        Profile the Trie traversal and caching mechanisms to reduce time complexity.
        Adjust data structures and feedback weighting based on performance analysis.

    Final Presentation Preparation
        Document initial algorithms and changes made to improve accuracy and performance.
        Compare performance between initial and final implementations (accuracy, time, memory).
        Highlight areas for potential improvement, particularly in memory usage and latency.
