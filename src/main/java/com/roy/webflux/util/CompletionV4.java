package com.roy.webflux.util;

// 다음 V4 버전에서는 에러 처리가 중복으로 표시되는 문제를 해결해본다.
// 여기서 에러 처리가 중복된다는 것은 아래와 비동기 처리가 진행될 때 마다 동일한 에러처리 코드가 계속 중복되어 사용된다는 점이다.
// 에러 처리는 전체의 흐름에서 단 한번만 처리하도록 V4 버전의 클래스 코드를 생성해본다.

// CompletionV4를 사용하는 코드를 확인해보면 1번 비동기 작업인 from이 있고 2번 비동기 작업인 andApply가 있다.
// 우리는 두개 중 하나에서 오류가 발생하더라도 andError의 매개변수로 입력된 function이 실행되도록 구현할 것이다.
// 또한 예외가 발생하는 경우 이후의 작업인 andAccept는 실행되지 않아야하며 예외가 발생하지 않는 경우 정상적으로 andAccept가 호출되어야 한다.

// CompletionV4의 onError 메서드를 살펴보면 계속 next 객체의 onError 메서드로 예외를 전달한다.
// 그러다가 ErrorCompletion 객체를 만나면 @Override된 onError 메서드가 호출되어 더 이상 에러가 전달되지 않고 처리되도록 구현되었다.
// 이제 정상적으로 우리가 예상하는대로 코드가 작동하는지 테스트를 진행해본다.
// 먼저 에러가 발생하지 않는 경우에 출력을 확인해보면 onError가 호출되지 않고 정상적으로 처리가 완료된 것을 확인할 수 있다.

// 다음으로 from 부분에서 에러가 나도록 코드를 수정하고 andAccept가 호출되지 않는 것을 확인해본다.
// 출력 결과를 확인해보면 우리의 예상과 같이 andAccept가 호출되지 않고 에러 메시지가 반환되는 것을 확인할 수 있다.


import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class CompletionV4 {
    protected CompletionV4 next;

    public static CompletionV4 from(ListenableFuture<ResponseEntity<String>> lf) {
        CompletionV4 completionV4 = new CompletionV4();
        lf.addCallback(completionV4::complete, completionV4::error);
        return completionV4;
    }

    public CompletionV4 andError(Consumer<Throwable> errConsumer) {
        CompletionV4 completionV4 = new ErrorCompletion(errConsumer);
        this.next = completionV4;
        return completionV4;
    }

    public void andAccept(Consumer<ResponseEntity<String>> consumer) {
        this.next = new CompletionV4.AcceptCompletion(consumer);
    }

    public CompletionV4 andApply(Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> function) {
        CompletionV4 completionV4 = new CompletionV4.ApplyCompletion(function);
        this.next = completionV4;
        return completionV4;
    }

    public void run(ResponseEntity<String> value) {}

    protected void complete(ResponseEntity<String> success) {
        if (Objects.nonNull(next)) {
            next.run(success);
        }
    }

    protected void error(Throwable throwable) {
        if (Objects.nonNull(next)) {
            next.error(throwable);
        }
    }

    public static class ErrorCompletion extends CompletionV4 {
        public Consumer<Throwable> errConsumer;
        public ErrorCompletion(Consumer<Throwable> errConsumer) {
            this.errConsumer = errConsumer;
        }
        @Override
        public void run(ResponseEntity<String> value) {
            if (Objects.nonNull(next)) {
                next.run(value);
            }
        }
        @Override
        public void error(Throwable e) {
            errConsumer.accept(e);
        }
    }

    public static class AcceptCompletion extends CompletionV4 {
        private Consumer<ResponseEntity<String>> consumer;
        public AcceptCompletion(Consumer<ResponseEntity<String>> consumer) {
            this.consumer = consumer;
        }
        @Override
        public void run(ResponseEntity<String> value) {
            consumer.accept(value);
        }

    }

    public static class ApplyCompletion extends CompletionV4 {
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
