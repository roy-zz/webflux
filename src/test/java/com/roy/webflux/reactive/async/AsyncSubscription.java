package com.roy.webflux.reactive.async;

import com.roy.webflux.reactive.AsyncReactiveTest;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Flow.Subscriber;
import static java.util.concurrent.Flow.Subscription;

public class AsyncSubscription implements Subscription {

    private final Subscriber<AsyncJob> subscriber;
    private final ExecutorService executorService;

    public AsyncSubscription(Subscriber<AsyncJob> subscriber, ExecutorService executorService) {
        this.subscriber = subscriber;
        this.executorService = executorService;
    }

    @Override
    public void request(long n) {
        int jobSize = AsyncReactiveTest.JOBS.size();
        boolean isComplete = AsyncReactiveTest.IS_COMPLETE.get();
        if (n < 0) {
            executorService.execute(() -> {
                subscriber.onError(new IllegalArgumentException());
            });
        }
        if (isComplete && jobSize == 0) {
            subscriber.onComplete();
        } else if (jobSize > 0) {
            long tempCount = n > jobSize ? n : jobSize;
            for (int i = 0; i < tempCount; i++) {
                executorService.execute(() -> {
                    AsyncJob tempJob = AsyncReactiveTest.JOBS.poll();
                    if (Objects.nonNull(tempJob)) {
                        System.out.printf("tempJob: %s, Thread Name: %s%n", tempJob, Thread.currentThread().getName());
                        subscriber.onNext(tempJob);
                    }
                });
            }
        }
    }

    @Override
    public void cancel() {
        shutdownThread();
    }

    private void shutdownThread() {
        System.out.printf("Call shutdownThread, Thread Name: %s%n", Thread.currentThread().getName());
        executorService.shutdown();
    }

}
