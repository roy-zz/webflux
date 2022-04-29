package com.roy.webflux.mvc.controller;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Objects;

@Slf4j
@EnableAsync
@SpringBootApplication
public class MyService {
    @RestController
    public static class MyController {
        private AsyncRestTemplate nettyRestTemplate = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(
                new NioEventLoopGroup(1)
        ));
        private AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate();
        private RestTemplate restTemplate = new RestTemplate();
        @GetMapping("/my-service/rest/{idx}")
        public String service(@PathVariable String idx) {
            String response = restTemplate.getForObject("http://localhost:8081/remote-service/{request}", String.class, idx);
            return String.format("my-response: %s, remote-response: %s", idx, response);
        }

        @GetMapping("/my-service/async-rest/{idx}")
        public ListenableFuture<ResponseEntity<String>> asyncService(@PathVariable String idx) {
            return asyncRestTemplate.getForEntity("http://localhost:8081/remote-service/{request}", String.class, idx);
        }

        @GetMapping("/my-service/async-netty/{idx}")
        public ListenableFuture<ResponseEntity<String>> nettyService(@PathVariable String idx) {
            return nettyRestTemplate.getForEntity("http://localhost:8081/remote-service/{request}", String.class, idx);
        }

        // DeferredResult를 반환하도록 구현하면 바로 값을 반환하는 것이 아니라 언젠가 DeferredResult에 값이 들어오면 들어온 값을 반환하는 방식이다.
        @GetMapping("/my-service/async-custom/{idx}")
        public DeferredResult<String> customAsyncService(@PathVariable String idx) {
            DeferredResult<String> deferredResult = new DeferredResult<>();
            ListenableFuture<ResponseEntity<String>> future = asyncRestTemplate.getForEntity(
                    "http://localhost:8081/remote-service/{request}", String.class, idx);
            future.addCallback(success -> {
                deferredResult.setResult(Objects.requireNonNull(success).getBody() + "/work");
            }, failure -> {
                deferredResult.setErrorResult(failure.getMessage());
            });
            return deferredResult;
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(MyService.class, args);
    }

    @Bean
    ThreadPoolTaskExecutor myExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(100);
        taskExecutor.setThreadNamePrefix("my-service-thread-");
        return taskExecutor;
    }
}
