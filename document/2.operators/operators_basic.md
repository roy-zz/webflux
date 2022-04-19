### Spring WebFlux Series - 2

[이전 장](https://imprint.tistory.com/186) 에서는 Reactive Streams의 핵심 기술인 Observer 패턴과 Pub, Sub 구조에 대해서 알아보았다.
이번 장에서는 Reactive Streams의 Operators에 대해서 알아본다.
모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/webflux)의 테스트 코드에 있으므로 필요하다면 참고하도록 한다.

---

### 개요

이전 장에서는 java.util.concurrent.Flow.Subscriber와 java.util.concurretn.Flow.Publisher를 사용하여 간단한 Pub, Sub 구조에 대해서 알아보았다.
이번 장에서는 일반 클래스로 java.reactivestreams의 Subscriber와 java reactivestreams의 Publisher를 구현하여 중간에서 데이터를 변경하는 Operators를 조합하여 조금 복잡한 구조를 만들어본다.

다음 장에서는 일반 클래스로 생성한 클래스 파일을 익명 클래스와 람다 표현식으로 리펙토링을 진행한다.
마지막으로는 같은 기능을 Reactor의 Flux를 사용하여 구현해보고 우리가 만든 코드와 얼마나 다른지 비교해본다.

java reactivestrams의 Subscriber와 Publisher를 build.gradle에 아래의 의존성을 추가해야 사용가능하다.
필자는 스프링 부트를 사용해서 의존성을 설치하였으며 스프링 부트 이외의 방법은 다루지 않는다.

```bash
implementation("org.springframework.boot:spring-boot-starter-webflux")
```

---

### Operators

간단하게 Function하고 BiFunction을 살펴본다.

Function에서 T는 입력받을 타입을 의미하며 R은 연산 결과의 타입을 의미한다.

```java
// Type parameters:
// <T> – the type of the input to the function
// <R> – the type of the result of the function
@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);
    // 이하 생략
}
```

BiFunction의 경우 T는 입력받을 타입의 첫번째 파라미터의 타입, U는 두번째 파라미터의 타입이며 R은 연산의 결과를 의미한다.

```java
// Type parameters:
// <T> – the type of the first argument to the function
// <U> – the type of the second argument to the function
// <R> – the type of the result of the function
@FunctionalInterface
public interface BiFunction<T, U, R> {
    R apply(T t, U u);
    // 이하 생략
}
```

이번 장에서는 이러한 Function과 BiFunction을 구현하여 Operators를 만들 것이다.

---

### 일반 클래스 파일을 만들어서 구현

일반 Function을 사용하여 입력받은 값에 *2 연산을 하는 Operators를 적용시킨 모습은 아래와 같다.
코드는 아래에서 살펴볼 것이므로 데이터의 흐름만 파악하면 된다.

![](image/basic-function.png)

BiFunction을 사용하여 입력받은 값들을 StringBuilder를 사용하여 합쳐서 한번에 출력하는 Operators를 적용시킨 모습은 아래와 같다.

![](image/basic-bi-function.png)

이제 클래스 파일을 하나씩 살펴본다.

**BasicPublisher**

생성되는 시점에 Subscription 객체를 생성하고 자신을 구독하는 subscriber의 onSubscribe 메서드의 파라미터로 전달한다.

```java
public class BasicPublisher implements Publisher {
    @Override
    public void subscribe(Subscriber subscriber) {
        BasicSubscription basicSubscription = new BasicSubscription(subscriber);
        subscriber.onSubscribe(basicSubscription);
    }
}
```

**BasicSubscriber**

일반 Subscriber는 큰 차이가 없다. 테스트를 위해서 subscription에게 모든 데이터를 처리할 수 있다는 의미에서 Long.MAX_VALUE를 전달한다.

```java
@Slf4j
public class BasicSubscriber implements Subscriber {
    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }
    @Override
    public void onNext(Object o) {
        log.info("Basic Subscriber On Next: {}", o);
    }
    @Override
    public void onError(Throwable t) {
        log.error("Basic Subscriber On Error: {}", t.toString());
    }
    @Override
    public void onComplete() {
        log.info("Basic Subscriber On Complete");
    }
}
```

**BasicSubscription**

코드가 조금 길어보일 수 있지만 단순히 1 ~ 10을 출력하는 Stream 객체를 생성하여 subscriber에게 전달하는 역할을 한다.
~~(이쯤되니 Publisher는 Subsription객체를 Subscriber에게 제공하는 역할만 하는거 아닌가싶다..)~~

```java
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
```

**BasicOperation**

BasicOperation은 Publisher의 구현체다.
정확한 역할은 새로운 Operators 역할을 하는 Subscriber를 생성하고 BasicPublisher에게 구독을 걸어주는 역할을 한다.
Subscriber을 내부 클래스로 가지고 있으면서 Publisher와 Subscriber 두 개의 역할을 한다. 

```java
@RequiredArgsConstructor
public class BasicOperation implements Publisher {
    private final Publisher publisher;
    private final Function<Integer, Integer> function;
    @Override
    public void subscribe(Subscriber sub) {
        SubscriberForOperation subscriberForOperation = new SubscriberForOperation(sub, function);
        publisher.subscribe(subscriberForOperation);
    }
    @RequiredArgsConstructor
    private class SubscriberForOperation implements Subscriber<Integer> {
        private final Subscriber subscriber;
        private final Function<Integer, Integer> function;
        @Override
        public void onSubscribe(Subscription subscription) {
            subscriber.onSubscribe(subscription);
        }
        @Override
        public void onNext(Integer i) {
            subscriber.onNext(function.apply(i));
        }
        @Override
        public void onError(Throwable t) {
            subscriber.onError(t);
        }
        @Override
        public void onComplete() {

        }
    }
}
```

**BasicBiOperation**

BasicOperation과 작동 원리가 동일하다. 중간에 Operators로 사용되는 Function의 종류가 변경되고 초기값이 존재한다는 차이만 있다.

```java
@RequiredArgsConstructor
public class BasicBiOperation implements Publisher {
    private final Publisher publisher;
    private final BiFunction biFunction;
    private final StringBuilder initValue;
    @Override
    public void subscribe(Subscriber sub) {
        SubscriberForOperation subscriberForOperation = new SubscriberForOperation(sub, biFunction, initValue);
        publisher.subscribe(subscriberForOperation);
    }
    private static class SubscriberForOperation implements Subscriber {
        private Subscriber subscriber;
        private BiFunction biFunction;
        private StringBuilder initValue;
        public SubscriberForOperation(Subscriber subscriber, BiFunction biFunction, StringBuilder initValue) {
            this.subscriber = subscriber;
            this.biFunction = biFunction;
            this.initValue = initValue;
        }
        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscriber.onSubscribe(subscription);
        }
        @Override
        public void onNext(Object o) {
            initValue = (StringBuilder) biFunction.apply(initValue, o);
        }
        @Override
        public void onError(Throwable t) {
            this.subscriber.onError(t);
            this.subscriber.onComplete();
        }
        @Override
        public void onComplete() {
            this.subscriber.onNext(initValue);
            this.subscriber.onComplete();
        }
    }
}
```

*2 연산을 하는 Operators를 테스트하는 테스트 코드는 아래와 같다.

```java
public class BasicTest {
    @Test
    void basicPubSubFunctionTest() {
        BasicSubscriber basicSubscriber = new BasicSubscriber();
        BasicPublisher basicPublisher = new BasicPublisher();
        Function<Integer, Integer> basicFunction = new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer integer) {
                return integer * 2;
            }
        };
        BasicOperation basicOperation = new BasicOperation(basicPublisher, basicFunction);
        basicOperation.subscribe(basicSubscriber);
    }
}
```

출력 결과는 우리가 예상한 것과 같이 *2되어서 출력된다.

```bash
Basic Subscriber On Next: 2
Basic Subscriber On Next: 4
Basic Subscriber On Next: 6
Basic Subscriber On Next: 8
Basic Subscriber On Next: 10
Basic Subscriber On Next: 12
Basic Subscriber On Next: 14
Basic Subscriber On Next: 16
Basic Subscriber On Next: 18
Basic Subscriber On Next: 20
```

입력받은 결과를 하나로 합쳐서 출력하는 Operators 테스트하는 테스트 코드는 아래와 같다.

```java
public class BasicTest {
    @Test
    void basicPubSubBiFunctionTest() {
        BasicSubscriber basicSubscriber = new BasicSubscriber();
        BasicPublisher basicPublisher = new BasicPublisher();
        BiFunction<StringBuilder, Integer, StringBuilder> basicBiFunction = new BiFunction<StringBuilder, Integer, StringBuilder>() {
            @Override
            public StringBuilder apply(StringBuilder sb, Integer integer) {
                return sb.append(integer).append(", ");
            }
        };
        BasicBiOperation basicBiOperation = new BasicBiOperation(basicPublisher, basicBiFunction, new StringBuilder());
        basicBiOperation.subscribe(basicSubscriber);
    }
}
```

출력 결과는 우리가 예상한 것과 같이 하나의 문자열로 합쳐져서 출력된다.

```bash
Basic Subscriber On Next: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 
Basic Subscriber On Complete
```

---

이번 장에서는 java.reactivestreams의 Subscriber와 Publisher를 구현하는 클래스를 작성하고 Function과 BiFunction을 구현하는 클래스 파일을 생성하여 Operators를 구현하였다.
굳이 재사용되지 않을 일회용(?) 클래스 파일이 생성되는 것을 알 수 있다.
다음 장에서는 익명 클래스와 람다 표현식을 사용하여 이러한 불편함을 제거해본다.

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