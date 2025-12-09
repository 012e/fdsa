package huyphmnat.fdsa.search.internal.activities;

import huyphmnat.fdsa.search.activities.DetectLangActivities;
import org.springframework.stereotype.Service;

@Service
public class DetectLangActivitiesImpl implements DetectLangActivities {
    @Override
    public String detectLanguage(String code) {
        return "java";
    }
}
