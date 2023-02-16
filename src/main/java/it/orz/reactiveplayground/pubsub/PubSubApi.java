package it.orz.reactiveplayground.pubsub;

import reactor.core.publisher.Flux;

public interface PubSubApi {

    <T> Flux<T> subscribe(String topic, Class<T> messageClass);

    <T> void publish(String topic, T message);

}
