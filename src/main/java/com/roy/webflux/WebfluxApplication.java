package com.roy.webflux;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@EnableAsync
@SpringBootApplication
public class WebfluxApplication {

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

		// DeferredResult를 저장할 수 있는 큐를 생성한다.
		// /deferred-result API에서는 위에서 만든 큐에 새로운 DeferredResult 객체를 저장한다.
		// /deferred-result/count API에서는 큐의 사이즈를 반환한다.
		// /deferred-result/event API에서는 큐에 있는 데이터를 확인하면서 전부 제거한다.
		// 브라우저를 켜고 localhost:8080/deferred-result 에 접속하면 브라우저는 아무런 화면도 뜨지않고 계속 서버의 응답을 받기 위해 대기할 것이다.
		// 브라우저를 추가로 하나 켜고 localhost:8080/deferred-result/event를 호출하면 이전에 켜놓았던 브라우저의 요청이 정상적으로 되는 것을 확인할 수 있다.
		// DeferredResult는 setResult 또는 setException이 발생하기 전까지 응답하지 않고 대기상태로 존재한다.
		// DeferredResult의 최대 장점은 작업을 처리하기 위해 새로운 스레드를 만들지 않는다는 점이다.
		// DeferredResult 객체만 유지하고 있으면 언제든지 객체를 불러와서 결과를 넣어주면 컨트롤러에서 응답값을 반환하는 것과 같이 바로 결과를 반환한다.
		// 한정된 자원을 최대의 효율로 처리하는데 좋은 방식이다. 물론 이벤트성 구조인 경우 유리하다.
		// 비동기 I/O를 이용한 외부 I/O 호출할 때도 이러한 방식을 사용한다.
		// 이번에는 톰캣의 최대 스레드의 수를 1개로 설정하고 테스트를 진행해본다.
		// 클라이언트 역할을 하는 코드에서는 100개의 요청을 설정하고 한 번에 요청을 전달한다.
		// 서버에서는 요청에 대한 새로운 DeferredResult만 생성하고 아직 DeferredResult 객체의 setResult 또는 setException을 호출하지 않았기 때문에 요청에 대해 응답하지 않고 대기하게 된다.
		// 이 상태에서 /deferred-result/count API를 호출하면 큐의 총 크기인 100을 보여주는 것을 확인할 수 있다.
		// /deferred-result/event API를 호출하면 큐에 저장되어 있던 100개의 요청이 한 번에 처리되는 것을 볼 수 있다.
		// VisualVM을 확인해보면 서블릿 스레드는 하나뿐이며 워커 스레드는 생성되지 않은 것을 확인할 수 있다.
		// DeferredResult와 NIO를 사용하여 서버의 자원을 최소한으로 사용하면서 최대의 효율을 끌어내는 것이 가능해진다.

		private final AtomicInteger requestCount = new AtomicInteger(0);
		private final Queue<DeferredResult<String>> results = new ConcurrentLinkedQueue<>();
		@GetMapping("/deferred-result")
		public DeferredResult<String> deferredResult() throws InterruptedException {
			log.info("Call deferred result");
			DeferredResult<String> dr = new DeferredResult<>();
			results.add(dr);
			requestCount.addAndGet(1);
			if (requestCount.get() == 100) {
				deferredEvent("Success");
			}
			return dr;
		}

		@GetMapping("/deferred-result/count")
		public String deferredCount() {
			return String.valueOf(results.size());
		}

		@GetMapping("/deferred-result/event")
		public String deferredEvent(String message) {
			for (DeferredResult<String> dr : results) {
				dr.setResult("Hello " + message);
				results.remove(dr);
			}
			return "OK";
		}

		// ResponseBodyEmitter, SseEmitter, StreamingResponseBody 세가지가 모두 같은 역할을 한다.
		// 우리는 이들 중 ResponseBodyEmitter에 대해서 알아본다.
		// HTTP 요청을 여러 번으로 나누어 전달하는 방식이다.
		// 아래와 같이 컨트롤러 코드를 작성하고 localhost:8080/emitter 페이지에 접속해보자.
		// 0.1초 간격으로 지속적으로 데이터가 출력되는 것을 확인할 수 있다.
		// 복잡하지 않은 스트리밍 방식의 응답코드를 쉽게 만들 수 있다.


		// 지금까지 자바와 스프링 비동기 기술의 기본이 되는 기술들에 대해서 알아보았다.
		// 기능적인 부분뿐만 아니라 VisualVM을 통한 리소스 사용량까지 확인해보았다.
//		@GetMapping("/emitter")
//		public ResponseBodyEmitter emitter() {
//			ResponseBodyEmitter emitter = new ResponseBodyEmitter();
//			Executors.newSingleThreadExecutor().submit(() -> {
//				try {
//					for (int i = 1; i <= 50; i++) {
//						emitter.send("<p>Stream " + i + "</p>");
//						Thread.sleep(100);
//					}
//				} catch (Exception e) {}
//			});
//			return emitter;
//		}
	}

	public static void main(String[] args) {
		SpringApplication.run(WebfluxApplication.class, args);
	}

	@Bean
	protected ThreadPoolTaskExecutor myExecutors() throws InterruptedException {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(100);
//		taskExecutor.setCorePoolSize(100);
//		taskExecutor.setMaxPoolSize(20);
//		taskExecutor.setQueueCapacity(50);
		taskExecutor.setThreadNamePrefix("my-thread-");
		return taskExecutor;
	}
}
