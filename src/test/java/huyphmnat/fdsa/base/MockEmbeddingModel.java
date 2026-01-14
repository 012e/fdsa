package huyphmnat.fdsa.base;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of EmbeddingModel for testing.
 * Returns fixed test embeddings instead of calling actual API.
 */
@Primary
@Component
@Profile("integration-testing")
public class MockEmbeddingModel implements EmbeddingModel {

    private static final int EMBEDDING_DIMENSION = 1536;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> instructions = request.getInstructions();
        List<Embedding> embeddings = new ArrayList<>();

        // Generate a test embedding for each input text
        for (int i = 0; i < instructions.size(); i++) {
            float[] embedding = generateTestEmbedding(instructions.get(i), i);
            embeddings.add(new Embedding(embedding, i));
        }

        // Return response with embeddings
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        // Generate embedding for a document
        return generateTestEmbedding(document.getText(), 0);
    }

    /**
     * Generate a deterministic test embedding based on the input text and index.
     * This creates a unique but consistent embedding for the same input.
     */
    private float[] generateTestEmbedding(String text, int index) {
        float[] embedding = new float[EMBEDDING_DIMENSION];

        // Generate deterministic values based on text hash and index
        int hash = text.hashCode();

        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            // Use a simple formula to generate values between -1 and 1
            // This ensures the same text always gets the same embedding
            float value = (float) Math.sin(hash + i + index) * 0.5f;
            embedding[i] = value;
        }

        // Normalize the vector (optional, but often done with real embeddings)
        normalizeVector(embedding);

        return embedding;
    }

    /**
     * Normalize the embedding vector to unit length
     */
    private void normalizeVector(float[] vector) {
        float sumSquares = 0.0f;
        for (float v : vector) {
            sumSquares += v * v;
        }
        float magnitude = (float) Math.sqrt(sumSquares);

        if (magnitude > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= magnitude;
            }
        }
    }
}

