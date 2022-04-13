package com.roy.webflux.controller;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reactive Streams 표준API 만으로 구현하는 MVC 프로그래밍
 * 우리는 Publisher만 생성하고 Subscriber는 스프링이 만들고 onSubscribe 또한 스프링이 호출한다.
 */
@RestController
@RequestMapping("/reactor")
public class ReactorController {

    @GetMapping
    public Publisher<String> reactor(@RequestParam("param") String message) {
        return new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> s) {
                s.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        s.onNext("Message: " + message);
                        s.onComplete();
                    }
                    @Override
                    public void cancel() {

                    }
                });
            }
        };
    }

}
