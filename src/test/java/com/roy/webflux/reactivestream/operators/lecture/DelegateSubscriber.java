package com.roy.webflux.reactivestream.operators.lecture;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@SuppressWarnings("rawtypes")
public class DelegateSubscriber<T, R> implements Subscriber<T> {

    Subscriber subscriber;

    public DelegateSubscriber(Subscriber<? super R> sub) {
        this.subscriber = sub;
    }

    @Override
    public void onSubscribe(Subscription sub) {
        subscriber.onSubscribe(sub);
    }

    @Override
    public void onNext(T i) {
        subscriber.onNext(i);
    }

    @Override
    public void onError(Throwable t) {
        subscriber.onError(t);
    }

    @Override
    public void onComplete() {

    }

}
