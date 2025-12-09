package huyphmnat.fdsa.search.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface DetectLangActivities {
    @ActivityMethod
    String detectLanguage(String code);
}
