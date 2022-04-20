package com.roy.webflux.reactivestream.operators.reactor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.stream.Stream;

@Slf4j
public class ReactorTest {

    @Test
    void reactorOperatorsTest() {
        Flux.create(emitter -> {
            Stream.iterate(1, a -> a + 1)
                    .limit(10)
                    .forEach(job -> emitter.next(job * 2));
            emitter.complete();
        })
                .log()
                .subscribe(s -> log.info("{}", s));
    }

    @Test
    void reactorBiOperatorsTest() {
        Flux.create(emitter -> {
            Stream.iterate(1, a -> a + 1)
                    .limit(10)
                    .forEach(emitter::next);
            emitter.complete();
        })
                .reduce(new StringBuilder(), (a, b) -> a.append(b).append(", "))
                .subscribe(s -> log.info("{}", s));
    }

}
