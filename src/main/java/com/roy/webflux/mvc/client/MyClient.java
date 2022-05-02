package com.roy.webflux.mvc.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@EnableAsync
public class MyClient {
    private static final AtomicInteger idx = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        ExecutorService es = Executors.newFixedThreadPool(1000);
        RestTemplate rt = new RestTemplate();
        // String url = "http://localhost:8080/my-service/rest/{idx}";
        // String url = "http://localhost:8080/my-service/async-rest/{idx}";
        // String url = "http://localhost:8080/my-service/async-netty/{idx}";
        // String url = "http://localhost:8080/my-service/async-custom/{idx}";
        // String url = "http://localhost:8080/my-service/async-complex/{idx}";
        // String url = "http://localhost:8080/callback-hell/resolve/v1/{idx}";
        // String url = "http://localhost:8080/callback-hell/resolve/v2/{idx}";
        // String url = "http://localhost:8080/callback-hell/resolve/v3/{idx}";
        String url = "http://localhost:8080/callback-hell/resolve/v4/{idx}";
        CyclicBarrier barrier = new CyclicBarrier(101);

        for (int i = 0; i < 100; i++) {
            es.submit(() -> {
                int index = idx.addAndGet(1);
                barrier.await();
                log.info("Thread {}", index);
                StopWatch sw = new StopWatch();
                sw.start();
                String response = rt.getForObject(url, String.class, index);
                sw.stop();
                log.info("Elapsed: {} {}, {}", index, sw.getTotalTimeSeconds(), response);
                return null;
            });
        }

        barrier.await();
        StopWatch main = new StopWatch();
        main.start();
        es.shutdown();
        es.awaitTermination(100, TimeUnit.SECONDS);
        main.stop();
        log.info("Total: {}", main.getTotalTimeSeconds());
    }

}
