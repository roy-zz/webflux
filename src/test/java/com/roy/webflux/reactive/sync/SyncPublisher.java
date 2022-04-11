package com.roy.webflux.reactive.sync;

import static java.util.concurrent.Flow.Publisher;
import static java.util.concurrent.Flow.Subscriber;

public class SyncPublisher implements Publisher<Integer> {
    @Override
    public void subscribe(Subscriber subscriber) {
        SyncSubscription syncSubscription = new SyncSubscription(subscriber);
        subscriber.onSubscribe(syncSubscription);
    }
}
