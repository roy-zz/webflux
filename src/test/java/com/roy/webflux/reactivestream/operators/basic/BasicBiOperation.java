package com.roy.webflux.reactivestream.operators.basic;

import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.function.BiFunction;

@RequiredArgsConstructor
public class BasicBiOperation implements Publisher {
    private final Publisher publisher;
    private final BiFunction biFunction;
    private final StringBuilder initValue;
    @Override
    public void subscribe(Subscriber sub) {
        SubscriberForOperation subscriberForOperation = new SubscriberForOperation(sub, biFunction, initValue);
        publisher.subscribe(subscriberForOperation);
    }
    private static class SubscriberForOperation implements Subscriber {
        private Subscriber subscriber;
        private BiFunction biFunction;
        private StringBuilder initValue;
        public SubscriberForOperation(Subscriber subscriber, BiFunction biFunction, StringBuilder initValue) {
            this.subscriber = subscriber;
            this.biFunction = biFunction;
            this.initValue = initValue;
        }
        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscriber.onSubscribe(subscription);
        }
        @Override
        public void onNext(Object o) {
            initValue = (StringBuilder) biFunction.apply(initValue, o);
        }
        @Override
        public void onError(Throwable t) {
            this.subscriber.onError(t);
            this.subscriber.onComplete();
        }
        @Override
        public void onComplete() {
            this.subscriber.onNext(initValue);
            this.subscriber.onComplete();
        }
    }
}
