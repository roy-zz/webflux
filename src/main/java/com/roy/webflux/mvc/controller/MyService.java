package com.roy.webflux.mvc.controller;

import com.roy.webflux.mvc.controller.service.MyLogic;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

        @GetMapping("/my-service/async-complex/{idx}")
        public DeferredResult<String> complexAsyncService(@PathVariable String idx) {
            DeferredResult<String> deferredResult = new DeferredResult<>();

            ListenableFuture<ResponseEntity<String>> future1 = nettyRestTemplate.getForEntity(
                    "http://localhost:8081/remote-service-1/{request}", String.class, idx);
            future1.addCallback(success -> {
                ListenableFuture<ResponseEntity<String>> future2 = nettyRestTemplate.getForEntity(
                        "http://localhost:8081/remote-service-2/{request}", String.class, Objects.requireNonNull(success).getBody());
                future2.addCallback(success2 -> {
                            deferredResult.setResult(Objects.requireNonNull(success2).getBody());
                        }, ex2 -> {
                            deferredResult.setErrorResult(ex2.getMessage());
                        });
                }, ex -> {
                deferredResult.setErrorResult(ex.getMessage());
            });
            return deferredResult;
        }

        @Autowired
        private MyLogic myLogic;

        @GetMapping("/my-service/async-complex-with-logic/{idx}")
        public DeferredResult<String> complexAsyncWithService(@PathVariable String idx) {
            DeferredResult<String> deferredResult = new DeferredResult<>();

            ListenableFuture<ResponseEntity<String>> future1 = nettyRestTemplate.getForEntity(
                    "http://localhost:8081/remote-service-1/{request}", String.class, idx);
            future1.addCallback(success -> {
                ListenableFuture<ResponseEntity<String>> future2 = nettyRestTemplate.getForEntity(
                        "http://localhost:8081/remote-service-2/{request}", String.class, Objects.requireNonNull(success).getBody());
                future2.addCallback(success2 -> {
                    ListenableFuture<String> future3 = myLogic.work(Objects.requireNonNull(success2).getBody());
                    future3.addCallback(success3 -> {
                        deferredResult.setResult(success3);
                    }, ex3 -> {
                        deferredResult.setErrorResult(ex3.getMessage());
                    });
                }, ex2 -> {
                    deferredResult.setErrorResult(ex2.getMessage());
                });
            }, ex -> {
                deferredResult.setErrorResult(ex.getMessage());
            });
            return deferredResult;
        }

    }

    @Bean
    public ThreadPoolTaskExecutor myThreadPool() {
        ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
        te.setCorePoolSize(1);
        te.setMaxPoolSize(1);
        te.initialize();
        return te;
    }

    public static void main(String[] args) {
        SpringApplication.run(MyService.class, args);
    }

}
