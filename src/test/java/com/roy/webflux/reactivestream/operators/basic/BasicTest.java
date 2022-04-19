package com.roy.webflux.reactivestream.operators.basic;

import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;
import java.util.function.Function;

public class BasicTest {
    @Test
    void basicPubSubFunctionTest() {
        BasicSubscriber basicSubscriber = new BasicSubscriber();
        BasicPublisher basicPublisher = new BasicPublisher();
        Function<Integer, Integer> basicFunction = new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer integer) {
                return integer * 2;
            }
        };
        BasicOperation basicOperation = new BasicOperation(basicPublisher, basicFunction);
        basicOperation.subscribe(basicSubscriber);
    }

    @Test
    void basicPubSubBiFunctionTest() {
        BasicSubscriber basicSubscriber = new BasicSubscriber();
        BasicPublisher basicPublisher = new BasicPublisher();
        BiFunction<StringBuilder, Integer, StringBuilder> basicBiFunction = new BiFunction<StringBuilder, Integer, StringBuilder>() {
            @Override
            public StringBuilder apply(StringBuilder sb, Integer integer) {
                return sb.append(integer).append(", ");
            }
        };
        BasicBiOperation basicBiOperation = new BasicBiOperation(basicPublisher, basicBiFunction, new StringBuilder());
        basicBiOperation.subscribe(basicSubscriber);
    }
}
