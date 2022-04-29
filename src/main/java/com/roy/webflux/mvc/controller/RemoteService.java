package com.roy.webflux.mvc.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
        @GetMapping("/remote-service/{request}")
        public String service(@PathVariable String request) throws InterruptedException {
            log.info("진입");
            TimeUnit.SECONDS.sleep(2);
            return request;
        }
    }

    public static void main(String[] args) {
        System.setProperty("server.port", "8081");
        System.setProperty("server.tomcat.max-threads", "1000");
        SpringApplication.run(RemoteService.class, args);
    }

    @Bean
    ThreadPoolTaskExecutor remoteExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setThreadNamePrefix("remote-service-thread-");
        return taskExecutor;
    }
}
