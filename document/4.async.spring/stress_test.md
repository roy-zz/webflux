### Spring WebFlux Series - 9

지금까지 우리는 [자바의 Future(링크)](https://imprint.tistory.com/229) 와 [스프링의 Async(링크)](https://imprint.tistory.com/230) 를 학습하면서 자바와 스프링의 비동기 기술에 대해서 알아보았다.
이번 장에서는 동기 방식과 비동기 방식으로 작성되어 있는 컨트롤러 코드를 부하 테스트를 통하여 성능을 확인하는 시간을 가져본다.
모든 코드는 [깃 허브 (링크)](https://github.com/roy-zz/webflux)의 테스트 코드에 있으므로 필요하다면 참고하도록 한다.

---

### 부하 테스트 설정

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

**application.yml**

한정된 자원을 맞추기 위하여 application.yml을 수정해가면서 톰캣의 스레드 수를 조절한다.

```yaml
server:
  tomcat:
    threads:
      max: 100
```

---

### 부하 테스트 

#### Case - 1 (Tomcat Thread 100, Request 100)

톰캣의 스레드를 100개로 지정하고 요청을 100개로 설정하고 테스트를 진행한다.
필자의 PC에서 VisualVM이 실행되지 않는 오류가 발생하여 Thread

1. 동기 컨트롤러












---

**참고한 강의:**

- https://www.youtube.com/watch?v=aSTuQiPB4Ns&ab_channel=TobyLee