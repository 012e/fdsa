package huyphmnat.fdsa.search.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface EmbeddingGenerator {
    @ActivityMethod
    float[] generateEmbedding(String content);

    @ActivityMethod
    float[][] generateEmbeddings(String[] contents);
}
