package com.roy.webflux.mvc.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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
