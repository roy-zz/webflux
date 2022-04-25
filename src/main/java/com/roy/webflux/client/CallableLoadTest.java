package com.roy.webflux.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// 부하 테스트를 위하여 StressTest라는 클래스를 작성하였다.
// 해당 클래스는 클라이언트의 역할을 하며 100개의 요청을 모아서 컨트롤러가 한번에 처리하도록 유도하는 역할을 한다.
// 100개의 요청을 받았을 때 우리의 코드가 어떻게 작동을 하고 어떻게 스레드가 할당되는지 한 번 살펴보도록 한다.
// awaitTermination은 지정한 시간 내에서 shutdown 작업이 종료될 때까지 대기한다.
// 만약 코드가 awaitTermination을 지나서 내려왔다면 대기 시간을 초과했거나 작업이 완료되었다는 의미가 된다.

// 먼저 비동기 컨트롤러가 아닌 기존에 사용하던 동기 방식인 /sync API를 호출해본다.
// 하나의 요청당 2초 정도의 시간이 소요되고 100개의 요청이 동시에 처리되었으므로 총 소요된 시간도 2초 정도의 시간이 소요되었다.
// 스레드가 어떻게 작동하는지 자세히 보기 위해서 VisualVM이라는 툴을 사용할 것이다.
// VisualVM의 Threads 탭을 선택하여 스레드의 상태를 확인해본다.
// 요청을 처리하기 위해서 100개의 nio 스레드가 생성된 것을 확인할 수 있다.
// 톰캣의 기본 스레드 풀의 크기가 200개이므로 100개의 요청은 당연히 처리가 가능하다.
// application.yml 파일을 수정하여 톰캣이 한 번에 20개의 요청만 처리할 수 있도록 최대 스레드의 수를 20으로 줄여보고 다시 테스트를 진행해본다.
// 요청은 100개이지만 한 번에 처리가능한 요청은 20개 밖에 안되는 상황이다.
// 100개의 요청을 20개씩 5번으로 나눠서 처리해야하기 때문에 기존의 5배인 10초가 걸리는 것을 확인할 수 있다.
// 하지만 우리의 heavyJob이라는 작업이 처리되는 동안 굳이 서블릿 스레드가 블로킹 되어 있을 필요는 없다.
// 모든 설정은 기존대로 유지하고 서블릿 스레드가 블로킹되지 않도록 /callable-async API를 호출해본다.
// 동기 방식과 다르게 같은 수의 요청이지만 2초 대에 해결되는 것을 확인할 수 있다.
// VisualVM을 확인해보면 결국 MvcThread라는 스레드가 200개가 생겨서 처리한 것을 확인할 수 있다.
// 그렇다면 톰캣의 서블릿 스레드를 만드는 것과 비동기 작업 스레드를 생성하는 것과 큰 차이가 없다고 생각될 수 있다.
// 작업 스레드를 새로 생성하여 오래 걸리는 작업을 처리하는 용도로만 사용한다면 큰 의미는 없을 수 있다.
// 작업 스레드를 따로 두는 것을 효율적으로 활용하기 위해서는 모든 작업을 작업 스레드에서 처리하는 것이 아니라 일반적인 요청은 서블릿 스레드에서 처리하고
// 오랜 시간이 걸리는 작업만 작업 스레드를 사용하여 일반 작업과 무거운 작업을 분리해서 활용해야 한다.
// 우리가 이러한 비동기 서블릿 방식에서 기억해야할 점은 하나의 서블릿 스레드로도 수많은 요청을 한 번에 처리할 수 있다는 점이다.
// 여기까지가 기본적인 워커 스레드 활용 방법이었다.

// 실제로 서블릿 스레드를 하나만 두고 워커 스레드도 제한된 상황에서 DeferredResultQueue를 사용하여 수많은 작업을 동시에 처리할 수 있다.
// 우리는 스프링 비동기 기술의 꽃이라고 할 수 있는 DeferredResult(지연된 결과)에 대해서 이해해야한다.
// 클라이언트의 요청이 10개가 들어왔다고 가정하면 10개의 요청을 DeferredResult에 저장하고 있다가 한 번에 혹은 하나씩 결과를 적어줄 수 있다.
// 즉, 이벤트 또는 또 다른 클라이언트의 요청에 의해서 기존에 지연되어 있는 HTTP 응답을 나중에 써줄 수 있게 해주는 기능이다.

@Slf4j
@EnableAsync
public class CallableLoadTest {
    private static final AtomicInteger sequence = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(1000);
        RestTemplate rt = new RestTemplate();
        // String url = "http://localhost:8080/sync";
        String url = "http://localhost:8080/callable-async";

        StopWatch main = new StopWatch();
        main.start();

        for (int i = 0; i < 100; i++) {
            es.execute(() -> {
                int index = sequence.addAndGet(1);
                // log.info("Thread {}", index);
                StopWatch sw = new StopWatch();
                sw.start();
                rt.getForObject(url, String.class);
                sw.stop();
                // log.info("Elapsed: {}, {}", index, sw.getTotalTimeSeconds());
            });
        }
        es.shutdown();
        es.awaitTermination(100, TimeUnit.SECONDS);
        main.stop();
        log.info("Total: {}", main.getTotalTimeSeconds());
    }

}
