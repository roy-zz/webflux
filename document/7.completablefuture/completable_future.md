### Spring WebFlux Series - 14

[이전 장(링크)](https://imprint.tistory.com/244) 에서는 `Callback 지옥`의 코드를 리펙토링하는 방법에 대해서 알아보았다.
이번 장에서는 `CompletableFuture`에 대해서 알아본다.
모든 코드는 [깃 허브 (링크)](https://github.com/roy-zz/webflux)의 테스트 코드에 있으므로 필요하다면 참고하도록 한다.

---

### 개요

오늘은 스프링 5.0의 `Reactive Streams`의 바로 직전 단계이며 자바 8에 추가된 `CompletableFuture`에 대해서 알아본다.
우리는 지금까지 자바와 스프링의 비동기 기술에 대해서 오래된 기술부터 나름 최신의 기술까지 알아보았다.
이번 장에서는 마지막으로 `CompletableFuture`를 알아보면서 자바와 스프링의 비동기 기술에 대한 정리를 마무리한다.
이번 장을 이해하기 위해서는 이전에 알아보았던 `RestTemplate`와 `Callback Hell`에 대해서 알아야한다.
이 부분을 확인하지 않았다면 글의 하단부에 링크를 첨부하였으므로 확인하고 다시 돌아오도록 한다. 

서블릿과 스프링의 비동기 웹 기술은 클라이언트의 커넥션 하나당 스레드가 하나씩 할당되면서 요청이 많아지면 서버의 자원을 소모하면서 스레드가 부족해져서 성능이 급격하게 낮아지는 `Thread Hell`이라는 현상을 막기위해서 출시되었다.
비동기 웹 기술을 사용하면 한정된 리소스를 효율적으로 사용할 수 있게 되며 우리가 작성하는 코드는 한결 간결해지게 된다.

우리는 지금까지 알아본 `Future`는 비동기 작업의 결과를 담고 있는 객체로 볼 수 있다. 
현재와 수행하던 스레드와 별개로 백그라운드에서 작업을 수행하고 결과를 반환한다.

`Future`, `Promise`, `Deferred`는 많은 언어에서 같은 의미이지만 다른 표현으로 많이 사용되고 있다.

---

### Completable Future 기초 사용법

`CompletableFuture`를 사용하면 이전의 `ListenableFuture`와 `FutureTask`랑은 다르게 간단하게 비동기 결과를 만들어 낼 수 있다.
간단하게 `CompletableFuture`의 사용법을 알아본다.

```java
public class CompletableFutureApplication {
    private void completableFutureTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> cf = CompletableFuture.completedFuture(1);
        System.out.println("cf.get() = " + cf.get());
    }
}
```
`completedFuture`라는 스태틱 메서드를 사용하면 당연한 얘기겠지만 완성된 결과인 `1`을 가져다 쓸 수 있다.
만약 아래와 같이 `new`를 사용하여 `CompletableFuture` 객체를 생성하고 `get` 메서드로 결과를 가져오려 한다면 완성된 결과를 `CompletableFuture`에 대입시켜주는 코드가 없기 때문에 애플리케이션은 무한 대기 상태에 빠지게 된다.

```java
public class CompletableFutureApplication {
    private void completableFutureTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> cf = new CompletableFuture<>();
        System.out.println("cf.get() = " + cf.get());
    }
}
```

다른 스레드에서 `CompletableFuture` 객체에 완성된 결과를 넣어주고 싶다면(물론 아래의 코드는 메인 스레드에서 처리하는 방식이지만) 아래와 같이 `complete` 메서드를 호출해주면 된다.

```java
public class CompletableFutureApplication {
    private void completableFutureTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.complete(2);
        System.out.println("cf.get() = " + cf.get());
    }
}
```

만약 예외가 발생하였다면 `completeExceptionally` 메서드를 사용하여 완셩된 결과 대신 발생한 예외를 전달할 수 있다.

```java
public class CompletableFutureApplication {
    private void completableFutureTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.completeExceptionally(new RuntimeException());
    }
}
```

하지만 위의 코드를 실행해보면 예외가 발생하는 로그는 출력되지 않는다.
이유는 `CompletableFuture` 객체에 예외가 담겨있지만 예외를 사용하는 코드는 없기 때문이며 `get` 메서드를 추가하여 결과를 가져오려하면 예외가 발생한다.
우리가 `CompletableFuture`의 기초 문법에서 기억해야하는 부분은 결과를 넣어주는 코드에서 `complete` 메서드를 호출하여 결과를 넣어주거나 `completeExceptionally` 메서드를 호출하여 예외를 넣어주는 부분이다.

---

### CompletableFuture와 스레드

이번에는 `CompletableFuture`에 백그라운드에서 돌아가는 새로운 `스레드`를 생성하고 해당 스레드에서 어떠한 작업을 수행하게 코드를 작성해볼 것이다.
아래의 코드와 같이 `CompletableFuture`의 `runAsync` 스태틱 메서드와 람다 표현식을 사용하여 `runAsnyc` 라는 문자를 출력하는 코드를 작성해본다.

```java
public class CompletableFutureApplication {
    private void completableFutureWithThread() throws InterruptedException {
        CompletableFuture.runAsync(() -> log.info("runAsync"));
        log.info("exit");
    }
}
```

출력된 결과는 우리의 예상과 같이 `exit`는 메인 스레드에 의해 출력되었고 `runAsync`는 `ForkJoinPool.commonPool-worker-19`라는 스레드에 의해 출력되었다.
우리가 Pool관련 설정을 따로 하지 않으면 자바 7 부터는 `ForkJoinPool`의 `commonPool`을 사용하게 되고 그 중 하나의 스레드에 의해서 `runAsync`가 수행된 것을 확인할 수 있다.

```bash
[main]                              - exit
[ForkJoinPool.commonPool-worker-19] - runAsync
```

`CompletableFuture`의 큰 특징을 살펴보기 위해 구현 코드를 확인해본다.
`Future`라는 자바 비동기의 기본 인터페이스 뿐만 아니라 자바 8에 추가된 `CompletionStage`라는 인터페이스를 구현하고 있다.
`CompletionStage`는 중요한 개념들이 많이 추가되어 있는데 큰 특징은 하나의 비동기 작업을 수행하고 완료되었을 때 완료된 결과에 의존적으로 다른 작업을 수행할 수 있도록 하는 기능들을 가지고 있다는 점이다.

```java
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {
    // 생략...
}
```

이러한 특징때문에 우리는 `runAsync`를 수행하고 추가로 다른 작업을 수행할 수 있도록 코드를 아래와 같이 수정할 수 있다.
아래의 코드가 의미하는 바는 `runAsync`메서드에 의해서 비동기 작업을 수행하고 그 결과에 따라서 `thenRun`작업을 수행하라는 의미이다.

```java
public class CompletableFutureApplication {
    private static void completableFutureWithThenRunAsync() {
        CompletableFuture
                .runAsync(() -> log.info("runAsync"))
                .thenRun(() -> log.info("thenRun"))
                .thenRun(() -> log.info("thenRun"));
        log.info("exit");
    }
}
```

출력된 결과는 아래와 같다.

```bash
[ForkJoinPool.commonPool-worker-19] - runAsync
[main] - exit
[ForkJoinPool.commonPool-worker-19] - thenRun
[ForkJoinPool.commonPool-worker-19] - thenRun
```

이번에는 `supplyAsync`를 사용해본다. 이름에서 알 수 있듯이 파라미터로 전달되는 타입이 `supplier`가 되어야 한다.
`supplier`는 파라미터는 없으며 반환값이 있어야한다. `supplyAsnyc`에서 생성된 값은 이후 `thenApply` 메서드에서 소모 할 수 있다. 
`thenApply`의 경우 파라미터와 반환값이 모두 존재하는 메서드다. 마지막으로는 `thenAccept` 메서드를 사용하였고 해당 메서드는 파라미터는 있고 반환값은 존재하지 않는다.

```java
public class CompletableFutureApplication {
    private static void completableFutureWithSupply() throws ExecutionException, InterruptedException {
        CompletableFuture
                .supplyAsync(() -> {
                    log.info("runAsync");
                    return 1;
                })
                .thenApply(s -> {
                    log.info("thenApply {}", s);
                    return s + 1;
                })
                .thenAccept(s -> log.info("thenAccept {}", s));
        log.info("exit");
        ForkJoinPool.commonPool().shutdown();
        ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
    }
}
```

출력된 결과는 아래와 같다.

```bash
[main] - exit
[ForkJoinPool.commonPool-worker-19] - runAsync
[ForkJoinPool.commonPool-worker-19] - thenApply 1
[ForkJoinPool.commonPool-worker-19] - thenAccept 2
```

---

#### CompletableFuture thenCompose

여기서 우리가 주목해야하는 부분은 `supplyAsync`, `thenApply` 모두 반환 값이 비동기 작업의 결과와 상태를 가지고 있는 `CompletableFuture`라는 것이다.

간혹 `supplyAsnyc`와 `thenApply`의 반환 값이 일반 자료형이 아닌 `CompletableFuture.completedFuture`일 수 있다.
`CompletableFuture`일 수 있다. `thenApply`의 반환 타입은 `CompletableFuture`이며 그 안에 `CompletableFuture`가 들어가는 구조가 된다.
이러한 경우 `thenApply`가 아니라 아래와 같이 `thenCompose`를 사용해주어야 한다.
쉽게 설명하면 `thenCompose`는 자바 Stream의 `flatmap`과 유사하면 `thenApply`는 Stream의 `map`과 유사하다.

```java
public class CompletableFutureApplication {
    private static void completableFutureWithThenCompose() throws InterruptedException {
        CompletableFuture
                .supplyAsync(() -> {
                    log.info("runAsync");
                    return 1;
                })
                .thenCompose(s -> {
                    log.info("thenCompose {}", s);
                    return CompletableFuture.completedFuture(s + 1);
                })
                .thenAccept(s -> log.info("thenAccept {}", s));
        log.info("exit");
        ForkJoinPool.commonPool().shutdown();
        ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
    }
}
```

출력 결과는 아래와 같다.

```bash
[ForkJoinPool.commonPool-worker-19] - runAsync
[main] - exit
[ForkJoinPool.commonPool-worker-19] - thenCompose 1
[ForkJoinPool.commonPool-worker-19] - thenAccept 2
```

--- 

#### CompletableFuture exceptionally

우리가 지금까지 작성한 코드의 모든 비동기 작업은 이론적으로 어디에서든 `RuntimeException`이 발생할 수 있다.
예외가 발생한 경우에는 모든 로직이 진행되어서는 안된다.
예외가 일어나는 경우 처리하는 방법은 크게 두 가지가 있는데 예외가 일어난 경우 요청의 마무리까지 `throw`하는 방법과 예외가 일어나면 적당한 값으로 다음 단계로 넘기는 방식이 있다.

비동기 작업 중 하나라도 문제가 발생하는 경우 입력받은 값에 기존의 로직이 아닌 `-10` 연산을 진행하여 다음 비동기 작업에게 전달하도록 구현해본다.

```java
public class CompletableFutureApplication {
    private static void completableFutureWithExceptionally() throws InterruptedException {
        CompletableFuture
                .supplyAsync(() -> {
                    log.info("runAsync");
                    if (true) throw new RuntimeException();
                    return 1;
                })
                .thenCompose(s -> {
                    log.info("thenCompose {}", s);
                    return CompletableFuture.completedFuture(s + 1);
                })
                .thenApply(s -> {
                    log.info("thenApply {}", s);
                    return s + 2;
                })
                .exceptionally(e -> -10)
                .thenAccept(s -> log.info("thenAccept {}", s));
        log.info("exit");
        ForkJoinPool.commonPool().shutdown();
        ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
    }
}
```

출력된 결과를 보면 아래와 같다.
`supplyAsnyc`에서 예외가 발생하였고 `thenCompose`, `thenApply`를 지나 예외가 `exceptionally`로 전달되었고 값이 `-10`으로 변경되어 `thenAccept`에 의해 소비된 것을 확인할 수 있다.
예외가 어디서 발생하든 `exceptionally`에게 전달되기 때문에 모든 비동기 작업에 따로 예외 상황에 대한 코드를 작성할 필요가 없어진다.

```bash
[ForkJoinPool.commonPool-worker-19] - runAsync
[main] - exit
[ForkJoinPool.commonPool-worker-19] - thenAccept -10
```

---

#### CompletableFuture MultiThread

지금까지 구현한 `supplyAsnyc`, `thenCompose`, `thenApply`와 같은 작업들 중 특정 작업만 어떠한 이유로 다른 스레드에서 작동시킨다고 가정해본다.
우리가 구현한 코드에서 `thenApply`를 예로 들면 `thenApply`뒤에 `Async`를 붙여서 `thenApplyAsync`를 사용하면 된다.
이렇게 사용하는 경우 우리가 정해놓은 설정값에 따라서 새로운 스레드를 사용하거나 기존의 스레드를 재사용하여 다른 스레드에서 작업하게 구현할 수 있다.

`Executors.newFilxedThreadPool`을 사용하여 우리가 정한 스레드에서 작업이 진행되도록 수행해본다.
`newFixedThreadPool`을 통해여 사이즈가 10인 스레드 풀을 생성하였다.
`thenCompose`를 제외한 `supplyAsync`, `thenApplyAsync`, `thenAcceptAsync` 메서드를 사용할 때 람다 식과 함께 사용할 스레드 풀을 파라미터로 전달하였다.

```java
public class CompletableFutureApplication {
    private static void completableFutureWithMultiThread() throws InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(10);
        CompletableFuture
                .supplyAsync(() -> {
                    log.info("runAsync");
                    return 1;
                }, es)
                .thenCompose(s -> {
                    log.info("thenCompose {}", s);
                    return CompletableFuture.completedFuture(s + 1);
                })
                .thenApplyAsync(s -> {
                    log.info("thenApply {}", s);
                    return s + 2;
                }, es)
                .exceptionally(e -> -10)
                .thenAcceptAsync(s -> log.info("thenAccept {}", s), es);
        log.info("exit");
        ForkJoinPool.commonPool().shutdown();
        ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
    }
}
```

출력되는 결과는 아래와 같다.

```bash
[pool-1-thread-1] - runAsync
[main] - exit
[pool-1-thread-1] - thenCompose 1
[pool-1-thread-2] - thenApply 2
[pool-1-thread-3] - thenAccept 4
```

출력 결과를 보면 `supplyAsync`에서 새로운 스레드인 `pool-1-thread-1`를 할당받아서 사용하였다.
`thenCompose`의 경우 다른 스레드를 사용을 유도하는 코드가 없으므로 동일한 스레드인 `pool-1-thread-1`을 사용하여 작업을 처리하였다.
`.thenApplyAsync`, `.thenAcceptAsync` 또한 새로운 스레드를 사용하기 위한 메서드이며 파라미터로 스레드 풀을 전달받아 각각 새로운 스레드인 `pool-1-thread-2`와 `pool-1-thread-3`에 의해 처리된 것을 확인할 수 있다.
메인 스레드를 제외한 스레드의 흐름을 보면 아래와 같다.

`pool-1-thread-1` -> `pool-1-thread-1` -> `pool-1-thread-2` -> `pool-1-thread-3`

이러한 흐름은 우리가 `fixedThreadPool`을 사용하였기 때문에 지속적으로 스레드가 생성된 결과이다.
만약 반납된 스레드를 다시 사용하고 싶다면 `cachedThreadPool`를 사용하면 된다.

자바9의 경우 자바8에 이어 `CompletableFuture`를 개선하였으며 비동기 관련 기능에 대해 적극적으로 개발을 진행하고 있다.

---

### Refactoring (Callback Hell)

우리가 이전 장에서 작성한 `Callback 지옥`코드를 `CompletableFuture`를 사용하여 해결해본다.

```java
public class CallbackHellService {
    @Autowired private MyLogic myLogic;
    private final static String URL_1 = "http://localhost:8081/remote-service-1/{request}";
    private final static String URL_2 = "http://localhost:8081/remote-service-2/{request}";
    AsyncRestTemplate rt = new AsyncRestTemplate(
            new Netty4ClientHttpRequestFactory(
                    new NioEventLoopGroup(1)));
    
    @GetMapping("/callback-hell/rest/{idx}")
    public DeferredResult<String> callbackHellRest(@PathVariable int idx) {
        DeferredResult<String> dr = new DeferredResult<>();
        ListenableFuture<ResponseEntity<String>> future1 = rt.getForEntity(URL_1, String.class, "h" + idx);
        future1.addCallback(
                success1 -> {
                        ListenableFuture<ResponseEntity<String>> future2 = rt.getForEntity(URL_2, String.class, Objects.requireNonNull(success1).getBody());
                        future2.addCallback(
                                success2 -> {
                                    ListenableFuture<String> future3 = myLogic.work(Objects.requireNonNull(success2).getBody());
                                    future3.addCallback(
                                            success3 -> {
                                                    dr.setResult(success3);
                                                },
                                            failure3 -> {
                                                dr.setErrorResult(failure3.getMessage());
                                            });
                                    },
                                failure2 -> {
                                    dr.setErrorResult(failure2.getMessage());
                                });
                    },
                failure1 -> {
                    dr.setErrorResult(failure1.getMessage());
                });
        return dr;
    }
}
```

코드를 변경할 때 주의해야 하는 부분은 `AsyncRestTemplate.getForEntity` 메서드의 반환 타입이 `ListenableFuture`라는 점이다.
우리는 `ListenableFuture`가 아닌 `CompletableFuture`가 필요하기 때문에 기존의 반환 타입을 우리가 원하는 타입으로 변경시켜주어야 한다.
간단하게 아래와 같은 메서드를 만들어서 `ListenableFuture`의 Callback 메서드로 `CompletableFuture`의 메서드를 등록시키면 호환이 가능해진다.

이제 기존의 `Callback 지옥` 코드를 수정해본다.
여기서 두 개의 `CompletableFuture`를 연결할 때 `thenApply`가 아니라 `thenCompose`가 되는 이유는 `map`과 같이 이전 `CompletableFuture`의 값을 가지고 새로운 값을 계산하는 것이 아니라 새로운 결과 값 또한 `CompletableFuture`이기 때문이다.
`CompletableFuture`에서 예외를 처리하기 위해서 우리는 `exceptionally`를 사용하였다.
하지만 `DeferredResult`의 경우 예외를 처리하기 위해서 `setResult` 대신 `setErrorResult`를 사용해야하는 차이가 있다.
이러한 경우에는 `thenAccept`이후에 `exceptionally`를 사용하여 처리를 해야한다.
이렇게 코드를 구현하는 경우 문제가 없다면 `thenAccept`에서 로직이 마무리되고 문제가 발생하는 경우 `exceptionally`까지 예외가 전달된다.
`exceptionally`의 경우 반드시 반환타입이 있어야하기 때문에 무의미한 `return (Void) null;`이라는 코드를 추가하였고 문법을 지킨다는 것 이상의 의미는 없다.

```java
public class CallbackHellService {
    @GetMapping("/callback-hell/refactoring/v1/{idx}")
    public DeferredResult<String> callbackHellRefactoring(@PathVariable int idx) {
        DeferredResult<String> dr = new DeferredResult<>();
        toCompletableFuture(rt.getForEntity(URL_1, String.class, "hell_" + idx))
                .thenCompose(s1 -> toCompletableFuture(rt.getForEntity(URL_2, String.class, s1.getBody())))
                .thenCompose(s2 -> toCompletableFuture(myLogic.work(s2.getBody())))
                .thenAccept(dr::setResult)
                .exceptionally(ex -> {
                    dr.setErrorResult(ex.getMessage());
                    return (Void) null;
                });
        return dr;
    }
    private <T> CompletableFuture<T> toCompletableFuture(ListenableFuture<T> lf) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        lf.addCallback(cf::complete, cf::completeExceptionally);
        return cf;
    }
}

@Service
public class MyLogic {
    @Async
    public ListenableFuture<String> work(String request) {
        return new AsyncResult<>(String.format("asyncwork/%s", request));
    }
}
```

여기서 한가지 더 해결해야하는 문제가 있다.
우리는 `MyLogic` 클래스의 `work`메서드가 비동기로 작동하기를 바라면서 `@Async` 애노테이션을 사용하였다. 
필요한 시점이 아닌 모든 코드에서 비동기로 작동하도록 `@Async` 애노테이션을 사용하게 되면 굳이 비동기로 작동할 필요가 없는 코드에서도 비동기로 작동하게 된다.
`@Async` 애노테이션을 사용하지 않는 방식으로 코드를 수정하여 필요한 시점에만 비동기로 작동하도록 코드를 수정해본다.

**MyLogic**
```java
@Service
public class MyLogic {
    public String work(String request) {
        return String.format("asyncwork/%s", request);
    }
}
```

`work` 메서드를 호출하는 쪽에서는 `thenCompose`가 아닌 `thenApplyAsync`를 사용하면 된다.

```java
public class CallbackHellService {
    @GetMapping("/callback-hell/refactoring/v1/{idx}")
    public DeferredResult<String> callbackHellRefactoring(@PathVariable int idx) {
        DeferredResult<String> dr = new DeferredResult<>();
        toCompletableFuture(rt.getForEntity(URL_1, String.class, "hell_" + idx))
                .thenCompose(s1 -> toCompletableFuture(rt.getForEntity(URL_2, String.class, s1.getBody())))
                .thenApplyAsync(s2 -> myLogic.work(s2.getBody()))
                .thenAccept(dr::setResult)
                .exceptionally(ex -> {
                    dr.setErrorResult(ex.getMessage());
                    return (Void) null;
                });
        return dr;
    }
}
```

---

**참고한 강의**

- https://www.youtube.com/watch?v=Tb43EyWTSlQ&ab_channel=TobyLee

**첨부 링크**

- [Rest Template - Blocking](https://imprint.tistory.com/238)
- [Rest Template - Async](https://imprint.tistory.com/240)
- [Rest Template - 복합](https://imprint.tistory.com/241)
- [Callback Hell 해결](https://imprint.tistory.com/244)