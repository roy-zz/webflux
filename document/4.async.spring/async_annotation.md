### Spring WebFlux Series - 7

[이전 장(링크)](https://imprint.tistory.com/229) 에서는 자바와 비동기 기술 중 기본이 되는 `Future`에 대해서 알아보았다.
이번 장에서는 스프링의 비동기 기술 중에서 가장 많이 사용되는 @Async 애노테이션에 대해서 알아본다.
모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/webflux)의 테스트 코드에 있으므로 필요하다면 참고하도록 한다.

---

### 테스트 코드 구성

@SpringBootApplication 애노테이션을 사용하여 테스트를 진행할 때 스프링 부트를 통해 테스트를 진행하도록 구현하였고 비동기 작업이 가능하도록 @EnableAsync 애노테이션을 사용하였따.
ApplicationRunner을 스프링 빈으로 등록하여 프로젝트가 실행되면서 `run()` 메서드가 호출되도록 구현하였다.

우리가 작성하는 테스트 코드는 `run()` 메서드 내에서 스프링 부트 환경에서 실행되며 이후의 코드에는 아래의 코드는 생략하고 설명한다.

```java
@Slf4j
@EnableAsync
@SpringBootApplication
public class AsyncJavaSpringTest2 {
    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = SpringApplication.run(AsyncJavaSpringTest2.class, args)) {
        }
    }

    @Bean
    ApplicationRunner run() {
        return args -> {
            // 테스트 메서드 호출부
        };
    }
    // 이하 생략...
}
```

---

### Sync 메서드

스프링의 비동기 프로그래밍을 알아보기 전에 비동기를 적용하지 않고 동기로 작동하는 코드를 먼저 살펴본다.

```java
@Slf4j
@EnableAsync
@SpringBootApplication
public class AsyncJavaSpringTest2 {
    @Autowired
    private MySyncService mySyncService;
    @Service
    public static class MySyncService {
        public String hello() throws InterruptedException {
            log.info("Call hello()");
            TimeUnit.SECONDS.sleep(2);
            return "HELLO";
        }
    }
    private void mySyncServiceTest() throws InterruptedException {
        log.info("Call run()");
        String result = mySyncService.hello();
        log.info("EXIT: {}", result);
    }
}
```

출력 결과는 우리가 예상한 것과 같이 `Call run()`, `Call hello()`가 먼저 출력되었고 대략 2초 후에 `EXIT: HELLO`가 출력되었다.

```bash
2022-04-26 11:56:55.952: Call run()
2022-04-26 11:56:55.952: Call hello()
2022-04-26 11:56:57.954: EXIT: HELLO
```

---

### @Async & Future

MyAsyncService는 mySyncService와 다르게 hello 메서드에 @Async 애노테이션이 붙어있다.
스프링에서는 이렇게 @Async 애노테이션을 사용하여 간단하게 비동기 메서드를 만들 수 있다.
하지만 비동기라는 특성상 먼 미래에 발생할 결과 값을 받아올 수 없으므로 반환 타입을 `Future`로 변경해야한다.
결과를 반환할 때는 꼭 Future로 반환할 필요는 없고 `new AsyncResult` 객체를 생성하여 생성자에 결과를 담아서 반환해도 된다.

현업에서 우리가 예제로 작성한 코드와 같이 사용하는 경우는 거의 없으며 일반적으로 사용되는 경우는 `@Async`로 작동하는 코드가 상당히 긴 시간동안 처리해야하는 배치작업을 수행하는 경우에 사용된다.
이러한 경우 Future를 사용하여 결과를 기다리는 것은 사실상 불가능 하며 반환타입을 void로 바꾸고 비동기로 작동하는 스레드는 작업이 끝나면 DB와 같은 저장소에 완료되었다는 플래그를 저장하고 작업 완료 여부가 궁금한 쪽에서 저장소의 플래그를 확인해서 작업 결과를 확인하는 방법이 있다.
또 다른 방법은 `Future`라는 핸들러를 Http 세션에 저장하고 반환한 뒤 만약 클라이언트가 작업 결과가 궁금하다면 다시 요청하여 세션에 있는 `Future`의 `isDone()`을 호출하여 성공적으로 완료되었는지 확인하는 방법이 있다.

```java
@Slf4j
@EnableAsync
@SpringBootApplication
public class AsyncJavaSpringTest2 {
    @Autowired
    private MyAsyncService myAsyncService;
    @Service
    public static class MyAsyncService {
        @Async
        public Future<String> hello() throws InterruptedException {
            log.info("Call hello()");
            TimeUnit.SECONDS.sleep(2);
            return new AsyncResult<>( "HELLO");
        }
    }
    private void myAsyncServiceTest() throws InterruptedException, ExecutionException {
        log.info("Call run()");
        Future<String> result = myAsyncService.hello();
        log.info("EXIT: {}", result.isDone());
        log.info("Result: {}", result.get());
    }
}
```

위의 코드의 출력 결과는 아래와 같다.

`Call run()`, `EXIT: false`, `Call hello()`는 애플리케이션 실행 시점에 출력되었다.
`Future.get()`의 결과를 기다리는 `Result: {}`만 2초 후에 출력되었다.

```bash
2022-04-26 14:21:36.980 : Call run()
2022-04-26 14:21:36.982 : More than one TaskExecutor bean found within the context, and none is named 'taskExecutor'. Mark one of them as primary or name it 'taskExecutor' (possibly as an alias) in order to use it for async processing: [springExecutor, coreExecutor]
2022-04-26 14:21:36.983 : EXIT: false
2022-04-26 14:21:36.986 : Call hello()
2022-04-26 14:21:38.992 : Result: HELLO
```

---

### ListenableFuture

스프링의 @Async 애노테이션은 자바5의 Concurrent 패키지가 등장하기 전인 자바4 버전에서 이미 등장했으며 스프링의 오래된 기술 중 하나다.
스프링에는 이전 장에서 우리가 만들었던 CallbackFutureHandleExceptionTask와 같이 @Async의 결과를 Callback 형식으로 받기 위해 ListenableFuture를 사용한다.
ListenableFuture은 자바 표준 문법은 아니며 스프링4.0 이상 버전에서 지원하는 문법이다.
이러한 콜백의 장점은 메인 스레드(혹은 콜백 스레드를 실행시킨 스레드)가 결과를 기다리며 블로킹되어 있는 것이 아니라 자신이 해야하는 일을 할 수 있다는 점이다.
필자가 참고한 강의와 다르게 필자가 작성한 코드의 스레드는 메인 스레드 종료시점에 interrupted가 발생하여 종료된다. 아직 원인은 찾지 못하였다.~~(버전 올라가면서 데몬 스레드로 변경된건가...???)~~
다른 방법으로는 자바8에 추가된 CompletableFuture를 사용하는 방법이 있으며 추후에 다시 살펴보도록 한다.

```java
@Slf4j
@EnableAsync
@SpringBootApplication
public class AsyncJavaSpringTest2 {
    @Autowired
    private MyListenableAsyncService myListenableAsyncService;
    @Service
    public static class MyListenableAsyncService {
        @Async
        public ListenableFuture<String> hello() throws InterruptedException {
            log.info("Call hello()");
            TimeUnit.SECONDS.sleep(2);
            return new AsyncResult<>("HELLO");
        }
    }
    private void myListenableAsyncServiceTest() throws InterruptedException {
        ListenableFuture<String> listenableFuture = myListenableAsyncService.hello();
        listenableFuture.addCallback(
                s -> log.info("Success: {}", s),
                f -> log.info("Failure: {}", f.getMessage()));
        log.info("EXIT");
    }
}
```

출력 결과는 우리의 예상과 동일하게 아래와 같이 출력되었다.

```bash
2022-04-26 14:31:07.883 [           main]: EXIT
2022-04-26 14:31:07.886 [cTaskExecutor-1]: Call hello()
2022-04-26 14:31:09.888 [cTaskExecutor-1]: Success: HELLO
```

---

### 번외

@Async도 Executor와 같이 섬세한 조작이 가능하다.
자바의 모든 스레드 풀은 Executor로 끝나는 인터페이스로 구현되어 있다.
스프링에서 기본 @Async에 사용되는 SimpleAsyncTaskExecutor는 우리의 예상과 다르게 사용할 수 없을 사양을 가지고 있다.
이유는 생성된 스레드를 캐싱하거나 스레드 풀을 만들어서 풀링하는 방식이 아니라 필요한 시점에 계속 생성하는 전략을 가지고 있다.
실습용, 교육용 수준에서만 사용해야하며 현업에서는 절대 사용해서는 안되는 전략이며 직접 스레드 풀을 생성해서 사용해야한다.

기본적으로 ThreadPoolTaskExecutor를 사용하며 자바에서 사용되는 대부분의 스레드 풀 특성을 담을 수 있다.
setCorePoolSize를 사용하여 스레드 풀의 최초 사이즈를 지정할 수 있다.
이때 풀에 담기는 스레드는 애플리케이션 실행 시점에 생성되는 것이 아니라 풀에 처음으로 요청이 들어올 때 생성된다.
setMaxPoolSize로 최대 풀사이즈를 지정할 수 있지만 이것은 우리가 생각하는 DB 풀의 최대값과 비슷하지만 같은 개념은 아니다.
모든 풀에서 스레드가 전부 작업중인 경우 스레드의 반납을 기다리는 Queue의 Capacity를 설정하는 setQueueCapacity와 같은 메서드가 있다.

우리가 생각하는 Max Pool 사이즈와 무엇이 다른지 잠깐 살펴본다.
예를 들어 setCorePoolSize(10), setMaxPoolSize(20), setQueueCapacity(50)과 같이 설정하였을 때 우리가 예상하는 순서는 아래와 같을 것이다.
최초에 스레드를 10개까지 생성하고 그 이상의 요청이 들어오면 최대 20개까지 늘리며 20개도 모두 소진하면 Queue에 최대 50개까지 저장한다. 라고 예상하지만
실제 작동은 최초에 스레드를 10개까지 생성하고 Queue에 최대 50개까지 저장하며 Queue가 가득차면 스레드를 20개까지 늘린다.
결국 Queue가 가득차면 발생할 에러를 스레드를 더 생성하여 막아주는 것이 setMaxPoolSize 옵션이다.

추가로 setKeepAliveSeconds는 MaxPoolSize만큼 추가로 생성된 스레드가 일정 기간동안 재할당되지 않으면 제거할 시간을 지정한다.
setTaskDecorator는 스레드를 만들거나 반환하는 시점에 콜백을 걸어줄 수 있다. 스레드 생성 및 반납 앞과 위에 로그를 걸어두면 스레드가 얼마나 생성되고 반납되는지 통계를 만들 때 유용하게 사용된다.
setThreadNamePrefix는 스레드 이름 앞에 원하는 문구를 넣을 수 있으며 디버깅 시에 유용하게 사용된다.
스레드 풀의 사이즈와 최대값, 큐의 크기들은 항상 많은 고민과 모니터링의 결과에 의해 조정되어야 한다.

아래와 같이 ThreadPoolTaskExecutor를 빈으로 등록하고 같은 예제를 실행시키고 다시 이전의 코드를 실행시켜보면 결과는 아래와 같이 스레드의 이름이 변경된다.
스프링은 기본적으로 SimpleAsyncTaskExecutor를 사용하며 ExecutorService, Executor, ThreadPoolTaskExecutor 중 하나라도 구현한 빈이 있으면 기본으로 사용된다.
만약 스레드 풀을 여러 개로 분리하여 필요에 따라 다른 스레드 풀에서 스레드를 받아서 사용해야 한다면 @Asnyc("coreExecutor")과 같이 직접 스레드를 지정하여 사용할 수 있다.


```java
@Bean
ThreadPoolTaskExecutor coreExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(10);
    taskExecutor.setMaxPoolSize(20);
    taskExecutor.setQueueCapacity(50);
    taskExecutor.setThreadNamePrefix("Core-Thread-");
    return taskExecutor;
}
```

지금까지 가장 기본적으로 자바와 스프링을 이용한 비동기 방식에 대해서 알아보았다.
이제부터 웹 애플리케이션을 만들어보면서 비동기 기술에 대해 심화학습을 해보도록 한다.

---

**참고한 강의:**

- https://www.youtube.com/watch?v=aSTuQiPB4Ns&ab_channel=TobyLee