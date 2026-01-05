package huyphmnat.fdsa.search;

import huyphmnat.fdsa.search.internal.services.LanguageDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class LanguageDetectionServiceTest {

    private LanguageDetectionService languageDetectionService;

    @BeforeEach
    void setUp() {
        languageDetectionService = new LanguageDetectionService();
    }

    @ParameterizedTest
    @CsvSource({
            "Main.java, Java",
            "script.py, Python",
            "component.tsx, TypeScript",
            "app.js, JavaScript",
            "styles.css, CSS",
            "config.yaml, YAML",
            "README.md, Markdown",
            "main.go, Go",
            "lib.rs, Rust",
            "app.rb, Ruby",
            "service.kt, Kotlin",
            "build.gradle, Unknown",
            "index.html, HTML",
            "script.sh, Shell",
            "query.sql, SQL",
            "main.cpp, C++",
            "header.h, C",
            "program.cs, C#",
            "App.swift, Swift",
            "index.php, PHP"
    })
    void testDetectLanguage_ShouldReturnCorrectLanguage(String fileName, String expectedLanguage) {
        String detectedLanguage = languageDetectionService.detectLanguage(fileName);
        assertEquals(expectedLanguage, detectedLanguage);
    }

    @Test
    void testDetectLanguage_NullFileName_ShouldReturnUnknown() {
        String result = languageDetectionService.detectLanguage(null);
        assertEquals("Unknown", result);
    }

    @Test
    void testDetectLanguage_EmptyFileName_ShouldReturnUnknown() {
        String result = languageDetectionService.detectLanguage("");
        assertEquals("Unknown", result);
    }

    @Test
    void testDetectLanguage_NoExtension_ShouldReturnUnknown() {
        String result = languageDetectionService.detectLanguage("Makefile");
        assertEquals("Unknown", result);
    }

    @Test
    void testDetectLanguage_DotFileWithoutExtension_ShouldReturnUnknown() {
        String result = languageDetectionService.detectLanguage(".gitignore");
        assertEquals("Unknown", result);
    }

    @Test
    void testDetectLanguage_MultipleDotsInFileName_ShouldUseLastExtension() {
        String result = languageDetectionService.detectLanguage("test.component.ts");
        assertEquals("TypeScript", result);
    }

    @ParameterizedTest
    @CsvSource({
            "Main.java, true",
            "script.py, true",
            "README.md, true",
            "Makefile, false",
            ".gitignore, false",
            "image.png, false",
            "document.pdf, false",
            "archive.zip, false"
    })
    void testIsCodeFile_ShouldIdentifyCodeFiles(String fileName, boolean expectedIsCode) {
        boolean result = languageDetectionService.isCodeFile(fileName);
        assertEquals(expectedIsCode, result);
    }

    @Test
    void testIsCodeFile_NullFileName_ShouldReturnFalse() {
        boolean result = languageDetectionService.isCodeFile(null);
        assertFalse(result);
    }

    @Test
    void testIsCodeFile_EmptyFileName_ShouldReturnFalse() {
        boolean result = languageDetectionService.isCodeFile("");
        assertFalse(result);
    }

    @Test
    void testDetectLanguage_CaseInsensitive() {
        assertEquals("Java", languageDetectionService.detectLanguage("Main.JAVA"));
        assertEquals("Python", languageDetectionService.detectLanguage("script.PY"));
        assertEquals("TypeScript", languageDetectionService.detectLanguage("app.TS"));
    }
}