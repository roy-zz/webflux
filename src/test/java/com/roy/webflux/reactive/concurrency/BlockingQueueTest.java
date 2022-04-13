package com.roy.webflux.reactive.concurrency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlockingQueueTest {

    private final BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(65535);
    private final int numberOfThreads = 1000;
    private final ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
    private final CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

    @Test
    @DisplayName("ArrayBlockingQueue 테스트")
    void arrayBlockingQueueTest() throws InterruptedException {
        Thread.sleep(20000L);
    }

    @Test
    @DisplayName("Non Concurrency Counter 테스트")
    void nonConcurrencyCounterTest() {
        Counter counter = new Counter();
        for (int i = 0; i < 500; i++) {
            counter.increment();
        }
        assertEquals(500, counter.count);
    }

    @Test
    @DisplayName("Concurrency Counter 테스트")
    void concurrencyCounterTest() throws InterruptedException {
        Counter counter = new Counter();
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                counter.increment();
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        assertEquals(numberOfThreads, counter.count);
    }

    static class Counter {
        private int count;
        public void increment() {
            System.out.println("Thread.currentThread().getName() = " + Thread.currentThread().getName());
            int temp = count;
            count = temp + 1;
        }
    }

}
