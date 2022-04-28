우리는 [이전 장(링크)](https://imprint.tistory.com/238) 에서 링크드인의 발표자료를 보면서 서비스간의 통신으로 인한 블로킹 현상에 대해서 알아보았다.
이번 장 에서는 이러한 블록킹 문제를 해결하기 위한 방법에 대해서 알아본다.
모든 코드는 [깃 허브 (링크)](https://github.com/roy-zz/webflux)의 테스트 코드에 있으므로 필요하다면 참고하도록 한다.

---

### 원격지 서비스 추가

#### HTTP Client 코드 개선

`MvcV2Client` 코드는 `MvcClient` 코드에 `CyclicBarrier`를 추가하여 스레드 동기화를 구현해두었다.
이번에 추가되는 `MyClient`는 `MvcV2Client`의 요청 URL에 index값을 추가하도록 수정하고 리턴값을 출력하도록 수정하였다.

**MyClient**
```java
@Slf4j
@EnableAsync
public class MyClient {
    private static final AtomicInteger idx = new AtomicInteger(0);
    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        ExecutorService es = Executors.newFixedThreadPool(1000);
        RestTemplate rt = new RestTemplate();
        String url = "http://localhost:8080/my-service/rest/{idx}";
        CyclicBarrier barrier = new CyclicBarrier(101);
        for (int i = 0; i < 100; i++) {
            es.submit(() -> {
                int index = idx.addAndGet(1);
                barrier.await();
                log.info("Thread {}", index);
                StopWatch sw = new StopWatch();
                sw.start();
                String response = rt.getForObject(url, String.class, index);
                sw.stop();
                log.info("Elapsed: {} {} / {}", index, sw.getTotalTimeSeconds(), response);
                return null;
            });
        }
        barrier.await();
        StopWatch main = new StopWatch();
        main.start();
        es.shutdown();
        es.awaitTermination(100, TimeUnit.SECONDS);
        main.stop();
        log.info("Total: {}", main.getTotalTimeSeconds());
    }
}
```

---

#### MyService Controller 개선

기존의 컨트롤러 코드를 개선한 `/mvc/rest API`도 `MyService` 로 개선하고 Path Variable을 받을 수 있도록 개선하였다.

```java
@SpringBootApplication
public class MyService {
    private static final RestTemplate REST_TEMPLATE = new RestTemplate();
    @RestController
    public static class MyController {
        @GetMapping("/my-service/rest/{idx}")
        public String service(@PathVariable String idx) {
            return idx;
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(MyService.class, args);
    }
}
```

`MyClient`를 실행시켜서 부하테스트를 진행해본다.

![](improveblocking_image/default-print.png)

크게 변경된 것은 없지만 이제 요청값과 반환값이 매칭된다는 사실을 알 수 있다.

---

#### RemoteService(원격지 서버) 추가

원격지 서버 역할을 하는 `RemoteService` 라는 클래스 파일을 추가하였고 `MyService`와 같은 경로에 위치한다.

![](improveblocking_image/add-remote-server.png)

`RemoteService`의 코드를 살펴본다.
`MyService`와 유사하게 SpringBoot에 의해 기동된다. 문제는 하나의 프로젝트 안에 application.yml을 공유하는 두 개의 서비스가 존재한다는 점이다.
만약 우리가 따로 지정하지 않는다면 `MyService`도 8080 포트를 사용하려 할 것이며 `RemoteService`고 8080 포트를 사용하려 할 것이다.
이러한 문제를 해결하기 위해 `RemoteService`의 경우 메인 메서드에 따로 설정값을 지정하여 `MyService`와는 다른 설정값을 가지도록 구현하였다.

**RemoteService**
```java
@SpringBootApplication
public class RemoteService {
    @RestController
    public static class RemoteController {
        @GetMapping("/remote-service/{request}")
        public String service(@PathVariable String request) {
            return request;
        }
    }
    public static void main(String[] args) {
        System.setProperty("server.port", "8081");
        System.setProperty("server.tomcat.max-threads", "1000");
        SpringApplication.run(RemoteService.class, args);
    }
}
```

`MyService` 또한 바로 반환값을 응답하는 것이 아니라 원격지 서버인 `RemoteService`의 `/remote-service/{request}` API를 호출하고 반환받은 응답 값을 응답하도록 수정하였다.

**MyService**

```java
@SpringBootApplication
public class MyService {
    private static final RestTemplate REST_TEMPLATE = new RestTemplate();
    @RestController
    public static class MyController {
        @GetMapping("/my-service/rest/{idx}")
        public String service(@PathVariable String idx) {
            String response = REST_TEMPLATE.getForObject("http://localhost:8081/remote-service/{request}", String.class, idx);
            return String.format("my-response: %s, remote-response: %s", idx, response);
        }
    }
    public static void main(String[] args) {
        SpringApplication.run(MyService.class, args);
    }
}
```

정상적으로 작동하는지 `MyClient`를 실행시켜서 출력을 확인해본다.

![](improveblocking_image/add-remote-response.png)

우리가 원하는대로 원격지 서버의 응답까지 포함되었으며 `MyService`에서 바로 반환하는 것보다 시간이 조금 더 걸린 것을 확인할 수 있다.
우리의 `RemoteService`는 데이터를 반환할 때 어떠한 작업도 하지않고 단순히 입력받은 값을 반환하고 있다.
하지만 실제 서비스에서 하나의 PC 내부에서 통신하는 일은 없으며 로직없이 바로 반환되는 경우는 더욱없다.
아래에서는 `RemoteService`를 조금 더 현실적인 서비스로 만들어보고 테스트를 진행한다.

---

### RemoteServer 고도화

`RemoteServer`의 코드를 수정하여 기존보다 sleep을 2초를 주어 작업을 처리하는데 2초의 시간이 소요되는 것 같은 효과를 준다.

```java
@SpringBootApplication
public class RemoteService {
    @RestController
    public static class RemoteController {
        @GetMapping("/remote-service/{request}")
        public String service(@PathVariable String request) throws InterruptedException {
            TimeUnit.SECONDS.sleep(2);
            return request;
        }
    }
    public static void main(String[] args) {
        System.setProperty("server.port", "8081");
        System.setProperty("server.tomcat.max-threads", "1000");
        SpringApplication.run(RemoteService.class, args);
    }
}
```

코드가 이렇게 작성되면 우리의 `MyService`는 `RemoteService`를 요청하고 2초를 대기해야하며 실제로 외부 서비스를 요청하고 대기하는 것과 비슷한 상황이 연출되었다.
참고로 `RestTemplate의 getForObject` 메서드는 블록킹 메서드로 `RemoteService`각 응답할 때까지 대기하고 다른 작업을 하지않고 대기하고 있는다.
또한 우리는 `MyService`의 톰캣 스레드를 1개로 지정하였기 때문에 하나의 스레드가 `getForObject`의 응답 값을 기다리는 동안 다른 스레드는 클라이언트의 요청을 처리할 방법이 없다.

이러한 상황에서 어떻게 작동하는지 출력을 확인해본다.

![](improveblocking_image/blocked-getforobject-blocking.png)

우리가 예상한 것처럼 요청들이 순서대로 처리되고 하나의 요청이 처리되는데 2초가 걸렸으니 100개의 요청이 처리되는데 200초가 넘는 시간이 소요되었다. 

















---