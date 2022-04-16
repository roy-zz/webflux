package com.roy.webflux.reactivestream.reactivestreams.async;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter @Setter
@AllArgsConstructor
public class AsyncJob {
    private String name;
    public void process() {
        try {
            System.out.printf("AsyncJob.process Thread Name: %s%n", Thread.currentThread().getName());
            Thread.sleep(100L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
