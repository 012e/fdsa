package huyphmnat.fdsa.search.internal.services;

public interface LanguageDetectionService {
    String detectLanguage(String fileName);

    boolean isCodeFile(String fileName);
}
