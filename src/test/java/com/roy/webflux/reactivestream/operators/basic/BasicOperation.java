package com.roy.webflux.reactivestream.operators.basic;

import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.function.Function;

@RequiredArgsConstructor
public class BasicOperation implements Publisher {
    private final Publisher publisher;
    private final Function<Integer, Integer> function;
    @Override
    public void subscribe(Subscriber sub) {
        SubscriberForOperation subscriberForOperation = new SubscriberForOperation(sub, function);
        publisher.subscribe(subscriberForOperation);
    }
    @RequiredArgsConstructor
    private class SubscriberForOperation implements Subscriber<Integer> {
        private final Subscriber subscriber;
        private final Function<Integer, Integer> function;
        @Override
        public void onSubscribe(Subscription subscription) {
            subscriber.onSubscribe(subscription);
        }
        @Override
        public void onNext(Integer i) {
            subscriber.onNext(function.apply(i));
        }
        @Override
        public void onError(Throwable t) {
            subscriber.onError(t);
        }
        @Override
        public void onComplete() {

        }
    }
}
