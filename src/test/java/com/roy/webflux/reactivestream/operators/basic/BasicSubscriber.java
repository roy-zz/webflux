package com.roy.webflux.reactivestream.operators.basic;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Slf4j
public class BasicSubscriber implements Subscriber {
    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }
    @Override
    public void onNext(Object o) {
        log.info("Basic Subscriber On Next: {}", o);
    }
    @Override
    public void onError(Throwable t) {
        log.error("Basic Subscriber On Error: {}", t.toString());
    }
    @Override
    public void onComplete() {
        log.info("Basic Subscriber On Complete");
    }
}
