package com.roy.webflux.reactive.sync;

import static java.util.concurrent.Flow.Subscriber;
import static java.util.concurrent.Flow.Subscription;

public class SyncSubscriber implements Subscriber<Integer> {

    private Subscription subscription;
    private String name;
    private int count;

    @Override
    public void onSubscribe(Subscription subscription) {
        System.out.println("Call subscription. Thread Name: " + Thread.currentThread().getName());
        this.subscription = subscription;
        this.name = Thread.currentThread().getName();
        count = 5;
        this.subscription.request(count);
    }

    @Override
    public void onNext(Integer item) {
        System.out.println("Call onNext item: " + item + " SyncSubscriber Name: " + name);
        count--;
        if (count <= 0) {
            System.out.println("구독 종료");
            subscription.cancel();
        } else {
            System.out.println("Call subscription.request, count: " + count + " SyncSubscriber Name: " + name);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Call onError" + " SyncSubscriber Name: " + name);
    }

    @Override
    public void onComplete() {
        System.out.println("Call onComplete" + " SyncSubscriber Name: " + name);
    }

}
