package com.roy.webflux.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class MyLogic {

    @Async
    public ListenableFuture<String> work(String request) {
        return new AsyncResult<>(String.format("asyncwork/%s", request));
    }

}
