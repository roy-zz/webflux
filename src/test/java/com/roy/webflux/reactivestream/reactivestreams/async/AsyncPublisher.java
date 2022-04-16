package com.roy.webflux.reactivestream.reactivestreams.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Collections.synchronizedList;
import static java.util.concurrent.Flow.*;

@SuppressWarnings("unchecked")
public class AsyncPublisher implements Publisher<AsyncJob> {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final List<Subscription> subscriptions = synchronizedList(new ArrayList<>());

    @Override
    public void subscribe(Subscriber<? super AsyncJob> subscriber) {
        executorService.execute(() -> {
            System.out.printf("Call subscribe, Thread Name: %s%n", Thread.currentThread().getName());
            AsyncSubscription asyncSubscription = new AsyncSubscription((Subscriber<AsyncJob>) subscriber, executorService);
            subscriptions.add(asyncSubscription);
            subscriber.onSubscribe(asyncSubscription);
        });
    }

}
