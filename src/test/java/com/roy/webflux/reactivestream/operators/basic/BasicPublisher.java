package com.roy.webflux.reactivestream.operators.basic;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class BasicPublisher implements Publisher {
    @Override
    public void subscribe(Subscriber subscriber) {
        BasicSubscription basicSubscription = new BasicSubscription(subscriber);
        subscriber.onSubscribe(basicSubscription);
    }
}
