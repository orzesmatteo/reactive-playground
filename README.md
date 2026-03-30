# Reactive PubSub

A lightweight, thread-safe, in-process publish-subscribe library built on [Project Reactor](https://projectreactor.io/).

## Usage

```java
PubSubApi pubSub = new PubSubImpl();

// Subscribe
Disposable subscription = pubSub.subscribe("orders", Order.class)
        .subscribe(order -> System.out.println("Received: " + order));

// Publish
pubSub.publish("orders", new Order("ABC-123"));

// Cleanup
subscription.dispose();
```

Messages are routed by both **topic name** and **message class**, enabling type-safe pub-sub without a framework event system.

## Design

- `PubSubApi` — interface defining `subscribe` and `publish`
- `PubSubImpl` — thread-safe implementation using Reactor `Sinks.Many`
  - `ConcurrentHashMap` + `CopyOnWriteArrayList` for lock-free reads during publish
  - Backpressure buffering via `onBackpressureBuffer()`
  - Automatic cleanup on subscription cancellation

## Adding to your project

### Gradle

```groovy
dependencies {
    implementation 'it.orz:reactive-pubsub:1.0.0'
}
```

## Build

```bash
./gradlew build
```

## Publish locally

```bash
./gradlew publishToMavenLocal
```

The JAR will be available at `~/.m2/repository/it/orz/reactive-pubsub/1.0.0/`.

## Requirements

- Java 21+
- Gradle 9.x (wrapper included)
