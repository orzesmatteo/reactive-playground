package it.orz.reactiveplayground.pubsub;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class implements the PubSubApi interface and provides the functionality for subscribing and publishing messages
 * to specific topics.
 */
@Component
public class PubSubImpl implements PubSubApi {

    private final Map<String, Map<Class<?>, List<Sinks.Many<?>>>> subscribersMap = new HashMap<>();

    /**
     * Subscribes to a specific topic to receive messages of a specified type.
     *
     * @param topic        The topic to subscribe to.
     * @param messageClass The class of the messages to subscribe to.
     * @param <T>          The type of the messages.
     * @return A Flux stream of messages of the specified type.
     */
    @Override
    public <T> Flux<T> subscribe(String topic, Class<T> messageClass) {
        Sinks.Many<T> many = Sinks.many().multicast().directBestEffort();
        Map<Class<?>, List<Sinks.Many<?>>> topicMap = subscribersMap.containsKey(topic) ? subscribersMap.get(topic) : new HashMap<>();
        List<Sinks.Many<?>> manyList = topicMap.containsKey(messageClass) ? topicMap.get(messageClass) : new ArrayList<>();
        manyList.add(many);
        topicMap.put(messageClass, manyList);
        subscribersMap.put(topic, topicMap);
        return many.asFlux();
    }

    /**
     * Publishes a message to the specified topic.
     *
     * @param <T>    The type of the message
     * @param topic  The topic to which the message will be published
     * @param message The message to be published
     */
    @Override
    public <T> void publish(String topic, T message) {
        if (subscribersMap.containsKey(topic) && subscribersMap.get(topic).containsKey(message.getClass())) {
            List<Sinks.Many<?>> manyList = subscribersMap
                    .get(topic)
                    .get(message.getClass());
            //noinspection unchecked
            manyList.forEach(many -> ((Sinks.Many<T>) many).tryEmitNext(message));
        }
    }
}
