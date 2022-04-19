package com.roy.webflux.reactivestream.operators.basic;

import lombok.RequiredArgsConstructor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class BasicSubscription<T> implements Subscription {
    private final Subscriber<T> subscriber;
    @Override
    public void request(long n) {
        List<T> jobs = (List<T>) getJobs();
        jobs.forEach(job -> {
            try {
                subscriber.onNext(job);
            } catch (Throwable t) {
                subscriber.onError(t);
            }
        });
        subscriber.onComplete();
    }

    @Override
    public void cancel() {}

    private List<Integer> getJobs() {
        return Stream
                .iterate(1, a -> a + 1)
                .limit(10)
                .collect(Collectors.toList());
    }
}
