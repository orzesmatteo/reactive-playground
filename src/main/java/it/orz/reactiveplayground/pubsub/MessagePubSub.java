package it.orz.reactiveplayground.pubsub;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;


/**
 * The MessagePubSub class is responsible for initializing the PubSub component, subscribing to the "TEST" topic,
 * and publishing messages at fixed intervals.
 *
 * @see PubSubApi
 * @see Logger
 */
@Component
public class MessagePubSub {

    private static final String TOPIC_TEST = "TEST";

    private final Logger logger = LoggerFactory.getLogger(MessagePubSub.class);

    private final PubSubApi pubSubApi;

    public MessagePubSub(PubSubApi pubSubApi) {
        this.pubSubApi = pubSubApi;
    }

    /**
     * Initializes the MessagePubSub component by subscribing to the "TEST" topic and registering
     * callback functions to handle the received messages.
     * This method is annotated with @PostConstruct to indicate that it should be executed after the
     * bean is constructed.
     * It subscribes to the "TEST" topic to receive messages of type String and Instant. The received
     * messages are logged using the logger.
     * Note: The implementation of pubSubApi is passed through the constructor injection.
     *
     * @see PubSubApi
     * @see #subscribe(String, Class)
     * @see Logger#info(String)
     */
    @PostConstruct
    public void init() {
        pubSubApi.subscribe(TOPIC_TEST, String.class).subscribe(logger::info);
        pubSubApi.subscribe(TOPIC_TEST, Instant.class).subscribe(message -> logger.info(message.toString()));
    }

    /**
     * This method sends a message by publishing it to a specified topic using the PubSubApi.
     * The message can be of any type and is published as a string representation. If the message
     * is of type Instant, it is also published as an Instant object.
     *
     * This method is scheduled to run at fixed intervals of 10 seconds.
     */
    @Scheduled(fixedRate = 10000L)
    public void sendMessage() {
        pubSubApi.publish(TOPIC_TEST, Instant.now().toString());
        pubSubApi.publish(TOPIC_TEST, Instant.now());
    }

}
