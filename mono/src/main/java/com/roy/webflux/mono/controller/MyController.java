package com.roy.webflux.mono.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

@Slf4j
@RestController
public class MyController {

    @GetMapping("/")
    public Mono<String> hello() {
        return Mono.just("Hello Webflux");
    }

    @GetMapping("/log/v1")
    public Mono<String> logV1() {
        return Mono.just("Hello Webflux").log();
    }

    @GetMapping("/log/v2")
    public Mono<String> logV2() {
        log.info("position 1");
        Mono<String> mono = Mono.just("Hello Webflux").log();
        log.info("position 2");
        return mono;
    }

    @GetMapping("/log/v3")
    public Mono<String> logV3() {
        log.info("position 1");
        String message = generateHello();
        Mono<String> mono = Mono.just(message).doOnNext(log::info).log();
        log.info("position 2");
        return mono;
    }

    @GetMapping("/log/v4")
    public Mono<String> logV4() {
        log.info("position 1");
        Mono<String> mono = Mono.fromSupplier(this::generateHello).doOnNext(log::info).log();
        log.info("position 2");
        return mono;
    }

    @GetMapping("/log/v5")
    public Mono<String> logV5() {
        log.info("position 1");
        Mono<String> mono = Mono.fromSupplier(new Supplier<String>() {
                    @Override
                    public String get() {
                        return generateHello();
                    }
                }).doOnNext(log::info).log();
        log.info("position 2");
        return mono;
    }

    @GetMapping("/log/v6")
    public Mono<String> logV6() {
        log.info("position 1");
        Mono<String> mono = Mono.fromSupplier(this::generateHello).doOnNext(log::info).log();
        mono.subscribe();
        log.info("position 2");
        return mono;
    }

    @GetMapping("/log/v7")
    public Mono<String> logV7() {
        log.info("position 1");
        Mono<String> mono = Mono.just(generateHello()).doOnNext(log::info).log();
        String message = mono.block();
        log.info("position 2: " + message);
        return mono;
    }

    private String generateHello() {
        log.info("Method generateHello()");
        return "Hello Mono";
    }

}
