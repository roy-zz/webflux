package com.roy.webflux.util;

import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.function.Consumer;

@NoArgsConstructor
public class CompletionV1 {
    private CompletionV1 next;
    private Consumer<ResponseEntity<String>> consumer;
    public CompletionV1(Consumer<ResponseEntity<String>> consumer) {
        this.consumer = consumer;
    }

    public static CompletionV1 from(ListenableFuture<ResponseEntity<String>> lf) {
        CompletionV1 completionV1 = new CompletionV1();
        lf.addCallback(completionV1::complete, completionV1::error);
        return completionV1;
    }

    public void andAccept(Consumer<ResponseEntity<String>> consumer) {
        this.next = new CompletionV1(consumer);
    }

    private void complete(ResponseEntity<String> success) {
        if (Objects.nonNull(next)) {
            next.run(success);
        }
    }

    private void run(ResponseEntity<String> success) {
        if (Objects.nonNull(consumer)) {
            consumer.accept(success);
        }
    }

    private void error(Throwable failure) {
    }

}
