package it.orz.reactiveplayground.pubsub;

import reactor.core.publisher.Flux;

/**
 * This interface represents a publish-subscribe (pub-sub) API, allowing clients to subscribe and publish messages
 * to specific topics.
 */
public interface PubSubApi {

    /**
     * Subscribes to a specific topic to receive messages of a specified type.
     *
     * @param topic         The topic to subscribe to.
     * @param messageClass  The class of the messages to subscribe to.
     * @param <T>           The type of the messages.
     * @return A Flux stream of messages of the specified type.
     */
    <T> Flux<T> subscribe(String topic, Class<T> messageClass);

    /**
     * Publishes a message to the specified topic.
     *
     * @param <T> The type of the message
     * @param topic The topic to which the message will be published
     * @param message The message to be published
     */
    <T> void publish(String topic, T message);

}
