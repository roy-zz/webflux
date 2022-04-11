package com.roy.webflux.reactive;

import com.roy.webflux.reactive.sync.SyncPublisher;
import com.roy.webflux.reactive.sync.SyncSubscriber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("deprecation")
public class ReactiveProgrammingTest {

    @Test
    @DisplayName("LSP 적용")
    void applyLsp() {
        Iterable<Integer> iterableNumbers = List.of(1, 2, 3, 4, 5);
        iterableNumbers.forEach(i -> System.out.println("i = " + i));

        Collection<Integer> collectionNumbers = List.of(1, 2, 3, 4, 5);
        collectionNumbers.forEach(i -> System.out.println("i = " + i));

        List<Integer> listNumbers = List.of(1, 2, 3, 4, 5);
        listNumbers.forEach(i -> System.out.println("i = " + i));
    }

    @Test
    @DisplayName("Iterable 구현")
    void implIterable() {
        Iterable<Integer> iterableNumbers = List.of(1, 2, 3, 4, 5);
        Iterable<Integer> customIterable = () ->
            new Iterator<>() {
                int i = 0;
                final static int MAX = 5;
                public boolean hasNext() {
                    return i < MAX;
                }
                public Integer next() {
                    return ++i;
                }
            };

        StringBuilder sBuilder1 = new StringBuilder();
        StringBuilder sBuilder2 = new StringBuilder();
        iterableNumbers.forEach(sBuilder1::append);
        customIterable.forEach(sBuilder2::append);

        assertEquals(sBuilder1.toString(), sBuilder2.toString());
    }

    @Test
    @DisplayName("Observer 구현")
    void implObserver() {
        Observer observer = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                System.out.println("arg = " + arg);
            }
        };
        ObservableNumber observableNumber = new ObservableNumber();
        observableNumber.addObserver(observer);
        observableNumber.run();
    }

    @Test
    @DisplayName("Observer 새로운 Thread 할당")
    void implObserverWithMultiThread() {
        Observer observer = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                System.out.printf("Observer Thread: %s, Arg: %s%n", Thread.currentThread().getName(), arg);
            }
        };

        ObservableNumber observableNumber = new ObservableNumber();
        observableNumber.addObserver(observer);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        System.out.println("Test Thread: " + Thread.currentThread().getName());
        executorService.execute(observableNumber);
        executorService.shutdown();
    }

    static class ObservableNumber extends Observable implements Runnable {
        @Override
        public void run() {
            for (int i = 1; i <= 5; i++) {
                setChanged();
                notifyObservers(i);
            }
        }
    }

    @Test
    @DisplayName("Sync Publisher & Subscriber")
    void publisherAndSubscriber() {
        SyncPublisher syncPublisher = new SyncPublisher();
        SyncSubscriber syncSubscriber = new SyncSubscriber();
        System.out.println("Test Thread: " + Thread.currentThread().getName());
        syncPublisher.subscribe(syncSubscriber);
    }

}
