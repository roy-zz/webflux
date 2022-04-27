#### 부하 테스트

지금까지 계속 비동기 서블릿 방식으로 웹 개발을 해야 한정된 자원을 활용하여 더 많은 요청을 처리할 수 있다고 학습하였다.
정말로 우리가 학습한 내용이 맞는지 부하 테스트를 진행하여 결과를 확인해본다.
결과를 확인하기에 앞서 이번 단계에서는 스레드 상태를 직접 눈으로 확인하기 위해 [VisualVM(링크)](https://visualvm.github.io/) 이라는 툴을 사용할 것이다.

**StressTest**

부하 테스트를 위하여 StressTest라는 클래스를 작성하였고 HTTP Request를 발생시키는 클라이언트 역할을 한다.
이번에는 한 번에 100개의 요청을 발생시켜 컨트롤러가 한 번에 100개의 요청을 처리하게 하는 역할을 한다.

```java
@Slf4j
@EnableAsync
public class CallableLoadTest {
    private static final AtomicInteger sequence = new AtomicInteger(0);
    public static void main(String[] args) throws InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(1000);
        RestTemplate rt = new RestTemplate();
        String url = "http://localhost:8080/sync";
        // String url = "http://localhost:8080/callable-async";
        StopWatch main = new StopWatch();
        main.start();
        for (int i = 0; i < 100; i++) {
            es.execute(() -> {
                int index = sequence.addAndGet(1);
                log.info("Thread {}", index);
                StopWatch sw = new StopWatch();
                sw.start();
                rt.getForObject(url, String.class);
                sw.stop();
                log.info("Elapsed: {}, {}", index, sw.getTotalTimeSeconds());
            });
        }
        es.shutdown();
        es.awaitTermination(100, TimeUnit.SECONDS);
        main.stop();
        log.info("Total: {}", main.getTotalTimeSeconds());
    }
}
```

**AsyncSpringController**

동기 방식으로 처리되는 `/sync` API와 비동기 Callable 방식으로 처리되는 `/callable-async`를 가지고 있다.

```java
@RestController
public static class AsyncSpringController {
    @GetMapping("/sync")
    public String sync() throws InterruptedException {
        log.info("Call sync");
        TimeUnit.SECONDS.sleep(2);
        return "HELLO";
    }

    @GetMapping("/callable-async")
    public Callable<String> callableAsync() throws InterruptedException {
        log.info("Call async async");
        return () -> {
            log.info("Call async method");
            TimeUnit.SECONDS.sleep(2);
            return "HELLO";
        };
    }
}
```