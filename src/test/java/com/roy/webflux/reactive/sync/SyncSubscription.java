package com.roy.webflux.reactive.sync;

import static java.util.concurrent.Flow.Subscriber;
import static java.util.concurrent.Flow.Subscription;

public class SyncSubscription implements Subscription {

    private final Subscriber<Integer> subscriber;

    public SyncSubscription(Subscriber<Integer> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void request(long n) {
        try {
            for (int i = 0; i < n; i++) {
                subscriber.onNext(i);
            }
        } catch (Throwable e) {
            subscriber.onError(e);
        }
    }

    @Override
    public void cancel() {

    }

}
