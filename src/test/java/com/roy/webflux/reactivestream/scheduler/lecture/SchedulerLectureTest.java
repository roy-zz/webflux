package com.roy.webflux.reactivestream.scheduler.lecture;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Slf4j
class SchedulerLectureTest {

    @Test
    void schedulerLectureTest() throws InterruptedException {
        Publisher<Integer> publisher = (subscriber) -> {
            subscriber.onSubscribe(new Subscription() {
                public void request(long n) {
                    try {
                        log.info("Publisher request");
                        Stream.iterate(1, i -> i + 1)
                                .limit(5)
                                .forEach(subscriber::onNext);
                        subscriber.onComplete();
                    } catch (Throwable t) {
                        subscriber.onError(t);
                    }
                }
                public void cancel() {}
            });
        };

//        Publisher<Integer> subscribeOnPublisher = (subscriber) -> {
//            ExecutorService es = Executors.newSingleThreadExecutor(new CustomizableThreadFactory() {
//                @Override
//                public String getThreadNamePrefix() {
//                    return "subscribe-on-";
//                }
//            });
//            es.execute(() -> publisher.subscribe(subscriber));
//        };

        final ExecutorService publishOnEs = Executors.newSingleThreadExecutor(new CustomizableThreadFactory() {
            @Override
            public String getThreadNamePrefix() {
                return "publish-on-";
            }
        });
        Publisher<Integer> publishOnPublisher = (subscriber) -> {
            publisher.subscribe(new Subscriber<Integer>() {
                public void onSubscribe(Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                }
                public void onNext(Integer integer) {
                    publishOnEs.execute(() -> subscriber.onNext(integer));
                }
                public void onError(Throwable t) {
                    publishOnEs.execute(() -> subscriber.onError(t));
                    publishOnEs.shutdown();
                }
                public void onComplete() {
                    publishOnEs.execute(subscriber::onComplete);
                    publishOnEs.shutdown();
                }
            });
        };

        publishOnPublisher.subscribe(new Subscriber<Integer>() {
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

        log.info("EXIT - Start Sleep");
        Thread.sleep(3000L);
        log.info("EXIT - End Sleep");
        log.info("ExecutorService.isShutdown: {}", publishOnEs.isShutdown());
    }

}
