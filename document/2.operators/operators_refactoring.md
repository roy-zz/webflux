### Spring WebFlux Series - 3

[이전 장](https://imprint.tistory.com/210) 에서는 reactivestreams의 Subscriber와 Publisher를 일반 클래스에서 구현하여 Operators를 구현해보았다.
이렇게 구현하는 과정에서 많은 클래스들이 생기는 불편함과 가독성이 떨어지는 불편함이 발생하였다.
이번 장에서는 람다 표현식을 사용하여 이러한 불편함을 해결해보고 Reactor의 Flux로 우리가 만든 기능을 똑같이 구현해보고 어떠한 차이가 있는지 살펴본다.
모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/webflux)의 테스트 코드에 있으므로 필요하다면 참고하도록 한다.

---

### 람다 표현식으로 변환

기존에 일반 클래스로 작성되어 있던 코드를 전부 람다 표현식으로 변경하였다.

getLambdaPublisher는 1 ~ 10을 출력하는 Subscription 객체를 가지는 Publisher를 반환한다.
getLambdaSubscriber는 Subscription에게 모든 데이터를 처리할 수 있다는 의미의 Long.MAX_VALUE를 파라미터로 전달하는 Subscriber를 반환한다.
테스트 코드를 보면 Function을 구현하는 코드도 단 한 줄로 정리가 되었다.

```java
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
```

출력 결과는 이전 장에서 보았던 결과와 동일하다.

```bash
Lambda Subscriber onSubscribe
Lambda Subscriber onNext: 2
Lambda Subscriber onNext: 4
Lambda Subscriber onNext: 6
Lambda Subscriber onNext: 8
Lambda Subscriber onNext: 10
Lambda Subscriber onNext: 12
Lambda Subscriber onNext: 14
Lambda Subscriber onNext: 16
Lambda Subscriber onNext: 18
Lambda Subscriber onNext: 20
Lambda Subscriber onComplete
```

데이터를 모아서 하나의 문자열로 합치는 Operators를 만드는 코드도 아래와 같이 변경되었다.
코드가 중복되는 부분은 생략한다.

```java
@Slf4j
public class LambdaTest {
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
}
```

출력 결과는 이전 장과 동일하게 출력된다.

```bash
Lambda Subscriber onSubscribe
Lambda Subscriber onNext: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 
Lambda Subscriber onComplete
```

### Reactor로 변환

이번에는 우리가 만들었던 코드와 동일한 역할을 하는 Reactor의 Flux를 생성해본다.

입력된 값에 *2 연산을 하는 Operators를 추가하는 방법은 아래와 같다.

```java
@Slf4j
public class ReactorTest {
    @Test
    void reactorOperatorsTest() {
        Flux.create(emitter -> {
            Stream.iterate(1, a -> a + 1)
                    .limit(10)
                    .forEach(job -> emitter.next(job * 2));
            emitter.complete();
        }).subscribe(s -> log.info("{}", s));
    }
    private List<Integer> getJobs() {
        return Stream
                .iterate(1, a -> a + 1)
                .limit(10)
                .collect(Collectors.toList());
    }
}
```

출력된 결과는 아래와 같다.

```bash
ReactorTest - 2
ReactorTest - 4
ReactorTest - 6
ReactorTest - 8
ReactorTest - 10
ReactorTest - 12
ReactorTest - 14
ReactorTest - 16
ReactorTest - 18
ReactorTest - 20
```

입력된 결과를 하나의 문자열로 합쳐서 출력하는 Operators를 사용하는 방법은 아래와 같다.

```java
@Slf4j
public class ReactorTest {
    @Test
    void reactorBiOperatorsTest() {
        Flux.create(emitter -> {
            Stream.iterate(1, a -> a + 1)
                    .limit(10)
                    .forEach(emitter::next);
            emitter.complete();
        })
                .reduce(new StringBuilder(), (a, b) -> a.append(b).append(", "))
                .subscribe(s -> log.info("{}", s));
    }
    private List<Integer> getJobs() {
        return Stream
                .iterate(1, a -> a + 1)
                .limit(10)
                .collect(Collectors.toList());
    }
}
```

출력된 결과는 우리가 만든 Operators와 동일하다.

```bash
ReactorTest - 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 
```

중간에 .log() Operators를 추가하여 내부에서 어떻게 호출되는지 확인 가능하다.

```java
@Slf4j
public class ReactorTest {
    @Test
    void reactorOperatorsTest() {
        Flux.create(emitter -> {
                    Stream.iterate(1, a -> a + 1)
                            .limit(10)
                            .forEach(job -> emitter.next(job * 2));
                    emitter.complete();
                })
                .log()
                .subscribe(s -> log.info("{}", s));
    }
}
```

출력 결과

```bash
reactor.Flux.Create.1 - onSubscribe(FluxCreate.BufferAsyncSink)
reactor.Flux.Create.1 - request(unbounded)
reactor.Flux.Create.1 - onNext(2)
com.roy.webflux.reactivestream.operators.reactor.ReactorTest - 2
reactor.Flux.Create.1 - onNext(4)
com.roy.webflux.reactivestream.operators.reactor.ReactorTest - 4
reactor.Flux.Create.1 - onNext(6)
com.roy.webflux.reactivestream.operators.reactor.ReactorTest - 6
reactor.Flux.Create.1 - onNext(8)
com.roy.webflux.reactivestream.operators.reactor.ReactorTest - 8
reactor.Flux.Create.1 - onNext(10)
com.roy.webflux.reactivestream.operators.reactor.ReactorTest - 10
reactor.Flux.Create.1 - onNext(12)
com.roy.webflux.reactivestream.operators.reactor.ReactorTest - 12
reactor.Flux.Create.1 - onNext(14)
com.roy.webflux.reactivestream.operators.reactor.ReactorTest - 14
reactor.Flux.Create.1 - onNext(16)
com.roy.webflux.reactivestream.operators.reactor.ReactorTest - 16
reactor.Flux.Create.1 - onNext(18)
com.roy.webflux.reactivestream.operators.reactor.ReactorTest - 18
reactor.Flux.Create.1 - onNext(20)
com.roy.webflux.reactivestream.operators.reactor.ReactorTest - 20
reactor.Flux.Create.1 - onComplete()
```

우리가 주의깊게 봐야하는 부분은 직접 subscribe, request, onSubscriber등 어떠한 메서드도 호출하지 않았다.
스프링에서 알아서 모든 것을 처리해준다는 부분이다.
우리는 이미 직접 Publisher와 Subscriber를 사용하여 같은 기능을 구현해보았기 때문에 내부에서 어떠한 방식으로 작동하는지 예상해볼 수 있다.

---

참고 강의:
- https://www.youtube.com/watch?v=8fenTR3KOJo&ab_channel=TobyLee

참고 문서:
- https://projectreactor.io/docs/core/release/api/ (All Classes -> Flux)
- http://www.reactive-streams.org/
- http://www.reactive-streams.org/reactive-streams-1.0.3-javadoc/org/reactivestreams/package-summary.html
- https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.3/README.md#specification
- https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html
- https://reactivex.io/
- https://grpc.io/
- https://bgpark.tistory.com/160
- https://gunju-ko.github.io/reactive/2018/07/18/Reactive-Streams.html