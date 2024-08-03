package it.orz.reactiveplayground.pubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;

@Component
public class MessagePubSub {

    private static final String TOPIC_TEST = "TEST";

    private final Logger logger = LoggerFactory.getLogger(MessagePubSub.class);

    private final PubSubApi pubSubApi;

    public MessagePubSub(PubSubApi pubSubApi) {
        this.pubSubApi = pubSubApi;
    }

    @PostConstruct
    public void init() {
        pubSubApi.subscribe(TOPIC_TEST, String.class).subscribe(logger::info);
        pubSubApi.subscribe(TOPIC_TEST, Instant.class).subscribe(message -> logger.info(message.toString()));
    }

    @Scheduled(fixedRate = 10000L)
    public void sendMessage() {
        pubSubApi.publish(TOPIC_TEST, Instant.now().toString());
        pubSubApi.publish(TOPIC_TEST, Instant.now());
    }

}
