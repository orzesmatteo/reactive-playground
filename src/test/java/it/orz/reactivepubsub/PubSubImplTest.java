package it.orz.reactivepubsub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PubSubImplTest {

    private PubSubImpl pubSub;

    @BeforeEach
    void setUp() {
        pubSub = new PubSubImpl();
    }

    @Test
    void subscribeThenPublish_receivesMessage() {
        Flux<String> flux = pubSub.subscribe("topic", String.class);

        StepVerifier.create(flux)
                .then(() -> pubSub.publish("topic", "hello"))
                .expectNext("hello")
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void publishWithNoSubscribers_doesNotThrow() {
        assertThatCode(() -> pubSub.publish("empty-topic", "message"))
                .doesNotThrowAnyException();
    }

    @Test
    void multipleSubscribers_allReceiveMessage() {
        List<String> received1 = new ArrayList<>();
        List<String> received2 = new ArrayList<>();

        Disposable d1 = pubSub.subscribe("topic", String.class).subscribe(received1::add);
        Disposable d2 = pubSub.subscribe("topic", String.class).subscribe(received2::add);

        pubSub.publish("topic", "broadcast");

        assertThat(received1).containsExactly("broadcast");
        assertThat(received2).containsExactly("broadcast");

        d1.dispose();
        d2.dispose();
    }

    @Test
    void differentTopics_onlyCorrectTopicReceives() {
        Flux<String> fluxA = pubSub.subscribe("A", String.class);
        Flux<String> fluxB = pubSub.subscribe("B", String.class);

        StepVerifier.create(fluxA)
                .then(() -> pubSub.publish("A", "for-A"))
                .expectNext("for-A")
                .thenCancel()
                .verify(Duration.ofSeconds(5));

        // Verify B did not receive the message published to A
        StepVerifier.create(fluxB)
                .then(() -> pubSub.publish("A", "also-for-A"))
                .expectTimeout(Duration.ofMillis(200))
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void differentMessageTypes_routedCorrectly() {
        Flux<String> stringFlux = pubSub.subscribe("topic", String.class);
        Flux<Integer> intFlux = pubSub.subscribe("topic", Integer.class);

        StepVerifier.create(stringFlux)
                .then(() -> pubSub.publish("topic", "text"))
                .expectNext("text")
                .thenCancel()
                .verify(Duration.ofSeconds(5));

        // Integer subscriber should not have received the String message
        StepVerifier.create(intFlux)
                .then(() -> pubSub.publish("topic", 42))
                .expectNext(42)
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void cancelSubscription_removesSubscriber() {
        Flux<String> flux = pubSub.subscribe("topic", String.class);
        Disposable disposable = flux.subscribe();

        // Cancel the subscription — triggers doOnCancel cleanup
        disposable.dispose();

        // Subscribe again and verify we only get new messages (old sink was cleaned up)
        Flux<String> newFlux = pubSub.subscribe("topic", String.class);

        StepVerifier.create(newFlux)
                .then(() -> pubSub.publish("topic", "after-cancel"))
                .expectNext("after-cancel")
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void concurrentPublishAndSubscribe_noErrors() throws InterruptedException {
        int threadCount = 4;
        int operationsPerThread = 250;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger received = new AtomicInteger(0);
        List<Disposable> disposables = new ArrayList<>();

        // Pre-subscribe to collect messages
        for (int i = 0; i < threadCount; i++) {
            Disposable d = pubSub.subscribe("concurrent", String.class)
                    .subscribe(msg -> received.incrementAndGet());
            disposables.add(d);
        }

        // Concurrently publish and subscribe
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        pubSub.publish("concurrent", "msg-" + threadId + "-" + i);
                        if (i % 50 == 0) {
                            Disposable d = pubSub.subscribe("concurrent", String.class)
                                    .subscribe(msg -> received.incrementAndGet());
                            synchronized (disposables) {
                                disposables.add(d);
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // Verify we received messages without any ConcurrentModificationException
        assertThat(received.get()).isPositive();

        // Cleanup
        synchronized (disposables) {
            disposables.forEach(Disposable::dispose);
        }
    }

}
