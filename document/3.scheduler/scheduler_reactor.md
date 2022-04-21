### Spring WebFlux Series - 5

우리는 이전 장 [Scheduler - Basic](https://imprint.tistory.com/210) 에서 reactivestreams의 Publisher와 Subscriber를 사용하여 Scheduler를 구현하였다.
이번 장에서는 동일한 기능을 Spring Reactor를 사용하여 구현해본다.
모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/webflux)의 테스트 코드에 있으므로 필요하다면 참고하도록 한다.

---

### 개요

직접 Publisher와 Subscriber를 구현하지 않고 스프링의 Reactor를 사용하면 손쉽게 스케쥴러를 구현할 수 있다.
하지만 실제로 스케쥴러의 종류는 다양하고 사용가능한 스레드풀의 종류도 다양하다.
여러 종류의 스케쥴러를 사용하여 레이턴시를 낮추거나 애플리케이션의 실행 속도를 빠르게 만드는 방법 등 여러가지 튜닝 요소가 있다.
이외에도 무거운 작업의 우선순위를 뒤로 밀어서 최대한 천천히 처리하는 방법, 논블로킹 IO를 사용하여 스레드를 빠르게 반납하게 유도하여 스레드를 효율적으로 운용하는 방법이 있다.
여러 종류의 Publisher가 복합적으로 구성되어 있을 때 특정 Publisher만 다른 스케쥴러를 사용하도록 하여 전반적인 성능의 향상을 이끌어 낼 수도 있다.

Reactive Programming의 고수가 되기 위해서는 이러한 많은 요소를 고려해야한다.
애플리케이션의 크기가 커지면 어떠한 부분을 비동기로 처리해야 애플리케이션 전반에 걸쳐서 성능이 향상될지 보는 눈을 키우기 위해서는 이미 완성되어 있는 Reactor를 사용하기보다 근간 기술인 Publisher와 Subscriber로 먼저 구현하는 것이 좋다.
물론 필자가 그런 고수라는 것은 아니고 필자가 참고한 강의의 강사인 "스프링 그 자체인 토비"님의 말씀을 정리하였다.

---

### Reactor 변환

본격적으로 기존의 코드를 Reactor로 변환해보도록 한다.
기존에 테스트 클래스에 테스트 메서드를 정의하여 진행하던 것과는 다르게 테스트 클래스에 메인 메서드를 생성하여 테스트를 진행한다. 이유는 아래에서 따로 설명한다.

#### subscribeOn
```java
static class SubscribeOnTest {
    public static void main(String[] args) {
        Flux.range(1, 10)
                .subscribeOn(Schedulers.newSingle("subscribe-on"))
                .subscribe(i -> log.info("{}", i));
        log.info("EXIT");
    }
}
```

출력 결과는 아래와 같으며 새로운 스레드에서 처리된 것을 확인할 수 있다.

```bash
[main] ~~(중략)~~ - EXIT
[subscribe-on-1] ~~(중략)~~ - 1
[subscribe-on-1] ~~(중략)~~ - 2
[subscribe-on-1] ~~(중략)~~ - 3
[subscribe-on-1] ~~(중략)~~ - 4
[subscribe-on-1] ~~(중략)~~ - 5
[subscribe-on-1] ~~(중략)~~ - 6
[subscribe-on-1] ~~(중략)~~ - 7
[subscribe-on-1] ~~(중략)~~ - 8
[subscribe-on-1] ~~(중략)~~ - 9
[subscribe-on-1] ~~(중략)~~ - 10
```

---

#### publishOn
```java
static class PublishOnTest {
    public static void main(String[] args) {
        Flux.range(1, 10)
                .publishOn(Schedulers.newSingle("publish-on"))
                .subscribe(i -> log.info("{}", i));
        log.info("EXIT");
    }
}
```

출력 결과는 아래와 같으며 subscribeOn과 동일하게 새로운 스레드에 의해서 처리된 것을 확인할 수 있다.

```bash
[main] ~~(중략)~~ - EXIT
[publish-on-1] ~~(중략)~ - 1
[publish-on-1] ~~(중략)~ - 2
[publish-on-1] ~~(중략)~ - 3
[publish-on-1] ~~(중략)~ - 4
[publish-on-1] ~~(중략)~ - 5
[publish-on-1] ~~(중략)~ - 6
[publish-on-1] ~~(중략)~ - 7
[publish-on-1] ~~(중략)~ - 8
[publish-on-1] ~~(중략)~ - 9
[publish-on-1] ~~(중략)~ - 10
```

---

#### publishOn & subscribeOn

이번에는 publishOn과 subscribeOn을 함께 사용해본다.

```java
static class PubSubComplexTest {
    public static void main(String[] args) {
        Flux.range(1, 10)
                .publishOn(Schedulers.newSingle("publish-on"))
                .log()
                .subscribeOn(Schedulers.newSingle("subscribe-on"))
                .subscribe(i -> log.info("{}", i));
        log.info("EXIT");
    }
}
```

우리가 이전 장에서 구현했던 스케쥴러와 동일하게 EXIT는 메인 스레드에 의해 출력되었으며 onSubscribe, request는 subscribeOn의 스레드에 의해 처리되었다.
onNext는 publishOn의 스레드에 의해 처리되었다.

```bash
[main] ~~(중략)~~ - EXIT
[subscribe-on-1] ~~(중략)~~ onSubscribe([Fuseable] FluxPublishOn.PublishOnSubscriber)
[subscribe-on-1] ~~(중략)~~ request(unbounded)
[publish-on-2] ~~(중략)~~ onNext(1)
[publish-on-2] ~~(중략)~~ - 1
[publish-on-2] ~~(중략)~~ onNext(2)
[publish-on-2] ~~(중략)~~ - 2
[publish-on-2] ~~(중략)~~ onNext(3)
[publish-on-2] ~~(중략)~~ - 3
[publish-on-2] ~~(중략)~~ onNext(4)
[publish-on-2] ~~(중략)~~ - 4
[publish-on-2] ~~(중략)~~ onNext(5)
[publish-on-2] ~~(중략)~~ - 5
[publish-on-2] ~~(중략)~~ onNext(6)
[publish-on-2] ~~(중략)~~ - 6
[publish-on-2] ~~(중략)~~ onNext(7)
[publish-on-2] ~~(중략)~~ - 7
[publish-on-2] ~~(중략)~~ onNext(8)
[publish-on-2] ~~(중략)~~ - 8
[publish-on-2] ~~(중략)~~ onNext(9)
[publish-on-2] ~~(중략)~~ - 9
[publish-on-2] ~~(중략)~~ onNext(10)
[publish-on-2] ~~(중략)~~ - 10
[publish-on-2] ~~(중략)~~ onComplete()
```

---

### Reactor Interval

이전 장에서 구현했던 내용을 Reactor로 변경하는 작업은 모두 완료되었고 Flux의 다른 기능들을 한 번 살펴본다.
Reactor에는 따로 subscribeOn을 사용하지 않아도 별도의 스레드가 생성되어 요청을 처리할 수 있다.
일정 주기 간격으로 무한대의 데이터를 발생시키는 interval을 사용해본다.

```java
static class IntervalNonPrintTest {
    public static void main(String[] args) {
        Flux.interval(Duration.ofMillis(500))
                .subscribe(i -> log.info("{}", i));
        log.info("EXIT");
    }
}
```

우리의 예상대로라면 애플리케이션은 종료되지 않고 계속 숫자를 출력해야한다.
하지만 결과는 아래와 같이 애플리케이션 실행과 동시에 종료된다. 심지어 생성된 새로운 스레드는 찾아볼 수 없다.

```bash
17:52:43.427 [main] ~~(중략)~~ - Using Slf4j logging framework
17:52:43.447 [main] ~~(중략)~~ - EXIT

Process finished with exit code 0
```

구글을 통해서 원인을 찾아보면 StackOverflow와 같은 사이트에서 Thread Sleep을 사용하면 해결된다고 제안하고 있다.
우리도 Thread Sleep을 사용하여 결과를 확인해본다.

```java
static class IntervalPrintDuringMainSleepTest {
    public static void main(String[] args) throws InterruptedException {
        Flux.interval(Duration.ofMillis(500))
                .subscribe(i -> log.info("{}", i));
        log.info("EXIT");
        TimeUnit.SECONDS.sleep(5);
    }
}
```

출력된 결과를 확인해보면 여전히 우리가 예상한 결과가 아니다.
우리는 interval에 의해서 0.5초마다 우리가 종료하기 전까지 숫자가 출력하는 것을 원했지만 결과는 애플리케이션 실행 5초 이후에 종료되었다.

```bash
17:55:34.034 [main] ~~(중략)~~ - Using Slf4j logging framework
17:55:34.055 [main] ~~(중략)~~ - EXIT
17:55:34.560 [parallel-1] ~~(중략)~~ - 0
17:55:35.059 [parallel-1] ~~(중략)~~ - 1
17:55:35.558 [parallel-1] ~~(중략)~~ - 2
17:55:36.058 [parallel-1] ~~(중략)~~ - 3
17:55:36.558 [parallel-1] ~~(중략)~~ - 4
17:55:37.059 [parallel-1] ~~(중략)~~ - 5
17:55:37.558 [parallel-1] ~~(중략)~~ - 6
17:55:38.058 [parallel-1] ~~(중략)~~ - 7
17:55:38.558 [parallel-1] ~~(중략)~~ - 8
17:55:39.058 [parallel-1] ~~(중략)~~ - 9
```

"main 스레드가 종료되니까 interval을 실행시키고 있는 parallel-1 스레드도 종료되는게 맞지 않나"라고 생각할 수 있지만 **그렇지 않다.**
아래의 예제를 한번 확인해보면 Executors를 사용하여 새로운 스레드를 생성하였고 5초 이후에 Job succeeded를 출력하도록 설정하였다.
만약 main 스레드가 종료될 때 다른 모든 스레드가 종료된다면 "EXIT"만 출력되고 "Job succeeded"는 출력되지 않아야한다.
```java
static class UserThreadNonShutdownTest {
    public static void main(String[] args) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.getStackTrace();
            }
            log.info("Job succeeded");
        });
        log.info("EXIT");
    }
}
```

출력 결과를 한 번 살펴본다.

```bash
18:00:21.525 [main] ~~(중략)~~ - EXIT
18:00:26.426 [pool-1-thread-1] ~~(중략)~~ - Job succeeded
```

메인 스레드에 의해 EXIT가 출력되고 5초 후에 Job succeeded가 출력된 것을 확인할 수 있다.
결국 메인 스레드가 종료된다고 해서 모든 스레드가 종료되는 것은 아니다.

원인은 interval이 생성하는 스레드와 Executors로 생성하는 스레드의 종류가 달라서 발생하는 현상이다.
interval의 경우 데몬 스레드를 생성하는 반면 Executors는 유저 스레드를 생성한다.
유저 스레드의 경우 강제로 종료, 인터럽트를 시키거나 작업이 종료되기 전에는 자동으로 종료되지 않는다.
하지만 데몬 스레드의 경우 실행 중인 유저 스레드가 없다면 메인 스레드가 종료되면서 동시에 강제로 종료된다.
이러한 이유로 우리가 처음으로 작성한 테스트 코드는 메인 스레드가 종료되면서 interval이 생성한 데몬 스레드가 종료되어 어떠한 출력도 발생하지 않은 것이다.

interval이 생성한 데몬 스레드의 경우 수많은 유저 스레드와 함께 생성되어 사용자가 종료시키기 전까지 살아있다.
만약 interval을 데몬 스레드가 아니라 유저 스레드로 생성되는 상황에서 개발자가 스레드를 종료시키는 코드를 만들지 않았을 때의 문제를 예상해서 스프링에서 데몬 스레드로 만들었을 것으로 추측된다.

---

### Reactor Take

추가로 take operators에 대해서 알아본다.
바로 위에서 우리는 일정 주기로 데이터를 무한대로 생성하는 interval에 대해서 알아보았다.
이렇게 무한대로 데이터가 생성되는 상황에서 자신이 원하는 만큼의 데이터를 받고 싶을 때 사용하는 것이 take Operators이다.

바로 코드를 살펴본다.
Flux는 interval에 의해서 종료되지 않고 0.2초 간격으로 1씩 증가하는 숫자를 전송하고 subscribe에 의해서 출력될 것이다.

```java
static class ReactorTakeTest {
    public static void main(String[] args) throws InterruptedException {
        Flux.interval(Duration.ofMillis(200))
                .take(10)
                .subscribe(s -> log.info("{}", s));
        TimeUnit.SECONDS.sleep(5);
    }
}
```

출력 결과를 보면 아래와 같다.

```bash
18:10:12.745 [main] ~~(중략)~~ - Using Slf4j logging framework
18:10:12.971 [parallel-1] ~~(중략)~~ - 0
18:10:13.166 [parallel-1] ~~(중략)~~ - 1
18:10:13.364 [parallel-1] ~~(중략)~~ - 2
18:10:13.565 [parallel-1] ~~(중략)~~ - 3
18:10:13.766 [parallel-1] ~~(중략)~~ - 4
18:10:13.965 [parallel-1] ~~(중략)~~ - 5
18:10:14.166 [parallel-1] ~~(중략)~~ - 6
18:10:14.366 [parallel-1] ~~(중략)~~ - 7
18:10:14.565 [parallel-1] ~~(중략)~~ - 8
18:10:14.767 [parallel-1] ~~(중략)~~ - 9

Process finished with exit code 0
```

정확히 take에서 지정한 만큼의 수치만 출력되었고 애플리케이션이 종료된 것을 확인할 수 있다.
take는 limit와 유사하게 수많은 데이터에서 원하는 양 만큼의 데이터만 받고 싶을 때 유용하게 사용할 수 있다.

---

#### Reactor take 직접 구현

마지막으로 위에서 살펴본 take를 Publisher와 Subscriber를 사용하여 직접 구현해본다.
Publisher를 생성하는 코드에서 Subscription은 cancel()이 호출되어 cancelled 값이 true로 바뀌기 전까지 ScheduledExecutorService에 의해서 0.3초 마다 1씩 증가하는 수를 출력한다.
subscriber에 의해 cancel()이 호출되면 onNext()를 호출하는 것을 멈추고 스레드를 종료시킨다.
take의 역할을 하는 takePublisher를 생성하는 코드를 살펴보면 받는 데이터가 10보다 작은 경우 Subscriber에게 전달한다.
만약 10보다 크거나 같다면 Subscription의 cancel() 메서드를 호출하여 데이터를 생성하는 스레드를 종료시킨다.

```java
static class TakeImplementationTest {
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
                // onError, onComplete 생략
            });
        };

        takePublisher.subscribe(new Subscriber<Integer>() {
            // Subscriber 구현 코드 생략
        });
    }
}
```

출력 결과는 아래와 같으며 take를 사용할 때와 같은 결과가 출력되는 것을 확인할 수 있다.

```bash
18:21:16.287 [main] ~~(중략)~~ - Subscriber on subscribe
18:21:16.294 [pool-1-thread-1] ~~(중략)~~ - Subscriber on next: 0
18:21:16.596 [pool-1-thread-1] ~~(중략)~~ - Subscriber on next: 1
18:21:16.895 [pool-1-thread-1] ~~(중략)~~ - Subscriber on next: 2
18:21:17.195 [pool-1-thread-1] ~~(중략)~~ - Subscriber on next: 3
18:21:17.496 [pool-1-thread-1] ~~(중략)~~ - Subscriber on next: 4
18:21:17.795 [pool-1-thread-1] ~~(중략)~~ - Subscriber on next: 5
18:21:18.093 [pool-1-thread-1] ~~(중략)~~ - Subscriber on next: 6
18:21:18.392 [pool-1-thread-1] ~~(중략)~~ - Subscriber on next: 7
18:21:18.695 [pool-1-thread-1] ~~(중략)~~ - Subscriber on next: 8
18:21:18.994 [pool-1-thread-1] ~~(중략)~~ - Subscriber on next: 9
18:21:19.292 [pool-1-thread-1] ~~(중략)~~ - Subscriber on next: 10

Process finished with exit code 0
```

---

**참고한 강의:**

- https://www.youtube.com/watch?v=Wlqu1xvZCak&ab_channel=TobyLee