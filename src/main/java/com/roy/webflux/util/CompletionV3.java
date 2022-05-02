package com.roy.webflux.util;

// CompletionV3 클래스에 CompletionV2를 상속받는 AcceptCompletion과 AsyncCompletion을 구현하였다.
// 다형성 적용이 필요한 run 메서드를 아래와 같이 수정하였다.
// 코드를 살펴보면 기존에 하나의 run 메서드에서 분기처리가 되던 코드를 두 개의 클래스로 나누어 다형성을 적용시킨 것을 확인할 수 있다.
// V3는 기능적으로 변경된 부분은 없으며 공통된 부분은 하나로 뽑아내고 다형성을 적용시킬 수 있는 부분에 다형성을 적용시켜 쉽게 확장시킬 수 있도록 변경하였다.
// 출력 결과는 아래와 같으며 우리가 예상한대로 작동한 것을 확인할 수 있다.

import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@NoArgsConstructor
public class CompletionV3 {
    private CompletionV3 next;

    public static CompletionV3 from(ListenableFuture<ResponseEntity<String>> lf) {
        CompletionV3 completionV3 = new CompletionV3();
        lf.addCallback(completionV3::complete, completionV3::error);
        return completionV3;
    }

    public void andAccept(Consumer<ResponseEntity<String>> consumer) {
        this.next = new AcceptCompletion(consumer);
    }

    public CompletionV3 andApply(Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> function) {
        CompletionV3 completionV3 = new ApplyCompletion(function);
        this.next = completionV3;
        return completionV3;
    }

    public void run(ResponseEntity<String> value) {}

    protected void complete(ResponseEntity<String> success) {
        if (Objects.nonNull(next)) {
            next.run(success);
        }
    }

    protected void error(Throwable failure) {}

    public static class AcceptCompletion extends CompletionV3 {
        private Consumer<ResponseEntity<String>> consumer;
        public AcceptCompletion(Consumer<ResponseEntity<String>> consumer) {
            this.consumer = consumer;
        }
        @Override
        public void run(ResponseEntity<String> value) {
            consumer.accept(value);
        }

    }

    public static class ApplyCompletion extends CompletionV3 {
        private Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> function;
        public ApplyCompletion(Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> function) {
            this.function = function;
        }
        @Override
        public void run(ResponseEntity<String> value) {
            ListenableFuture<ResponseEntity<String>> lf = function.apply(value);
            lf.addCallback(this::complete, this::error);
        }
    }

}
