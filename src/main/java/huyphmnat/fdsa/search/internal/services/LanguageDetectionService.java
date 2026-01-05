package huyphmnat.fdsa.search.internal.services;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for detecting programming language from file extensions.
 */
@Service
public class LanguageDetectionService {

    private static final Map<String, String> EXTENSION_TO_LANGUAGE = Map.ofEntries(
        // Java ecosystem
        Map.entry("java", "Java"),
        Map.entry("kt", "Kotlin"),
        Map.entry("kts", "Kotlin"),
        Map.entry("scala", "Scala"),
        Map.entry("groovy", "Groovy"),

        // JavaScript/TypeScript ecosystem
        Map.entry("js", "JavaScript"),
        Map.entry("jsx", "JavaScript"),
        Map.entry("mjs", "JavaScript"),
        Map.entry("cjs", "JavaScript"),
        Map.entry("ts", "TypeScript"),
        Map.entry("tsx", "TypeScript"),

        // Python
        Map.entry("py", "Python"),
        Map.entry("pyw", "Python"),
        Map.entry("pyx", "Python"),

        // C/C++
        Map.entry("c", "C"),
        Map.entry("h", "C"),
        Map.entry("cpp", "C++"),
        Map.entry("cc", "C++"),
        Map.entry("cxx", "C++"),
        Map.entry("hpp", "C++"),
        Map.entry("hh", "C++"),
        Map.entry("hxx", "C++"),

        // C#
        Map.entry("cs", "C#"),

        // Go
        Map.entry("go", "Go"),

        // Rust
        Map.entry("rs", "Rust"),

        // Ruby
        Map.entry("rb", "Ruby"),

        // PHP
        Map.entry("php", "PHP"),

        // Swift
        Map.entry("swift", "Swift"),

        // Web
        Map.entry("html", "HTML"),
        Map.entry("htm", "HTML"),
        Map.entry("css", "CSS"),
        Map.entry("scss", "SCSS"),
        Map.entry("sass", "Sass"),
        Map.entry("less", "Less"),

        // Shell
        Map.entry("sh", "Shell"),
        Map.entry("bash", "Bash"),
        Map.entry("zsh", "Zsh"),
        Map.entry("fish", "Fish"),

        // Other
        Map.entry("sql", "SQL"),
        Map.entry("yaml", "YAML"),
        Map.entry("yml", "YAML"),
        Map.entry("json", "JSON"),
        Map.entry("xml", "XML"),
        Map.entry("md", "Markdown"),
        Map.entry("rst", "reStructuredText"),
        Map.entry("vim", "VimScript"),
        Map.entry("lua", "Lua"),
        Map.entry("r", "R"),
        Map.entry("dart", "Dart"),
        Map.entry("ex", "Elixir"),
        Map.entry("exs", "Elixir")
    );

    /**
     * Detect programming language from file extension.
     *
     * @param fileName the file name
     * @return the detected language, or "Unknown" if not detected
     */
    public String detectLanguage(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "Unknown";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "Unknown";
        }

        String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
        return EXTENSION_TO_LANGUAGE.getOrDefault(extension, "Unknown");
    }

    /**
     * Check if a file is a code file based on extension.
     *
     * @param fileName the file name
     * @return true if the file is likely a code file
     */
    public boolean isCodeFile(String fileName) {
        return !detectLanguage(fileName).equals("Unknown");
    }
}

