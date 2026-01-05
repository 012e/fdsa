package huyphmnat.fdsa.search;

import huyphmnat.fdsa.search.internal.services.LanguageDetectionService;
import huyphmnat.fdsa.search.internal.services.LanguageDetectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class LanguageDetectionServiceTest {

    private LanguageDetectionService languageDetectionService;

    @BeforeEach
    void setUp() {
        languageDetectionService = new LanguageDetectionServiceImpl();
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
        assertThat(detectedLanguage).isEqualTo(expectedLanguage);
    }

    @Test
    void testDetectLanguage_NullFileName_ShouldReturnUnknown() {
        String result = languageDetectionService.detectLanguage(null);
        assertThat(result).isEqualTo("Unknown");
    }

    @Test
    void testDetectLanguage_EmptyFileName_ShouldReturnUnknown() {
        String result = languageDetectionService.detectLanguage("");
        assertThat(result).isEqualTo("Unknown");
    }

    @Test
    void testDetectLanguage_NoExtension_ShouldReturnUnknown() {
        String result = languageDetectionService.detectLanguage("Makefile");
        assertThat(result).isEqualTo("Unknown");
    }

    @Test
    void testDetectLanguage_DotFileWithoutExtension_ShouldReturnUnknown() {
        String result = languageDetectionService.detectLanguage(".gitignore");
        assertThat(result).isEqualTo("Unknown");
    }

    @Test
    void testDetectLanguage_MultipleDotsInFileName_ShouldUseLastExtension() {
        String result = languageDetectionService.detectLanguage("test.component.ts");
        assertThat(result).isEqualTo("TypeScript");
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
        assertThat(result).isEqualTo(expectedIsCode);
    }

    @Test
    void testIsCodeFile_NullFileName_ShouldReturnFalse() {
        boolean result = languageDetectionService.isCodeFile(null);
        assertThat(result).isFalse();
    }

    @Test
    void testIsCodeFile_EmptyFileName_ShouldReturnFalse() {
        boolean result = languageDetectionService.isCodeFile("");
        assertThat(result).isFalse();
    }

    @Test
    void testDetectLanguage_CaseInsensitive() {
        assertThat(languageDetectionService.detectLanguage("Main.JAVA")).isEqualTo("Java");
        assertThat(languageDetectionService.detectLanguage("script.PY")).isEqualTo("Python");
        assertThat(languageDetectionService.detectLanguage("app.TS")).isEqualTo("TypeScript");
    }
}