package com.roy.webflux.reactivestream.reactivestreams.async;

import com.roy.webflux.reactivestream.reactivestreams.AsyncReactiveTest;

import java.util.Random;

import static java.util.concurrent.Flow.Subscriber;
import static java.util.concurrent.Flow.Subscription;

public class AsyncSubscriber implements Subscriber<AsyncJob> {
    private static final Random RANDOM = new Random();

    @Override
    public void onSubscribe(Subscription subscription) {
        while(Boolean.FALSE == AsyncReactiveTest.IS_COMPLETE.get()) {
            subscription.request(RANDOM.nextInt(5));
        }
    }

    @Override
    public void onNext(AsyncJob item) {
        System.out.printf("Call onNext: %s, Thread Name: %s%n", item, getThreadName());
        item.process();
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.printf("Call onError Thread Name: %s%n", getThreadName());
    }

    @Override
    public void onComplete() {
        System.out.printf("Call onComplete Thread Name: %s%n", getThreadName());
    }

    private String getThreadName() {
        return Thread.currentThread().getName();
    }
}
