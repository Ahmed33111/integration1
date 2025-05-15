package utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class BadWordFilter {
    private static Set<String> badWords;
    private static BadWordFilter instance;

    private BadWordFilter() {
        loadBadWords();
    }

    public static BadWordFilter getInstance() {
        if (instance == null) {
            instance = new BadWordFilter();
        }
        return instance;
    }

    private void loadBadWords() {
        badWords = new HashSet<>();
        try {
            InputStream inputStream = getClass().getResourceAsStream("/bad_words.csv");
            if (inputStream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line = reader.readLine();
                    if (line != null) {
                        String[] words = line.split(",");
                        for (String word : words) {
                            badWords.add(word.trim().toLowerCase());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading bad words: " + e.getMessage());
        }
    }

    public String filterText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Keep the original text for preserving spaces and punctuation
        String originalText = text;
        
        // Replace bad words with asterisks
        for (String badWord : badWords) {
            // Create a regex that matches the word with word boundaries
            String regex = "(?i)\\b" + badWord + "\\b";
            originalText = originalText.replaceAll(regex, "****");
        }
        
        return originalText;
    }
}
