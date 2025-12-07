package huyphmnat.fdsa.snippet.internal.services.interfaces;

public interface EventService {
    void publish(String eventName, Object payload);
}
