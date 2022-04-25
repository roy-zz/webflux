package com.roy.webflux.reactivestream.asyncspring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
@EnableAsync
@SpringBootApplication
public class AsyncJavaSpringTest3 {
    // Servlet 3.0: 비동기 서블릿
    // HTTP Connection은 논블록킹 IO
    // 서블릿 요청 읽기, 응답 쓰기는 블록킹
    // 비동기 작업 시작 즉시 서블릿 스레드 반납
    // 비동기 작업이 완료되면 서블릿 스레드 재할당
    // 비동기 서블릿 컨텍스트 이용 (AsyncContext)
    // Servlet 3.1: 논블록킹 IO
    // 논블록킹 서블릿 요청, 응답 처리
    // Callback
    //
    // 서블릿 3.0이 넘어가면서 이미 서블릿 위에서 동작하는 스프링 프레임워크를 사용하게 되었다.
    // 서블릿 3.0에는 비동기적으로 서블릿 요청을 처리하는 기능이 추가되었다.
    // 기본적으로 서블릿은 블록킹 구조로 이루어져있다. 블록킹이 I/O에서 가지는 큰 의미는 I/O가 발생할 때마다 서블릿에서 스레드를 하나씩 할당 해주었다.
    // 여기서 I/O는 HTTP로 브라우저가 요청을 하는 것, 요청을 받아서 결과를 반환하는 것을 의미한다.
    // 동시에 커넥션이 100개가 생기면 스레드 또한 100개가 사용되었으며 블로킹 방식에서는 어쩔 수 없는 구조다.
    // 서블릿이 기본적으로 블로킹 구조인 이유는 HTTP 커넥션과 연관되어 있는 HttpServletRequest와 HttpServletResponse 두 개가 있는데
    // 내부적으로 InputStream, OutputStream을 구현했고 여기서 사용되는 read와 같은 메서드는 기본적으로 블로킹 I/O 방식이다.
    // read를 사용한 순간에 I/O쪽에 아무런 데이터가 없으면 대기하고 있다가 어떠한 데이터가 반환되거나 커넥션이 끊어지는 경우에 반환된다.
    // 스레드가 블로킹되는 순간 CPU는 스레드를 대기상태로 전환(스위칭 한 번)하고 다시 작업을 시작할 때 Running 상태로 바꾸면서 전환(스위칭 한 번)이 발생하면서 총 두 번의 Context Switching이 발생하게 된다.
    // 이렇게 스레드 블로킹을 많이 사용하게 되면 스레드간 Context Switching이 많이 일어나게 되고 불필요하게 많은 CPU 자원을 사용하게 된다.

    // 우리가 사용하는 스프링의 기본적으로 사용되는 HttpServletRequest와 HttpServletResponse는 기본적으로 InputStream을 사용하고 있다.
    // 이미 NIO를 사용하여 이러한 한계를 극복해보려 하였지만 큰 성과는 없었다.
    // 사실 스레드 하나에서 request -> 서비스 로직 -> response를 처리하는 것은 큰 문제가 되지 않는다.
    // 문제가 되는 상황은 request -> DB Connection or 외부 API 통신 -> response 과 같이 중간에 오래걸리는 작업이 중간에 들어가게 되면
    // 해당 요청을 처리하는 스레드는 요청이 처리되기 전까지 불필요한 Context Switching을 유발하며 블로킹 상태가 된다.
    // 이렇게 블로킹 상태가 된 스레드가 많아지면 결국 스레드는 전부 고갈될 것이고 클라이언트의 요청은 Queue에 쌓이게 될 것이다.
    // Queue까지 가득차게 되면 클라이언트는 서비스 불가라는 응답을 받게 된다. Latency는 높아지고 에러율이 높아지는 상태가 된다.

    // 스레드를 많이 만들면 되는거 아니냐는 의문이 발생할 수 있지만 자바에서 스레드는 내부적으로 Stack Trace를 가지고 있으며 이것은 메모리를 사용하는 것을 의미한다.
    // 만약 많아진 요청을 스레드의 수를 늘려서 해결한다면 우리는 OutOfMemory 에러를 만나게 될 것이다.
    // 또한 스레드가 많아지면 Contenxt Switching이 기존보다 훨씬 많이 발생하게 되면서 CPU 사용량이 치솟게 되며 오히려 응답속도가 느려질 수 있다.
    // 결국 스레드를 많이 만드는 것은 근본적인 해결 방법이 아니다.

    // 우리는 이러한 문제를 해결하기 위한 방법으로 Servlet 스레드가 블로킹I/O를 만났을 때 응답을 대기하고 있는 것이 아니라 바로 스레드 풀로 반환되어 다음 요청을 처리하게 해야하며 이러한 구조로 만드는 것이 비동기 서블릿이라는 기술이다.
    // 스프링 3.0에서 나온 비동기 서블릿 기술을 스프링 3.1 기술에서 많은 부분 보완하였고 논블로킹 웹개발을 위한 기반이 만들어졌다.

    // 스프링에서 어떠한 방식으로 논블로킹 웹개발을 가능하게 하였는지 살펴본다.
    // 가장 앞단의 NIO Connector가 클라이언트의 요청을 받는다.
    // NIO Connector는 서블릿 스레드를 만들거나 서블릿 스레드 풀에서 스레드를 가져온다.
    // 만약 스레드의 작업이 오래걸리거나 @Async와 같은 애노테이션을 사용하여 작업 스레드에게 작업을 위임하면 바로 스레드 풀로 반환된다.
    // 문제는 처리 중이던 "서블릿 스레드를 반납을 하면 누가 HTTP응답을 처리할 것이냐"인데
    // 이 부분은 비동기 서블릿 엔진 자체적으로 작업 스레드가 작업을 마치고 반환하는 시점에 스레드 풀에서 스레드를 재할당하여 NIO Connector에게 응답을 반환하고 바로 서블릿 스레드를 반납한다.
    // 이러한 메커니즘을 활용하여 적은 스레드의 수로 동시에 많은 요청을 처리할 수 있다.

    // 지금부터 어떠한 방식으로 비동기 서블릿 엔진이 작동하는지 코드로 알아보도록 한다.
    // /async API에서 처리하는데 2초가 소요되는 heavyJob 메서드를 호출한다. 우리의 서블릿은 동기방식으로 작동하기 때문에 heavyJob을 처리할 때 까지 서블릿의 스레드는 블로킹 되어 있을 것이다.
    // 이러한 경우 Callable을 사용하여 heavyJob을 비동기로 작동시키면 다른 스레드에서 작업을 처리하게 되면서 문제는 해결된다.
    // 출력된 결과를 살펴보면 아래와 같다.
    // 물론 클라이언트가 "HELLO"라는 문자를 보기까지 2초의 시간이 소요되는 것은 동일하지만 서블릿 스레드가 블로킹되지 않았다는 점은 큰 차이라고 볼 수 있다.
    // 스레드의 `Call async method`라는 문구를 출력한 스레드의 이름을 살펴보면 MvcAsync1인 것을 확인할 수 있다.
    // 우리가 비동기 방식으로 작동하는 코드를 컨트롤러에서 반환하면 스프링에서 비동기로 해당하는 코드를 실행시킨 후에 작업이 끝나면 반환하게 된다.
    // 그리고 컨트롤러 메서드에서 출력한 `Call callable async`를 출력한 스레드는 nio-8080-exec-1인 것을 확인할 수 있으며 해당 스레드는 비동기 작업인 heavyJob() 메서드가 종료되기를 기다리지 않고 다음 작업을 처리하기 위해 바로 반환된다.
    // 이렇게 비동기로 작동하면 서블릿 스레드는 같은 시간동안 많은 요청을 처리할 수 있게 되고 애플리케이션 전반적인 Throughput 이 높아진다.

    // 정말로 Throughput이 높아지는지 간단하게 부하 테스트하는 코드를 작성해본다.



    @Bean
    protected ThreadPoolTaskExecutor springExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(20);
        taskExecutor.setQueueCapacity(50);
        taskExecutor.setThreadNamePrefix("Spring-Thread-");
        return taskExecutor;
    }

    @RestController
    @RequiredArgsConstructor
    public static class MyController {
        private final MySpringService mySpringService;
        @GetMapping("/sync")
        public String sync() throws InterruptedException {
            log.info("Call sync");
            heavyJob();
            return "HELLO";
        }

        @GetMapping("/callable-async")
        public Callable<String> callableAsync() {
            log.info("Call callable async");
            return () -> {
                log.info("Call async method");
                heavyJob();
                return "HELLO";
            };
        }

        private void heavyJob() throws InterruptedException {
            TimeUnit.SECONDS.sleep(2);
        }
    }

    @Service
    public static class MySpringService {
        @Async
        public ListenableFuture<String> hello() throws InterruptedException {
            log.info("Call hello()");
            TimeUnit.SECONDS.sleep(1);
            return new AsyncResult<>("HELLO");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(AsyncJavaSpringTest3.class, args);
    }

}
