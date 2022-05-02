package com.roy.webflux.util;

// 마지막으로 V5 버전은 외부 API를 호출하는 코드뿐만 아니라 내부 로직인 MyLogic을 호출하는 부분도 동일한 방법으로 처리한다.
// V5 버전을 적용할 때 컨트롤러 코드는 기존 버전들과 동일하게 andApply만 추가로 연결하면된다.
// 하지만 MyLogic work 메서드의 반환타입이 String이므로 CompletionV5에 제네릭을 적용시켜야 한다.
// 제네릭을 적용시켰다면 결과가 정상적으로 출력되는지 결과를 확인해보도록 한다.
// 우리가 예상한 것과 같이 결과가 출력되는 것을 확인할 수 있다.
// 이전 단계의 Callback 지옥 코드와 비교해보면 상당히 간소화된 코드를 확인할 수 있다.

// 이번 장에서는 Callback 지옥 코드를 해결하는 방법에 대해서 알아보았다.


import org.springframework.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class CompletionV5<S, T> {
    protected CompletionV5 next;

    public void andAccept(Consumer<T> consumer) {
        this.next = new AcceptCompletion<>(consumer);
    }

    public static <S, T> CompletionV5<S, T> from(ListenableFuture<T> lf) {
        CompletionV5<S, T> completionV5 = new CompletionV5<>();
        lf.addCallback(completionV5::complete, completionV5::error);
        return completionV5;
    }

    public CompletionV5<T, T> andError(Consumer<Throwable> errConsumer) {
        CompletionV5<T, T> completionV5 = new ErrorCompletion<>(errConsumer);
        this.next = completionV5;
        return completionV5;
    }

    public <V> CompletionV5<T, V> andApply(Function<T, ListenableFuture<V>> function) {
        CompletionV5<T, V> completionV5 = new ApplyCompletion<>(function);
        this.next = completionV5;
        return completionV5;
    }

    public void run(S value) {}

    protected void complete(T success) {
        if (Objects.nonNull(next)) {
            next.run(success);
        }
    }

    protected void error(Throwable throwable) {
        if (Objects.nonNull(next)) {
            next.error(throwable);
        }
    }

    public static class ErrorCompletion<T> extends CompletionV5<T, T> {
        public Consumer<Throwable> errConsumer;
        public ErrorCompletion(Consumer<Throwable> errConsumer) {
            this.errConsumer = errConsumer;
        }
        @Override
        public void run(T value) {
            if (Objects.nonNull(next)) {
                next.run(value);
            }
        }
        @Override
        public void error(Throwable e) {
            errConsumer.accept(e);
        }
    }

    public static class AcceptCompletion<S> extends CompletionV5<S, Void> {
        private Consumer<S> consumer;
        public AcceptCompletion(Consumer<S> consumer) {
            this.consumer = consumer;
        }
        @Override
        public void run(S value) {
            consumer.accept(value);
        }

    }

    public static class ApplyCompletion<S, T> extends CompletionV5<S, T> {
        private Function<S, ListenableFuture<T>> function;
        public ApplyCompletion(Function<S, ListenableFuture<T>> function) {
            this.function = function;
        }
        @Override
        public void run(S value) {
            ListenableFuture<T> lf = function.apply(value);
            lf.addCallback(this::complete, this::error);
        }
    }
}
