### Spring WebFlux Series - 6

이번 장에서는 자바와 스프링의 비동기 개발 기술 중 기본이 되는 `Future`에 대해서 알아본다.
모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/webflux)의 테스트 코드에 있으므로 필요하다면 참고하도록 한다.

---

### 개요

리액티브 프로그래밍을 Flux와 Mono를 사용하여 몇가지 API를 작성하는 것만으로 이해했다고 보기는 힘들다.
물론 보는 관점에 따라 다를 수는 있지만 핵심 기술을 이해해야 리액티브 프로그래밍을 제대로 이해하고 사용할 수 있다.
리액티브 프로그래밍을 옵저버 패턴을 사용하여 역할이 다른 관찰자와 관찰 대상자를 나누어 구현하는 구조만을 말하는 것이 아니라 비동기적인 동작 환경에서 어떠한 의미를 가지는지 살펴봐야 한다.

스프링이 10년 이상 유지한 MVC 방식에서 어떠한 문제를 해결하기 위해 Reactive 방식을 적용하기 시작하였는지에 대해서 알아봐야 한다.
자바의 경우 1.4에서 등장한 concurrent 패키지는 이후 계속 업데이트를 통해 발전하고 있다.
스프링으로 보면 출시된지 15년 정도된 2.0 버전에서 어떻게 비동기 방식을 스프링에 녹일 것인지에 대한 고찰이 있었으며 현재까지 지속적으로 발전되고 있다.
스프링은 자바의 비동기 기술과 비동기 관련 오픈소스 기술을 참고하여 스프링에 비동기 기술을 녹여왔다.
어떠한 방식으로 발전을 해왔고 자바 1.8에서의 커다란 변화와 Reactive를 도입하기까지의 중요한 일들에 대해서 살펴본다.

---

### 고전적인 방식의 비동기 프로그래밍

#### 일반 동기 방식의 프로그래밍

아래와 같이 코드를 작성하게 되면 모든 작업은 메인 스레드에서 처리하게 되고 메인 스레드는 sleep에 의해 2초간 블로킹되어 있다가 `HELLO`와 `EXIT`라는 문구는 출력하게 될 것이다.

```java
static class OnlyMainThreadTest {
    public static void main(String[] args) throws InterruptedException {
        log.info("Start Time: {}", LocalDateTime.now());
        TimeUnit.SECONDS.sleep(2);
        log.info("HELLO, Time: {}", LocalDateTime.now());
        log.info("EXIT, Time: {}", LocalDateTime.now());
        log.info("End Time: {}", LocalDateTime.now());
    }
}
```

출력 결과는 아래와 같다.

```bash
[main] Start Time: 2022-04-25T17:16:03.216159
[main] HELLO, Time: 2022-04-25T17:16:05.225627
[main] EXIT, Time: 2022-04-25T17:16:05.226046
[main] End Time: 2022-04-25T17:16:05.226249
```

---

#### 새로운 스레드를 생성하여 비동기 처리하는 방식

Java 1.5에 등장한 Future 인터페이스를 확인해본다.

Future란 비동기적인 작업의 결과를 나타내는 것이다.
스레드 자기 자신이 아닌 새로운 스레드를 생성하여 새로운 작업을 수행한다는 의미이며 다른 스레드에 의해 완성된 결과를 가져오는 대표적인 인터페이스가 Future다.

ExecutorService를 사용하여 스레드 풀을 생성한다.
newCachedThreadPool은 스레드의 갯수 제한은 없으며 요청할 때마다 새로 만들고 요청을 처리하고 스레드를 풀에 반납한다. 새로운 요청이 들어왔을 때 스레드 풀의 스레드로 요청을 처리한다.

만약 우리가 원하는 작동 방식이 "EXIT"는 바로 출력되고 "HELLO"만 2초 후에 출력되기를 바란다면 코드는 어떻게 수정이 되어야할까?
아래와 같이 스레드를 `sleep하는 코드`와 `HELLO를 출력하는 코드`를 ExecutorService를 통해서 다른 스레드에서 실행시키고 `EXIT를 출력하는 코드`를 메인 스레드에서 실행시키면 된다.
번외로 스레드의 `sleep`을 사용할 때 InterruptedException을 따로 처리해야하는 이유는 스레드가 `sleep`인 상태에서 Owner 스레드가 실행 중인 스레드를 종료 및 인터럽트 하는 경우 `InterruptedException`이 발생하기 때문이다.

```java
static class UseOtherThreadTest {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        es.execute(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
                log.info("HELLO, Time: {}", LocalDateTime.now());
            } catch (InterruptedException e) {}
        });
        log.info("EXIT, Time: {}", LocalDateTime.now());
    }
}
```

출력 결과는 아래와 같다.
Start, EXIT, END는 애플리케이션 실행과 동시에 메인 스레드에 의해 출력되었다.
HELLO를 출력하는 부분만 비동기로 다른 스레드인 pool-1-thread-1에 의해 출력된 것을 알 수 있다.
이전 예제와 같이 기존에 작업하던 스레드가 무거운 작업의 종료를 기다리며 대기하는 상태를 `블로킹 상태`라고 하며 이번 예제와 같이 무거운 작업의 종료와 상관없이 진행되는 것을 `논블로킹 상태`라고 한다. 

```bash
[main] Start Time: 2022-04-25T17:21:09.728698
[main] EXIT, Time: 2022-04-25T17:21:09.733717
[main] End Time: 2022-04-25T17:21:09.733837
[pool-1-thread-1] HELLO, Time: 2022-04-25T17:21:11.735280
```

---

#### Future 인터페이스를 활용하는 동기 방식

이번에는 다른 스레드에서 수행되는 로직이 2초 후에 `HELLO`라는 문자열을 출력하도록 구현해본다.
참고로 `Callable`은 리턴 값이 있지만 `Runnable`은 리턴 값이 없다.
리턴 값이 있다는 것은 리턴 값을 생성하는 스레드 이외의 다른 스레드에서 리턴 값을 사용하겠다는 의미가 되며 예외가 발생하는 경우 예외를 다른 스레드로 throw 할 수도 있다.
리턴 값을 바로 사용할 수 있는 것은 아니고 우리가 계속 언급하였던 Future 객체를 사용하여 받을 수 있다.
이렇게 리턴 값이 필요한 경우 우리는 `execute`메서드가 아닌 `submit`메서드를 사용해야한다.

```java
static class UseOtherThreadWithReturnTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        log.info("Start Time: {}", LocalDateTime.now());
        Future<String> future = es.submit(() -> {
            TimeUnit.SECONDS.sleep(2);
            log.info("Async, Time: {}", LocalDateTime.now());
            return "HELLO";
        });
        log.info("Future: {}, Time: {}", future.get(), LocalDateTime.now());
        log.info("EXIT, Time: {}", LocalDateTime.now());
    }
}
```

출력되는 순서를 보면 이전에 단순히 다른 스레드에서 `HELLO`를 출력한 것과는 결과가 다르다는 점이다.
결국 메인 스레드는 `Future`의 get 메서드를 호출하면서 다른 스레드의 결과를 가져올 때까지 **블로킹**되어 있었다는 의미가 된다.
만약 Future의 get 메서드를 중지시키지 않고 null과 같은 값을 일단 반환하고 결과 값이 생성되는 시점에 채워넣는다면 **논블로킹** 방식이 된다.

```bash
[main] Start Time: 2022-04-25T17:38:14.525808
[pool-1-thread-1] Async, Time: 2022-04-25T17:38:16.536348
[main] Future: HELLO, Time: 2022-04-25T17:38:16.537733
[main] EXIT, Time: 2022-04-25T17:38:16.538042
```

---

#### 다중 Futures

만약 우리의 메인 스레드가 `HELLO`를 출력하는 스레드와 동시에 작동하는 상황이라고 가정하고 `EXIT`를 출력하는 코드와 `Future를 get하는 코드`의 위치를 바꿔본다.
필자의 경우 총 세개의 Future와 스레드를 생성하고 메인 스레드와 총 세개의 스레드가 동시에 진행되어 모든 출력이 애플리케이션 실행 2초 정도 후에 출력되었는지 확인한다.

```java
    static class ManyThreadParallelTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        log.info("Start");
        ExecutorService es = Executors.newCachedThreadPool();
        Future<String> future1 = es.submit(() -> {
            TimeUnit.SECONDS.sleep(2);
            log.info("Async-1");
            return "HELLO-1";
        });
        Future<String> future2 = es.submit(() -> {
            TimeUnit.SECONDS.sleep(2);
            log.info("Async-2");
            return "HELLO-2";
        });
        Future<String> future3 = es.submit(() -> {
            TimeUnit.SECONDS.sleep(2);
            log.info("Async-3");
            return "HELLO-3";
        });
        log.info("EXIT");
        log.info("Future-1: {}", future1.get());
        log.info("Future-2: {}", future2.get());
        log.info("Future-3: {}", future3.get());
        log.info("End");
    }
}
```

메인 스레드는 Start와 EXIT를 출력하고 2초간 블로킹되어 있다가 세 개의 스레드의 작업이 끝나는 시점인 2초 후에 Future의 결과를 출력하며 종료되는 것을 확인할 수 있다.

```bash
17:59:02.831 [main] - Start Time
17:59:02.836 [main] - EXIT
17:59:04.840 [pool-1-thread-1] - Async-1
17:59:04.840 [pool-1-thread-3] - Async-3
17:59:04.840 [pool-1-thread-2] - Async-2
17:59:04.841 [main] - Future-1: HELLO-1
17:59:04.843 [main] - Future-2: HELLO-2
17:59:04.843 [main] - Future-3: HELLO-3
17:59:04.843 [main] - End
```

#### Future.isDone()

Future의 `isDone()` 메서드를 사용하여 작업의 진행 여부를 확인할 수 있다.
`get()` 메서드와는 다르게 블로킹 시키지 않기 때문에 작업이 끝났는지 확인을 해보고 작업이 끝나지 않은 경우 다른 작업을 진행하고 돌아와서 다시 작업 진행유무를 확인할 수 있다.

```java
static class IsDoneTest {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newCachedThreadPool();
        Future<String> future = es.submit(() -> {
            TimeUnit.SECONDS.sleep(2);
            log.info("Async");
            return "HELLO";
        });
        log.info("Future.isDone - 1: {}", future.isDone());
        TimeUnit.SECONDS.sleep(3);
        log.info("EXIT");
        log.info("Future.isDone - 2: {}", future.isDone());
        log.info("Future.get: {}", future.get());
    }
}
```

예시의 출력을 보면 `첫번째 isDone()`은 작업이 끝나지 않았으므로 `false`를 반환하고 `두번째 isDone()`은 작업이 끝났음을 의미하는 `true`를 반환한다.
결국 Future는 결과를 얻어올 수 있는 핸들러와 같은 역할을 하는 것이지 결과 자체는 아니다.

```bash
18:13:54.250 [main] - Future.isDone - 1: false
18:13:56.135 [pool-1-thread-1] - Async
18:13:57.253 [main] - EXIT
18:13:57.253 [main] - Future.isDone - 2: true
18:13:57.253 [main] - Future.get: HELLO
```

---

#### FutureTask

Future와 비슷한 방식으로는 Javascript에서 많이 사용되는 Callback이 있으며 비동기 프로그래밍을 구현할 때 많이 사용되는 방식이다.
자바에서는 비동기로 코드를 작성할 때 가장 기본적으로 Future와 Callback을 사용한다.
자바 1.8 이전에는 Future가 대표적으로 사용되었으나 결과를 가져올 때(get() 메서드를 호출할 때) 호출한 스레드가 블로킹된다는 사실을 많은 개발자들이 불편해하였다.
일단 FutureTask를 사용하여 코드를 작성해본다.

```java
static class FutureTaskTest {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newCachedThreadPool();
        FutureTask<String> futureTask = new FutureTask<String>(() -> {
            TimeUnit.SECONDS.sleep(2);
            log.info("Async");
            return "HELLO";
        });
        es.execute(futureTask);
        log.info("Future.isDone - 1: {}", futureTask.isDone());
        TimeUnit.SECONDS.sleep(3);
        log.info("EXIT");
        log.info("Future.isDone - 2: {}", futureTask.isDone());
        log.info("Future.get: {}", futureTask.get());
    }
}
```

FutureTask는 Future를 상속하고 있는 클래스로서 출력 결과 또한 동일하다는 것을 알 수 있다.

```bash
18:21:06.160 [main] - Future.isDone - 1: false
18:21:08.051 [pool-1-thread-1] - Async
18:21:09.168 [main] - EXIT
18:21:09.169 [main] - Future.isDone - 2: true
18:21:09.169 [main] - Future.get: HELLO
```

---

#### FutureTask done 재정의

ExecutorService.shutdown은 진행 중인 작업이 끝나면 스레드를 종료(Graceful 하게)하라는 의미이며 비동기 작업 자체가 종료되지는 않는다.
이번에는 FutureTask를 익명 클래스로 만들어본다. 우리가 재정의 할 `done`이라는 메서드는 FutureTask의 작업이 모두 완료되면 호출되는 hook같은 존재다.

```java
static class AnonymousFutureTaskTest {
    public static void main(String[] args) {
        ExecutorService es = Executors.newCachedThreadPool();
        FutureTask<String> futureTask = new FutureTask<String>(() -> {
            TimeUnit.SECONDS.sleep(2);
            log.info("Async");
            return "HELLO";
        }) {
            @Override
            protected void done() {
                try {
                    log.info("Call done: {}", get());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };
        es.execute(futureTask);
        es.shutdown();
    }
}
```

우리는 작업의 결과를 따로 출력하지 않았지만 작업이 완료되고 `done`이 호출되어 `HELLO`가 출력되는 것을 볼 수 있다.
FutureTask 또한 Future와 동일하게 자바 1.5부터 추가되어 있었으며 Callback과 비슷한 방식으로 사용할 수 있는 기법들이 이미 구현되어 있다.
하지만 매번 오버라이딩하여 `done`을 구현해야한다는 점에서 자연스러운 Callback이라고 보기는 어렵다.

```bash
18:28:47.862 [pool-1-thread-1] - Async
18:28:47.865 [pool-1-thread-1] - Call done: HELLO
```

---

#### 자연스러운 Callback 구현

이번에는 FutureTask를 사용하여 자연스러운 Callback이 되도록 CallbackFutureTask 메서드를 작성해본다.
Objects의 RequireNonNull은 검증하는 대상이 null인 경우 즉시 NullPointerException이 발생하기 때문에 절대 null값이 들어가서는 안되는 곳에 많이 사용된다.
Callback 인터페이스인 SuccessCallback을 생성한다.
물론 람다 표현식을 사용하기 때문에 자바 1.8 이전 버전은 사용이 불가능한 방식이다.

우리가 새로 정의한 CallbackFutureTask 클래스는 FutureTask를 상속받고 있으며 작업이 완료되었을 때 `done` 메서드를 호출하여 SuccessCallback 인터페이스의 onSuccess 메서드를 호출하도록 구현되어 있다.
CallbackFutureTask를 테스트하는 CallbackFutureTaskTest 코드를 살펴보면 `CallbackFutureTask` 객체를 생성 시점에 작업이 완료되면 Callback으로 진행되야하는 메서드를 전달하면서 이전보다 자연스러운 Callback을 구현하였다. 

```java
interface SuccessCallback {
    void onSuccess(String result);
}

static class CallbackFutureTask extends FutureTask<String> {
    private final SuccessCallback successCallback;
    public CallbackFutureTask(Callable<String> callable, SuccessCallback successCallback) {
        super(callable);
        this.successCallback = Objects.requireNonNull(successCallback);
    }
    @Override
    protected void done() {
        try {
            successCallback.onSuccess(get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}

static class CallbackFutureTaskTest {
    public static void main(String[] args) {
        ExecutorService es = Executors.newCachedThreadPool();
        CallbackFutureTask callbackFutureTask = new CallbackFutureTask(() -> {
            TimeUnit.SECONDS.sleep(2);
            log.info("Async");
            return "HELLO";
        }, result -> {
            log.info("SuccessCallback.onSuccess: {}", result);
        });
        es.execute(callbackFutureTask);
        es.shutdown();
    }
}
```

출력 결과는 아래와 같다.

```bash
10:26:25.276 [pool-1-thread-1] - Async
10:26:25.278 [pool-1-thread-1] - SuccessCallback.onSuccess: HELLO
```

---

#### 자연스럽고 예외처리까지 가능한 Callback 구현

우리는 이전 예제에서 CallbackFutureTask를 생성하여 Callback 형식으로 비동기로 작업을 처리하는 방법에 대해서 알아보았다.
우리는 원하는 결과를 얻어올 수는 있었지만 예외가 발생했을 때에 대한 예외처리는 따로 작성하지 않았다. 다른 스레드에서 발생한 예외를 우리는 메인 스레드로 끌어와야한다.
최근에는 try catch를 사용하는 것보다 Exception Object를 Exception을 처리하는 Callback에 던져주는 방식으로 많이 사용된다.
이번에는 예외를 처리하는 ExceptionCallback을 만들고 CallbackFutureTask가 ExceptionCallback을 생성자 파라미터로 받을 수 있도록 변경해본다.

FutureTask의 `get()`을 호출할 때 InterruptedException과 ExecutionException을 처리해야한다.
InterruptedException의 경우 다른 Exception들과는 성향이 다르다.
단순히 Interrupt 메시지를 받아도 발생하는 예외이기 때문에 스레드에 다시 인터럽트를 걸어서 처리해도 무관하다.
ExecutionException은 비동기 작업을 수행하다가 예외 상황이 생겼을 때 발생하며 우리가 처리해줘야한다.
발생한 예외를 ExceptionCallback에게 전달할 때 **ExecutionException은 한 번 가공된 예외이기 때문에 getCause를 사용하여 정확한 원인을 전달**해야한다.

```java
interface ExceptionCallback {
    void onError(Throwable t);
}

static class CallbackFutureHandleExceptionTask extends FutureTask<String> {
    private final SuccessCallback successCallback;
    private final ExceptionCallback exceptionCallback;
    public CallbackFutureHandleExceptionTask(Callable<String> callable,
                                             SuccessCallback successCallback,
                                             ExceptionCallback exceptionCallback) {
        super(callable);
        this.successCallback = Objects.requireNonNull(successCallback);
        this.exceptionCallback = Objects.requireNonNull(exceptionCallback);
    }
    @Override
    protected void done() {
        try {
            successCallback.onSuccess(get());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ee) {
            exceptionCallback.onError(ee.getCause());
        }
    }
}

static class CallbackFutureHandleExceptionTaskTest {
    public static void main(String[] args) {
        ExecutorService es = Executors.newCachedThreadPool();
        CallbackFutureHandleExceptionTask task = new CallbackFutureHandleExceptionTask(() -> {
            TimeUnit.SECONDS.sleep(2);
            if (true) throw new RuntimeException("Async Error Occurred");
            log.info("Async");
            return "HELLO";
        },
                result -> log.info("onSuccess: {}", result),
                error -> log.error("onError: {}", error.getMessage()));
        es.execute(task);
        es.shutdown();
    }
}
```

CallbackFutureHandleExceptionTaskTest의 코드를 살펴보면 일부러 RuntimeException을 발생시켜서 Exception을 처리하는 Callback으로 전달되도록 하였다.
`if (true)`를 사용하지 않는 경우 throw new RuntimeException() 이하의 코드는 unreachable 컴파일 오류가 발생하기 때문에 컴파일러를 속이기 위해서 사용하였다.

출력되는 결과는 아래와 같다.

```bash
10:41:41.695 [pool-1-thread-1] - onError: Async Error Occurred
```

---

지금까지 자바의 비동기 방식 중 고전적인 방식인 Future를 사용한 방식에 대해서 알아보았다.
정리해보면 비동기 작업의 결과를 가져오는 방법은 두 가지가 있다.
첫번째로 Future와 작업의 결과를 담고 있는 핸들러를 리턴받아서 처리하는 방식이 있다. `get()`메서드를 호출할 때 블로킹이 발생하며 예외가 발생하는 경우는 try catch를 사용하여 해결해야한다.
두번째로 Callback을 구현하여 사용하는 방법이 있다.

자바에서는 어떻게 비동기를 구현하고 있는지 `AsynchronousByteChannel` 코드를 확인해 본다.
`read()` 메서드를 확인해보면 CompletionHandler를 전달받고 있다.
CompletionHandler의 내부에는 completed와 failed 두 개의 메서드가 있으며 지금까지 우리가 구현한 방식과 동일하다.

```java
public interface AsynchronousByteChannel extends AsynchronousChannel {
    // 생략...
    <A> void read(ByteBuffer dst,
                  A attachment,
                  CompletionHandler<Integer,? super A> handler);
    // 생략...
    Future<Integer> read(ByteBuffer dst);
}

public interface CompletionHandler<V,A> {
    void completed(V result, A attachment);
    void failed(Throwable exc, A attachment);
}
```

AsynchronousByteChannel의 메서드 중 반환타입이 Future<Integer>인 메서드도 있다.
우리가 지금까지 살펴본 것과 같이 Future는 바로 반환이 되지만 `get()` 메서드를 호출하는 순간 스레드는 블로킹된다.

지금까지 우리가 만들었던 로직을 살펴보면 비동기를 위한 로직과 서비스를 위한 로직이 하나의 소스 파일에 위치하며 스프링이 지향하는 AOP와는 거리가 멀게 코드를 작성하게 되었다.
다음 장에서는 이러한 문제를 스프링의 옛 기술(작성일 기준 대략 15년전)을 사용하여 해결해본다.

---

**참고한 강의:**

- https://www.youtube.com/watch?v=aSTuQiPB4Ns&ab_channel=TobyLee