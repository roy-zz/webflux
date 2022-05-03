package com.roy.webflux.mvc.controller;

import com.roy.webflux.mvc.controller.service.MyLogic;
import com.roy.webflux.util.*;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@EnableAsync
@SpringBootApplication
public class CallbackHellService {

    public static void main(String[] args) {
        SpringApplication.run(CallbackHellService.class);
    }

    @RestController
    public static class CallbackHellController {
        @Autowired
        private MyService myService;
        @Autowired
        private MyLogic myLogic;

        private final static String URL_1 = "http://localhost:8081/remote-service-1/{request}";
        private final static String URL_2 = "http://localhost:8081/remote-service-2/{request}";

        AsyncRestTemplate rt = new AsyncRestTemplate(
                new Netty4ClientHttpRequestFactory(
                        new NioEventLoopGroup(1)));

        @GetMapping("/callback-hell/rest/{idx}")
        public DeferredResult<String> callbackHellRest(@PathVariable int idx) {
            DeferredResult<String> dr = new DeferredResult<>();

            ListenableFuture<ResponseEntity<String>> future1 = rt.getForEntity(URL_1, String.class, "h" + idx);
            future1.addCallback(
                    success1 -> {
                            ListenableFuture<ResponseEntity<String>> future2 = rt.getForEntity(URL_2, String.class, Objects.requireNonNull(success1).getBody());
                            future2.addCallback(
                                    success2 -> {
                                        ListenableFuture<String> future3 = myLogic.work(Objects.requireNonNull(success2).getBody());
                                        future3.addCallback(
                                                success3 -> {
                                                        dr.setResult(success3);
                                                    },
                                                failure3 -> {
                                                    dr.setErrorResult(failure3.getMessage());
                                                });
                                        },
                                    failure2 -> {
                                        dr.setErrorResult(failure2.getMessage());
                                    });
                        },
                    failure1 -> {
                        dr.setErrorResult(failure1.getMessage());
                    });
            return dr;
        }

        @GetMapping("/callback-hell/refactoring/v1/{idx}")
        public DeferredResult<String> callbackHellRefactoring(@PathVariable int idx) {
            DeferredResult<String> dr = new DeferredResult<>();
            toCompletableFuture(rt.getForEntity(URL_1, String.class, "hell_" + idx))
                    .thenCompose(s1 -> toCompletableFuture(rt.getForEntity(URL_2, String.class, s1.getBody())))
                    .thenCompose(s2 -> toCompletableFuture(myLogic.work(s2.getBody())))
                    .thenAccept(dr::setResult)
                    .exceptionally(ex -> {
                        dr.setErrorResult(ex.getMessage());
                        return (Void) null;
                    });
            return dr;
        }

        private <T> CompletableFuture<T> toCompletableFuture(ListenableFuture<T> lf) {
            CompletableFuture<T> cf = new CompletableFuture<>();
            lf.addCallback(cf::complete, cf::completeExceptionally);
            return cf;
        }

        @GetMapping("/callback-hell/resolve/v1/{idx}")
        public DeferredResult<String> callbackHellResolveV1(@PathVariable int idx) {
            DeferredResult<String> dr = new DeferredResult<>();
            CompletionV1
                    .from(rt.getForEntity(URL_1, String.class, "v1_" + idx))
                    .andAccept(success -> dr.setResult(success.getBody()));
            return dr;
        }

        @GetMapping("/callback-hell/resolve/v2/{idx}")
        public DeferredResult<String> callbackHellResolveV2(@PathVariable int idx) {
            DeferredResult<String> dr = new DeferredResult<>();
            CompletionV2
                    .from(rt.getForEntity(URL_1, String.class, "v2_" + idx))
                    .andApply(success -> rt.getForEntity(URL_2, String.class, success.getBody()))
                    .andAccept(success -> dr.setResult(success.getBody()));
            return dr;
        }

        @GetMapping("/callback-hell/resolve/v3/{idx}")
        public DeferredResult<String> callbackHellResolveV3(@PathVariable int idx) {
            DeferredResult<String> dr = new DeferredResult<>();
            CompletionV3
                    .from(rt.getForEntity(URL_1, String.class, "v3_" + idx))
                    .andApply(success -> rt.getForEntity(URL_2, String.class, success.getBody()))
                    .andAccept(success -> dr.setResult(success.getBody()));
            return dr;
        }

        @GetMapping("/callback-hell/resolve/v4/{idx}")
        public DeferredResult<String> callbackHellResolveV4(@PathVariable int idx) {
            DeferredResult<String> dr = new DeferredResult<>();
            CompletionV4
                    .from(rt.getForEntity(URL_1, String.class, "v4_" + idx))
                    .andApply(success -> rt.getForEntity(URL_2, String.class, success.getBody()))
                    .andError(dr::setErrorResult)
                    .andAccept(success -> dr.setResult(success.getBody()));
            return dr;
        }

        @GetMapping("/callback-hell/resolve/v5/{idx}")
        public DeferredResult<String> callbackHellResolveV5(@PathVariable int idx) {
            DeferredResult<String> dr = new DeferredResult<>();
            CompletionV5
                    .from(rt.getForEntity(URL_1, String.class, "v5_" + idx))
                    .andApply(success -> rt.getForEntity(URL_2, String.class, success.getBody()))
                    .andApply(success -> myLogic.work(success.getBody()))
                    .andError(dr::setErrorResult)
                    .andAccept(dr::setResult);
            return dr;
        }
    }
}
