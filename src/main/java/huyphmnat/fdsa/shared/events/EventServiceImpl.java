package huyphmnat.fdsa.shared.events;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(String eventName, Object payload) {
        kafkaTemplate.send(eventName, payload);
    }
}

