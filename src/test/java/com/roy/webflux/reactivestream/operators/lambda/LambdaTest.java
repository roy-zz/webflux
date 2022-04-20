package com.roy.webflux.reactivestream.operators.lambda;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class LambdaTest {

    @Test
    void lambdaOperationsTest() {
        Publisher<Integer> lambdaPublisher = getLambdaPublisher(getJobs());
        Function<Integer, Integer> multiplyFunction = integer -> integer * 2;
        Publisher<Integer> multiplyOperator = subscriber -> lambdaPublisher.subscribe(new Subscriber<Integer>() {
            public void onSubscribe(Subscription s) {
                subscriber.onSubscribe(s);
            }
            public void onNext(Integer integer) {
                subscriber.onNext(multiplyFunction.apply(integer));
            }
            public void onError(Throwable t) {
                subscriber.onError(t);
            }
            public void onComplete() {
                subscriber.onComplete();
            }
        });
        multiplyOperator.subscribe(getLambdaSubscriber());
    }

    @Test
    void lambdaBiOperationsTest() {
        Publisher<Integer> lambdaPublisher = getLambdaPublisher(getJobs());
        BiFunction<StringBuilder, Integer, StringBuilder> reduceFunction = (stringBuilder, integer) -> stringBuilder.append(integer).append(", ");
        Publisher<StringBuilder> reduceOperator = subscriber -> lambdaPublisher.subscribe(new Subscriber<Integer>() {
            StringBuilder result = new StringBuilder();
            public void onSubscribe(Subscription s) {
                subscriber.onSubscribe(s);
            }

            public void onNext(Integer integer) {
                result = reduceFunction.apply(result, integer);
            }

            public void onError(Throwable t) {
                subscriber.onError(t);
            }
            public void onComplete() {
                subscriber.onNext(result);
                subscriber.onComplete();
            }
        });
        reduceOperator.subscribe(getLambdaSubscriber());
    }

    private <T> Publisher<T> getLambdaPublisher(Iterable<T> jobs) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                try {
                    jobs.forEach(subscriber::onNext);
                    subscriber.onComplete();
                } catch (Throwable t) {
                    subscriber.onError(t);
                }
            }
            @Override
            public void cancel() {}
        });
    }

    private <T> Subscriber<T> getLambdaSubscriber() {
        return new Subscriber<T>() {
            public void onSubscribe(Subscription subscription) {
                log.info("Lambda Subscriber onSubscribe");
                subscription.request(Long.MAX_VALUE);
            }
            public void onNext(T t) {
                log.info("Lambda Subscriber onNext: {}", t);
            }
            public void onError(Throwable t) {
                log.info("Lambda Subscriber onError: {}", t.toString());
            }
            public void onComplete() {
                log.info("Lambda Subscriber onComplete");
            }
        };
    }

    private List<Integer> getJobs() {
        return Stream
                .iterate(1, a -> a + 1)
                .limit(10)
                .collect(Collectors.toList());
    }

}
