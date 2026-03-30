package it.orz.reactivepubsub;

import reactor.core.publisher.Flux;

/**
 * Publish-subscribe API allowing clients to subscribe and publish messages to specific topics.
 *
 * <p>Messages are routed by both topic name and message type, enabling type-safe pub-sub
 * communication within a reactive pipeline.</p>
 */
public interface PubSubApi {

    /**
     * Subscribes to a specific topic to receive messages of a specified type.
     *
     * <p>The returned {@link Flux} will emit messages published to the given topic that match
     * the specified class. Cancelling the Flux automatically removes the subscriber,
     * preventing memory leaks.</p>
     *
     * @param topic        the topic to subscribe to
     * @param messageClass the class of the messages to receive
     * @param <T>          the type of the messages
     * @return a Flux stream of messages matching the topic and type
     */
    <T> Flux<T> subscribe(String topic, Class<T> messageClass);

    /**
     * Publishes a message to the specified topic.
     *
     * <p>The message is delivered to all subscribers registered for the given topic whose
     * message type matches {@code message.getClass()}. If no subscribers exist for the
     * topic/type combination, the message is silently dropped. Emit failures are logged
     * but do not throw.</p>
     *
     * @param topic   the topic to which the message will be published
     * @param message the message to be published
     * @param <T>     the type of the message
     */
    <T> void publish(String topic, T message);

}
