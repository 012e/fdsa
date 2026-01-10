package huyphmnat.fdsa.base.utils;

import huyphmnat.fdsa.search.dtos.CodeFileDocument;
import net.datafaker.Faker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CodeGenerator {
    private static final Faker faker = new Faker();

    public static String generateJavaClass(String className) {
        StringBuilder sb = new StringBuilder();
        sb.append("public class ").append(className).append(" {\n");

        // Add fields
        for (int i = 0; i < faker.number().numberBetween(2, 6); i++) {
            String fieldName = faker.name().firstName().toLowerCase();
            String fieldType = faker.options().option("String", "int", "boolean", "double", "long");
            sb.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n");
        }

        sb.append("\n");

        // Add constructor
        sb.append("    public ").append(className).append("() {\n");
        sb.append("        // Constructor\n");
        sb.append("    }\n\n");

        // Add methods
        for (int i = 0; i < faker.number().numberBetween(1, 4); i++) {
            String methodName = faker.name().firstName().toLowerCase();
            sb.append("    public void ").append(methodName).append("() {\n");
            sb.append("        // ").append(faker.lorem().sentence()).append("\n");
            sb.append("    }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    public static String generatePythonFunction(String functionName) {
        StringBuilder sb = new StringBuilder();
        sb.append("def ").append(functionName).append("(");

        // Add parameters
        int paramCount = faker.number().numberBetween(0, 4);
        for (int i = 0; i < paramCount; i++) {
            if (i > 0) sb.append(", ");
            sb.append(faker.name().firstName().toLowerCase());
        }

        sb.append("):\n");
        sb.append("    \"\"\"").append(faker.lorem().sentence()).append("\"\"\"\n");

        // Add some logic
        for (int i = 0; i < faker.number().numberBetween(1, 3); i++) {
            sb.append("    # ").append(faker.lorem().sentence()).append("\n");
        }
        sb.append("    pass\n");

        return sb.toString();
    }

    public static String generateJavaScriptFunction(String functionName) {
        StringBuilder sb = new StringBuilder();
        sb.append("function ").append(functionName).append("(");

        int paramCount = faker.number().numberBetween(0, 3);
        for (int i = 0; i < paramCount; i++) {
            if (i > 0) sb.append(", ");
            sb.append(faker.name().firstName().toLowerCase());
        }

        sb.append(") {\n");
        sb.append("  // ").append(faker.lorem().sentence()).append("\n");
        sb.append("  return null;\n");
        sb.append("}\n");

        return sb.toString();
    }

    public static List<CodeFileDocument> generateTestDocuments(int count, UUID repositoryId, String repositoryIdentifier) {
        List<CodeFileDocument> documents = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String language;
            String extension;
            String content;
            String fileName;

            // Randomly choose language
            int langChoice = faker.number().numberBetween(0, 3);
            if (langChoice == 0) {
                language = "Java";
                extension = "java";
                String className = faker.name().firstName() + "Class";
                fileName = className + ".java";
                content = generateJavaClass(className);
            } else if (langChoice == 1) {
                language = "Python";
                extension = "py";
                String funcName = faker.name().firstName().toLowerCase();
                fileName = funcName + ".py";
                content = generatePythonFunction(funcName);
            } else {
                language = "JavaScript";
                extension = "js";
                String funcName = faker.name().firstName().toLowerCase();
                fileName = funcName + ".js";
                content = generateJavaScriptFunction(funcName);
            }

            String filePath = "src/" + (i % 10 == 0 ? "test/" : "main/") + fileName;

            documents.add(CodeFileDocument.builder()
                    .id(UUID.randomUUID())
                    .repositoryId(repositoryId)
                    .repositoryIdentifier(repositoryIdentifier)
                    .filePath(filePath)
                    .fileName(fileName)
                    .fileExtension(extension)
                    .language(language)
                    .content(content)
                    .size((long) content.length())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());
        }

        return documents;
    }

    private CodeGenerator() {
    }
}
