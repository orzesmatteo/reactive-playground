package it.orz.reactivepubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe implementation of {@link PubSubApi} using Project Reactor's {@link Sinks.Many}.
 *
 * <p>Subscribers are stored in a nested {@link ConcurrentHashMap} keyed by topic and message type.
 * Each subscriber gets its own {@link Sinks.Many} with backpressure buffering. Subscribers are
 * automatically removed when their Flux is cancelled, preventing memory leaks.</p>
 */
public class PubSubImpl implements PubSubApi {

    private static final Logger LOG = LoggerFactory.getLogger(PubSubImpl.class);

    private final Map<String, Map<Class<?>, List<Sinks.Many<?>>>> subscribersMap = new ConcurrentHashMap<>();

    @Override
    public <T> Flux<T> subscribe(String topic, Class<T> messageClass) {
        Sinks.Many<T> sink = Sinks.many().multicast().onBackpressureBuffer();
        subscribersMap
                .computeIfAbsent(topic, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(messageClass, k -> new CopyOnWriteArrayList<>())
                .add(sink);
        return sink.asFlux()
                .doOnCancel(() -> removeSink(topic, messageClass, sink));
    }

    @Override
    public <T> void publish(String topic, T message) {
        Map<Class<?>, List<Sinks.Many<?>>> classMap = subscribersMap.get(topic);
        if (classMap == null) {
            return;
        }

        List<Sinks.Many<?>> sinks = classMap.get(message.getClass());
        if (sinks == null) {
            return;
        }

        for (Sinks.Many<?> sink : sinks) {
            @SuppressWarnings("unchecked")
            Sinks.Many<T> typedSink = (Sinks.Many<T>) sink;
            Sinks.EmitResult result = typedSink.tryEmitNext(message);
            if (result.isFailure()) {
                LOG.warn("Failed to emit message to subscriber on topic '{}': {}", topic, result);
            }
        }
    }

    private void removeSink(String topic, Class<?> messageClass, Sinks.Many<?> sink) {
        subscribersMap.computeIfPresent(topic, (t, classMap) -> {
            classMap.computeIfPresent(messageClass, (c, sinks) -> {
                sinks.remove(sink);
                return sinks.isEmpty() ? null : sinks;
            });
            return classMap.isEmpty() ? null : classMap;
        });
    }

}
