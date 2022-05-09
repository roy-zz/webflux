package com.roy.webflux.practice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Slf4j
@EnableAsync
@RestController
@SpringBootApplication
public class WebFluxController {

    private final static String URL_1 = "http://localhost:8081/remote-service-1/{request}";
    private final static String URL_2 = "http://localhost:8081/remote-service-2/{request}";

    private WebClient client = WebClient.create();

    @GetMapping("/webflux/practice")
    public Mono<String> rest(int idx) {
        return client.get().uri(URL_1, idx).exchange()
                .flatMap(c -> c.bodyToMono(String.class));
    }

    @GetMapping("/webflux/practice/v2")
    public Mono<String> restV2(int idx) {
        return client.get().uri(URL_1, idx).exchange()                                // Mono<ClientResponse>
                .flatMap(c -> c.bodyToMono(String.class))                             // Mono<String>
                .flatMap((String res1) -> client.get().uri(URL_2, res1).exchange())   // Mono<ClientResponse>
                .flatMap(c -> c.bodyToMono(String.class));                            // Mono<String>
    }

    @Autowired
    private MyService myService;

    @GetMapping("/webflux/practice/v3")
    public Mono<String> restV3(int idx) {
        return client.get().uri(URL_1, idx).exchange()                                // Mono<ClientResponse>
                .flatMap(c -> c.bodyToMono(String.class))                             // Mono<String>
                .doOnNext(log::info)
                .flatMap((String res1) -> client.get().uri(URL_2, res1).exchange())   // Mono<ClientResponse>
                .flatMap(c -> c.bodyToMono(String.class))                             // Mono<String>
                .doOnNext(log::info)
                .flatMap(res2 -> Mono.fromCompletionStage(myService.work(res2)))      // Mono<String>
                .doOnNext(log::info);
    }

    public static void main(String[] args) {
        System.setProperty("reactor.ipc.netty.workerCount", "2");
        System.setProperty("reactor.ipc.netty.pool.maxConnections", "2000");
        SpringApplication.run(WebFluxController.class, args);
    }

    @Service
    public static class MyService {
        @Async
        public CompletableFuture<String> work(String req) {
            return CompletableFuture.completedFuture(req + "/asyncwork");
        }
    }

}
