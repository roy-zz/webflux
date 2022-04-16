package com.roy.webflux.reactivestream.reactivestreams;

import com.roy.webflux.reactivestream.reactivestreams.async.AsyncJob;
import com.roy.webflux.reactivestream.reactivestreams.async.AsyncPublisher;
import com.roy.webflux.reactivestream.reactivestreams.async.AsyncSubscriber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReactiveTest {

    public static final AtomicBoolean IS_COMPLETE = new AtomicBoolean(false);
    public static final Queue<AsyncJob> JOBS = new ConcurrentLinkedQueue<>();

    @Test
    @DisplayName("Async Reactive 테스트")
    void asyncReactiveTest() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(this::generateJob);
        AsyncPublisher asyncPublisher = new AsyncPublisher();
        List<AsyncSubscriber> subscribers = List.of(
                new AsyncSubscriber(), new AsyncSubscriber(), new AsyncSubscriber(), new AsyncSubscriber(), new AsyncSubscriber()
        );
        System.out.printf("Test Thread: %s%n", Thread.currentThread().getName());
        for (AsyncSubscriber subscriber : subscribers) {
            asyncPublisher.subscribe(subscriber);
        }

        Thread.sleep(100000L);
    }

    private void generateJob() {
        try {
            for (int i = 0; i < 100; i++) {
                JOBS.offer(new AsyncJob(String.valueOf(i)));
                Thread.sleep(10L);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IS_COMPLETE.set(true);
        }
    }

}
