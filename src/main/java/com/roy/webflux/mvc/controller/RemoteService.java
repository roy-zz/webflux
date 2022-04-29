package com.roy.webflux.mvc.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Slf4j
@EnableAsync
@SpringBootApplication
public class RemoteService {
    @RestController
    public static class RemoteController {
        @GetMapping("/remote-service-1/{request}")
        public String service1(@PathVariable String request) throws InterruptedException {
            TimeUnit.SECONDS.sleep(2);
            return String.format("remote-service-1: %s", request);
        }

        @GetMapping("/remote-service-2/{request}")
        public String service2(@PathVariable String request) throws InterruptedException {
            TimeUnit.SECONDS.sleep(2);
            return String.format("remote-service-2: %s", request);
        }
    }

    public static void main(String[] args) {
        System.setProperty("server.port", "8081");
        System.setProperty("server.tomcat.max-threads", "1000");
        SpringApplication.run(RemoteService.class, args);
    }

}
