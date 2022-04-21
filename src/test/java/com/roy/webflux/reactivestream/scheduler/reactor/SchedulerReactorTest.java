package com.roy.webflux.reactivestream.scheduler.reactor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
class SchedulerReactorTest {

    @Test
    void schedulerReactorTest() {
        Flux.range(1, 10)
                .publishOn(Schedulers.newSingle("publish-on"))
                .log()
                .subscribeOn(Schedulers.newSingle("subscribe-on"))
                .subscribe(i -> log.info("{}", i));
        log.info("EXIT");
    }

    @Test
    void schedulerIntervalTest() throws InterruptedException {
        Flux.interval(Duration.ofMillis(500))
                .subscribe(i -> log.info("{}", i));
        log.info("EXIT");
        TimeUnit.SECONDS.sleep(5);
    }

    @Test
    void zzz() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                log.info("진입?");
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {}
            log.info("Job succeeded");
        });
        log.info("EXIT");
    }

    @Test
    void takeTest() throws InterruptedException {
        Flux.interval(Duration.ofMillis(200))
                .take(10)
                .subscribe(s -> log.info("{}", s));
        TimeUnit.SECONDS.sleep(10);
    }

    /**
     * Take 구현 테스트
     */
    public static void main(String[] args) {
        Publisher<Integer> publisher = (subscriber) -> {
            subscriber.onSubscribe(new Subscription() {
                int seq = 0;
                boolean cancelled = false;
                public void request(long n) {
                    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
                    executorService.scheduleAtFixedRate(() -> {
                        if (cancelled) {
                            executorService.shutdown();
                            return;
                        }
                        subscriber.onNext(seq++);
                    }, 0, 300, TimeUnit.MILLISECONDS);
                }
                public void cancel() {
                    cancelled = true;
                }
            });
        };

        Publisher<Integer> takePublisher = (subscriber) -> {
            publisher.subscribe(new Subscriber<Integer>() {
                int count = 0;
                Subscription subscription;
                public void onSubscribe(Subscription s) {
                    subscription = s;
                    subscriber.onSubscribe(s);
                }
                public void onNext(Integer integer) {
                    subscriber.onNext(integer);
                    if (count++ >= 10) {
                        subscription.cancel();
                    }
                }
                public void onError(Throwable t) {
                    subscriber.onError(t);
                }
                public void onComplete() {
                    subscriber.onComplete();
                }
            });
        };

        takePublisher.subscribe(new Subscriber<Integer>() {
            public void onSubscribe(Subscription subscription) {
                log.info("Subscriber on subscribe");
                subscription.request(Long.MAX_VALUE);
            }
            public void onNext(Integer integer) {
                log.info("Subscriber on next: {}", integer);
            }
            public void onError(Throwable t) {
                log.error("Subscriber on Error: {}", t.toString());
            }
            public void onComplete() {
                log.info("Subscriber on complete");
            }
        });
    }

}
