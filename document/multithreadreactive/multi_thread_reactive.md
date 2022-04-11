### Spring WebFlux Series - 2

[이전 장(링크)](https://imprint.tistory.com/186)에서는 Flow.Publisher, Flow.Subscriber를 통해서 Reactive Programming에 대해서 알아보았다.
이번 장에서는 이전 장에서 작성한 코드를 다중 쓰레드에서 사용할 수 있도록 수정하여 Reactive Streams 표준을 지키도록 수정해본다.
모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/webflux)의 테스트 코드에 있으므로 필요하다면 참고하도록 한다.

---

다중 쓰레드에서의 Reactive Programming을 테스트하기 위한 테스트 코드를 먼저 살펴본다.

**AsyncReactiveTest**
```java
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
```

5개의 AsyncSubscriber를 생성하여 AsyncPublisher에게 구독하도록 하였다.
asyncReactiveTest() 테스트 메서드 내에서 새로운 Thread를 생성하여 generateJob()을 호출하였다.
generateJob()은 테스트가 진행되는 동안에 일정주기(100ms)로 JOBS에 새로운 AsyncJob을 생성하여 집어넣는다.
설정한 크기인 100에 도달하면 더 이상 추가되는 Job이 없음을 Publisher와 Subscriber에게 알리기 위해 IS_COMPLETE 값을 true로 변경한다.

---

Subscriber가 처리해야하는 Job 클래스다.
Thread를 사용하여 Job마다 100ms의 작업 소요시간을 걸리는 것처럼 구현하였다.

**AsyncJob**
```java
@ToString
@Getter @Setter
@AllArgsConstructor
public class AsyncJob {
    private String name;
    public void process() {
        try {
            System.out.printf("AsyncJob.process Thread Name: %s%n", Thread.currentThread().getName());
            Thread.sleep(100L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

**AsyncPublisher**

```java
public class AsyncPublisher implements Publisher<AsyncJob> {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final List<Subscription> subscriptions = synchronizedList(new ArrayList<>());

    @Override
    public void subscribe(Subscriber<? super AsyncJob> subscriber) {
        executorService.execute(() -> {
            System.out.printf("Call subscribe, Thread Name: %s%n", Thread.currentThread().getName());
            AsyncSubscription asyncSubscription = new AsyncSubscription((Subscriber<AsyncJob>) subscriber, executorService);
            subscriptions.add(asyncSubscription);
            subscriber.onSubscribe(asyncSubscription);
        });
    }
}
```

---

**AsyncSubscription**

```java
public class AsyncSubscription implements Subscription {

    private final Subscriber<AsyncJob> subscriber;
    private final ExecutorService executorService;

    public AsyncSubscription(Subscriber<AsyncJob> subscriber, ExecutorService executorService) {
        this.subscriber = subscriber;
        this.executorService = executorService;
    }

    @Override
    public void request(long n) {
        int jobSize = AsyncReactiveTest.JOBS.size();
        boolean isComplete = AsyncReactiveTest.IS_COMPLETE.get();
        if (n < 0) {
            executorService.execute(() -> {
                subscriber.onError(new IllegalArgumentException());
            });
        }
        if (isComplete && jobSize == 0) {
            subscriber.onComplete();
        } else if (jobSize > 0) {
            long tempCount = n > jobSize ? n : jobSize;
            for (int i = 0; i < tempCount; i++) {
                executorService.execute(() -> {
                    AsyncJob tempJob = AsyncReactiveTest.JOBS.poll();
                    if (Objects.nonNull(tempJob)) {
                        System.out.printf("tempJob: %s, Thread Name: %s%n", tempJob, Thread.currentThread().getName());
                        subscriber.onNext(tempJob);
                    }
                });
            }
        }
    }

    @Override
    public void cancel() {
        shutdownThread();
    }

    private void shutdownThread() {
        System.out.printf("Call shutdownThread, Thread Name: %s%n", Thread.currentThread().getName());
        executorService.shutdown();
    }
}
```

**request()**: Subscriber와 동일한 라이프 사이클을 가지며 Subscriber가 Publisher에 등록될 때 새로 생성된다.
               Subscriber는 하나의 Subscription을 가지며 Subscription의 request메서드를 통해서 Publisher에게 요청이 가능하다.
               (이전 장에 설명하였지만 요청한다고 해서 Pull 방식은 아니며 자신이 작업을 처리할 수 있다고 알리는 요청이다.)

**cancel()**: Publisher에게 더 이상 데이터를 보내지 않을 것을 요청한다. cancel을 호출하기 이전에 발생한 데이터는 정상적으로 전달되므로 처리해야한다.

---

**AsyncSubscriber**

AsyncPublisher에게 구독하고 있으며 전달받은 AsyncJob을 처리하는 AsyncSubscriber를 생성한다.

```java
public class AsyncSubscriber implements Subscriber<AsyncJob> {
    private static final Random RANDOM = new Random();

    @Override
    public void onSubscribe(Subscription subscription) {
        while(Boolean.FALSE == AsyncReactiveTest.IS_COMPLETE.get()) {
            subscription.request(RANDOM.nextInt(5));
        }
    }

    @Override
    public void onNext(AsyncJob item) {
        System.out.printf("Call onNext: %s, Thread Name: %s%n", item, getThreadName());
        item.process();
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.printf("Call onError Thread Name: %s%n", getThreadName());
    }

    @Override
    public void onComplete() {
        System.out.printf("Call onComplete Thread Name: %s%n", getThreadName());
    }

    private String getThreadName() {
        return Thread.currentThread().getName();
    }
}
```

**onSubscribe()**: Publisher에 Subscriber를 등록하면 Subscriber는 onSubscriber를 호출하며 Subscription을 주입받는다.
                   Subscriber는 작업을 처리할 준비가 되면 주입받은 Sbuscription의 request 메서드를 호출하여 작업을 처리할 준비가 되었음을 알린다.
                   이때 request의 Argument로 자신이 처리할 수 있는 양을 지정하여 Publisher에게 전달한다.

**onNext()**: 실제로 데이터를 받아와서 처리하는 로직이 존재해야한다.

**onComplete()**: 작업이 정상적으로 완료되어 종료됨을 알린다. Subscription의 request가 호출되어도 더 이상 이벤트를 전송하지 않는다.

**onError()**: 에러가 발생하여 종료되었음을 알린다. Subscription의 request가 호출되어도 더 이상 에빈트를 전송하지 않는다.

---

출력 결과는 아래와 같다.

```bash
Test Thread: main
Call subscribe, Thread Name: pool-2-thread-1
Call subscribe, Thread Name: pool-2-thread-2
Call subscribe, Thread Name: pool-2-thread-3
Call subscribe, Thread Name: pool-2-thread-4
Call subscribe, Thread Name: pool-2-thread-5
tempJob: AsyncJob(name=0), Thread Name: pool-2-thread-6
tempJob: AsyncJob(name=1), Thread Name: pool-2-thread-8
Call onNext: AsyncJob(name=1), Thread Name: pool-2-thread-8
Call onNext: AsyncJob(name=0), Thread Name: pool-2-thread-6
AsyncJob.process Thread Name: pool-2-thread-6
AsyncJob.process Thread Name: pool-2-thread-8
# 중략 ----
tempJob: AsyncJob(name=98), Thread Name: pool-2-thread-2
Call onNext: AsyncJob(name=98), Thread Name: pool-2-thread-2
AsyncJob.process Thread Name: pool-2-thread-2
tempJob: AsyncJob(name=99), Thread Name: pool-2-thread-1
Call onNext: AsyncJob(name=99), Thread Name: pool-2-thread-1
AsyncJob.process Thread Name: pool-2-thread-1
```

출력을 살펴보면 다른 부분은 예상대로 작동하였지만 onComplete()가 호출되지 않은 것을 확인할 수 있다.
이 부분은 아직 필자도 원인을 파악하지 못하였으며 추후 원인을 파악하면 추가로 원인에 대해서 글을 작성하도록 하겠다.

---

참고 강의:
- https://www.youtube.com/watch?v=8fenTR3KOJo&ab_channel=TobyLee

참고 문서:
- http://www.reactive-streams.org/
- http://www.reactive-streams.org/reactive-streams-1.0.3-javadoc/org/reactivestreams/package-summary.html
- https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.3/README.md#specification
- https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html
- https://reactivex.io/
- https://grpc.io/
- https://bgpark.tistory.com/160
- https://gunju-ko.github.io/reactive/2018/07/18/Reactive-Streams.html