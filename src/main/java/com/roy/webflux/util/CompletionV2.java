package com.roy.webflux.util;

// 비동기 작업을 처리하고 ListenableFuture와 같은 객체를 가져와서 Callback으로 데이터를 넣는 것들을 작업이 있다고 가정해본다.
// 우리가 만드는 `Completion`이라는 클래스는 완료되었을 때 에러가 났을 때 이러한 처리를 다시 한 번 재정의 해주기 위한 클래스라고 보면된다.
// 첫 번째 비동기 작업은 의존성이 앞에 어떤 작업에 의존하지 않기 때문에 파라미터 값 정도를 의존하는 것 말고는 의존성이 없다.
// 두 번째 비동기 작업 같은 경우는 사실 앞에 비동기 작업이 완료 및 성공을 하게 되면 그 때 정보를 받아서 작동하는 의존적인 비동기 작업이다.
// 세 번째 비동기도 두 번째 비동기 작업과 같은 구조라고 볼 수 있다.

// from 메서드에서 Completion을 반환할 때 Callback 메서드를 처리하는 로직을 추가하였다.

// Function의 메서드 중 `compose`는 다른 메서드를 실행할 수 있도록 해준다.
// `andThen`을 현재 Function의 apply을 실행하고 다음 Function의 apply를 실행할 수 있도록 한다.
// andAccept라는 것은 컨슈머 인터페이스를 구현한 람다식을 만들면 Accept하나만 구현하게 된다.
// from에 대한 결과를 두번째 이어서 andAccept에서 Completion 객체를 생성한다.
// 람다식 안에서 결과값을 변경가능하도록 수정이 필요하다.
// andAccept의 경우 다음에 추가적인 작업이 필요없으므로 리턴값은 void로 하였다.

// andAccept는 from의 결과를 andAccept의 파라미터로 들어가있는 람다 식에 전달하여 처리되도록 한다.
// andAccept가 호출될 때 파라미터로 전달받은 Consumer 객체를 통해 새로운 Completion 객체를 생성하고 생성된 객체를 next 변수에 대입한다.
// andAccept를 통해서 하나의 Callback 메서드만 전달하여 호출해본다. (이번 예제 또한 필자의 PC에서 비동기로 작동하지 않는 현상이 발생하여 영상의 출력값으로 대체한다.)
// 출력된 결과는 아래와 같이 2초 정도 시간이 소요되었고 우리가 예상한 결과가 출력된 것을 확인할 수 있다.

// 이번에는 Completion 객체를 추가하여 한 번에 두 개를 Chaining하여 2단 Callback 구조를 Chaining 구조로 변경해본다.
// andApply 메서드를 추가하여 중간에 Callback 메서드를 하나 추가하였다.

// Function이 추가되면서 run 메서드도 변경이 필요해졌다.
// 기존에 consumer의 유무를 확인하고 로직을 실행시켰지만 function이 추가되면서 function이 존재하는 경우에 대한 처리까지 필요해졌다.
// 만약 function이 null이 아니라면 function의 apply를 호출하면 백그라운드에서 API호출과 같은 작업들이 수행되고 작업의 결과가 ListenableFuture로 반환된다.
// 반환된 ListenableFuture는 apply의 결과가 성공인 경우 자기 자신의 complete 메서드를 호출하고 실패한 경우 자기 자신의 error 메서드를 호출한다.
// andApply와 andAccept의 가장 큰 차이는 작업의 완료와 과정의 차이라고 할 수 있다.
// andApply는 자신을 적용한 이후에 다음 Completion 객체를 연결시켜 다음 작업을 진행할 수 있도록 해야한다.
// andAccept는 자신이 호출되면서 작업이 완료되므로 다음 Completion 객체를 연결할 필요가 없다.
// Builder과 비교하면 필드명으로 필드의 값을 입력하여 builder 객체를 반환하는 것은 andApply라고 할 수 있고
// build() 메서드를 호출하여 우리가 생성하고자하는 객체를 생성하는 단계를 andAccept라고 할 수 있다.
// andApply를 추가한 코드를 실행하여 출력되는 결과를 확인해본다.
// 우리가 예상한대로 결과가 출력되는 것을 확인할 수 있다.

// 여기서 CompletionV2 클래스에서 run을 확인해보면 객체가 생성되고 실행되는 시점에 필드의 값을 확인하여 어떠한 용도로 사용되는 객체인지 if문을 사용하여 구분하고 있다.
// 이러한 코드는 리펙토링이 필요한 코드로 판단되며 용도에 따라서 AsyncCompletion과 AcceptCompletion 두 개의 객체로 구분하여 다형성을 적용시켜본다.



import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@NoArgsConstructor
public class CompletionV2 {
    private CompletionV2 next;
    private Consumer<ResponseEntity<String>> consumer;
    public CompletionV2(Consumer<ResponseEntity<String>> consumer) {
        this.consumer = consumer;
    }

    private Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> function;
    public CompletionV2(Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> function) {
        this.function = function;
    }

    public static CompletionV2 from(ListenableFuture<ResponseEntity<String>> lf) {
        CompletionV2 completionV2 = new CompletionV2();
        lf.addCallback(completionV2::complete, completionV2::error);
        return completionV2;
    }

    public void andAccept(Consumer<ResponseEntity<String>> consumer) {
        this.next = new CompletionV2(consumer);
    }

    public CompletionV2 andApply(Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> function) {
        CompletionV2 completionV2 = new CompletionV2(function);
        this.next = completionV2;
        return completionV2;
    }

    private void complete(ResponseEntity<String> success) {
        if (Objects.nonNull(next)) {
            next.run(success);
        }
    }

    private void run(ResponseEntity<String> success) {
        if (Objects.nonNull(consumer)) {
            consumer.accept(success);
        } else if (Objects.nonNull(function)) {
            ListenableFuture<ResponseEntity<String>> lf = function.apply(success);
            lf.addCallback(this::complete, this::error);
        }
    }

    private void error(Throwable failure) {
    }
}
