package huyphmnat.fdsa.shared.events;
public interface EventService {
    void publish(String eventName, Object payload);
}
