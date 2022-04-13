package com.roy.webflux.reactive.operators;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reactive Streams - Operators
 *
 * Operators: 중간에 데이터를 변경하는 작업자
 * Publisher -> [Data1] -> Op2 -> [Data2] -> Op2 -> [Data3] -> Subscriber
 *
 *
 * Function은 입력을 하나를 받아서 리턴을 하나한다.
 * BiFunction은 입력을 두 개 받아서 리턴을 하나한다.
 *
 * Reduce 작동 방식
 * 1, 2, 3, 4, 5
 * 0 -> (0, 1) -> 0 + 1 = 1
 * 1 -> (1, 2) -> 1 + 2 = 3
 * 3 -> (3, 3) -> 3 + 3 = 6
 */

@Slf4j
public class OperatorTest {

    @Test
    @DisplayName("Publisher Test")
    void publisherTest() {
        Publisher<Integer> publisher = iterPublisher(getIterable());
        // Publisher<Integer> mapPublisher = mapPublisher(publisher, s -> s * 10);
        // Publisher<String> mapPublisherStr = mapPublisherStr(publisher, s -> "*" + s.toString() + "*");
        // Publisher<List> mapPublisherColl = mapPublisherColl(publisher, Collections::singletonList);
        // Publisher<Integer> sumPublisher = sumPublisher(publisher);
        // Publisher<String> reducePublisher = reducePublisher(publisher,"",  (a, b) -> a + " & " + b);
        Publisher<StringBuilder> reducePublisherSBuilder = reducePublisherSBuilder(publisher, new StringBuilder(), (a, b) -> a.append(b + ", "));
        reducePublisherSBuilder.subscribe(getSubscriber());
    }

    private <T, R> Publisher<R> reducePublisherSBuilder(Publisher<T> publisher, R initValue, BiFunction<R, T, R> biFunction) {
        return new Publisher<R>() {
            @Override
            public void subscribe(Subscriber<? super R> sub) {
                publisher.subscribe(new DelegateSubscriber<T, R>(sub) {
                    R result = initValue;
                    @Override
                    public void onNext(T i) {
                        result = biFunction.apply(result, i);
                    }
                    @Override
                    public void onComplete() {
                        sub.onNext(result);
                        sub.onComplete();
                    }
                });
            }
        };
    }

    private <T, R> Publisher<R> reducePublisher(Publisher<T> publisher, R initValue, BiFunction<R, T, R> biFunction) {
        return new Publisher<R>() {
            @Override
            public void subscribe(Subscriber<? super R> sub) {
                publisher.subscribe(new DelegateSubscriber<T, R>(sub) {
                    R result = initValue;
                    @Override
                    public void onNext(T i) {
                        result = biFunction.apply(result, i);
                    }
                    @Override
                    public void onComplete() {
                        sub.onNext(result);
                        sub.onComplete();
                    }
                });
            }
        };
    }

//    private Publisher<Integer> sumPublisher(Publisher<Integer> publisher) {
//        return new Publisher<Integer>() {
//            @Override
//            public void subscribe(Subscriber<? super Integer> sub) {
//                publisher.subscribe(new DelegateSubscriber(sub) {
//                    int sum = 0;
//                    @Override
//                    public void onNext(Integer i) {
//                        sum += i;
//                    }
//
//                    @Override
//                    public void onComplete() {
//                        sub.onNext(sum);
//                        sub.onComplete();
//                    }
//                });
//            }
//        };
//    }

    // T -> R
    private static <T, R> Publisher<R> mapPublisher(Publisher<T> publisher, Function<T, R> function) {
        return new Publisher<R>() {
            @Override
            public void subscribe(Subscriber<? super R> sub) {
                publisher.subscribe(new DelegateSubscriber<T, R>(sub) {
                    @Override
                    public void onNext(T i) {
                        subscriber.onNext(function.apply(i));
                    }
                });
            }
        };
    }

    private static <T, R> Publisher<R> mapPublisherStr(Publisher<T> publisher, Function<T, R> function) {
        return new Publisher<R>() {
            @Override
            public void subscribe(Subscriber<? super R> sub) {
                publisher.subscribe(new DelegateSubscriber<T, R>(sub) {
                    @Override
                    public void onNext(T i) {
                        subscriber.onNext(function.apply(i));
                    }
                });
            }
        };
    }

    private static <T, R> Publisher<R> mapPublisherColl(Publisher<T> publisher, Function<T, R> function) {
        return new Publisher<R>() {
            @Override
            public void subscribe(Subscriber<? super R> sub) {
                publisher.subscribe(new DelegateSubscriber<T, R>(sub) {
                    @Override
                    public void onNext(T i) {
                        subscriber.onNext(function.apply(i));
                    }
                });
            }
        };
    }

    private Publisher<Integer> iterPublisher(Iterable<Integer> numbers) {
        return subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    try {
                        numbers.forEach(subscriber::onNext);
                        subscriber.onComplete();
                    }
                    catch (Throwable t) {
                        subscriber.onError(t);
                    }
                }

                @Override
                public void cancel() {

                }
            });
        };
    }

    private List<Integer> getIterable() {
        return Stream
                .iterate(1, a -> a + 1)
                .limit(10)
                .collect(Collectors.toList());
    }

    private static <T> Subscriber<T> getSubscriber() {
        return new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription sub) {
                log.debug("onSubscribe");
                sub.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T i) {
                log.info("onNext: {}", i);
            }

            @Override
            public void onError(Throwable t) {
                log.info("onError: {}", t);
            }

            @Override
            public void onComplete() {
                log.info("onComplete");
            }
        };
    }

}
