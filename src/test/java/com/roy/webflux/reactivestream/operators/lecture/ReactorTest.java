package com.roy.webflux.reactivestream.operators.lecture;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

/**
 * Flux: Operator 테스트에서 사용한 org.reactivestreams.Publisher를 상속받아서 확장한 유틸리티 클래스
 * log() 메서드는 체이닝을 끊지 않으면서 중간에 데이터가 전송되는 결과를 출력해주는 역할을 한다.
 * unbounded: request(n)에서 n의 값을 지정하지 않았기 때문에 unbounded라고 표시된다.
 */
@Slf4j
public class ReactorTest {

    @Test
    @DisplayName("Reactor 테스트")
    void reactorTest() {
//        Flux.create(emitter -> {
//            emitter.next(1);
//            emitter.next(2);
//            emitter.next(3);
//            emitter.complete();
//        })
//                .log()
//                .subscribe(s -> log.info("{}", s));

//        Flux.<Integer>create(emitter -> {
//            emitter.next(1);
//            emitter.next(2);
//            emitter.next(3);
//            emitter.complete();
//        })
//                .log()
//                .map(s -> s * 10)
//                .log()
//                .subscribe(s -> log.info("{}", s));

        Flux.<Integer>create(emitter -> {
                    emitter.next(1);
                    emitter.next(2);
                    emitter.next(3);
                    emitter.complete();
                })
                .log()
                .map(s -> s * 10)
                .reduce(0, (a, b) -> a + b)
                .log()
                .subscribe(s -> log.info("{}", s));
    }

}
