[이전 장(링크)](https://imprint.tistory.com/241) 에서는 `AsyncRestTemplate`과 `DeferredResult`를 통해서 외부 API를 호출할 때 발생하는 블록킹 문제를 해결하였다.
이번 장에서는 블록킹 문제를 해결하면서 등장한 `Callback Hell`이라는 문제를 해결해보도록 한다.
모든 코드는 [깃 허브 (링크)](https://github.com/roy-zz/webflux)의 테스트 코드에 있으므로 필요하다면 참고하도록 한다.

---

### 개요

이번 장에서는 이전에 작성한 코드에 새로운 기능을 추가하는 것이 아니라 `Callback Hell`을 해결하기 위하여 코드를 리펙토링 하는 시간을 가져볼 것이다.

```java
@GetMapping("/my-service/async-complex-with-logic/{idx}")
public DeferredResult<String> complexAsyncWithService(@PathVariable String idx) {
    DeferredResult<String> deferredResult = new DeferredResult<>();

    ListenableFuture<ResponseEntity<String>> future1 = nettyRestTemplate.getForEntity(
            "http://localhost:8081/remote-service-1/{request}", String.class, idx);
    future1.addCallback(success -> {
        ListenableFuture<ResponseEntity<String>> future2 = nettyRestTemplate.getForEntity(
                "http://localhost:8081/remote-service-2/{request}", String.class, Objects.requireNonNull(success).getBody());
        future2.addCallback(success2 -> {
            ListenableFuture<String> future3 = myLogic.work(Objects.requireNonNull(success2).getBody());
            future3.addCallback(success3 -> {
                deferredResult.setResult(success3);
            }, ex3 -> {
                deferredResult.setErrorResult(ex3.getMessage());
            });
        }, ex2 -> {
            deferredResult.setErrorResult(ex2.getMessage());
        });
    }, ex -> {
        deferredResult.setErrorResult(ex.getMessage());
    });
    return deferredResult;
}
```

우리는 이전에 요청마다 2초의 처리시간이 걸리는 두 개의 API를 호출하고 내부 서비스 로직까지 실행시켜서 반납하는 API를 생성하였다.
코드를 작성하는 과정에서 아래와 같이 연쇄적으로 작동하는 코드를 만들게 되었다.

`첫번째 API를 호출하고 성공` -> `두번째 API를 호출하고 성공` -> `서비스 로직 실행`

로직만 보면 간단해보일 수 있지만 실제로 코드를 살펴보면 무한 Callback 형식의 지저분한 코드라고 볼 수 있다.
또한 한 번의 Callback이 추가될 때마다 똑같은 형태의 예외처리 코드가 중복되고 있다.

우리가 기존에 작성한 코드를 확인해보면 Callback을 활용한 `명령형 코드`다. 
`함수형 코드`로 작성하면 우리가 작성한 코드와 다르게 복잡하지 않게 구현이 가능하다. 
자바8 이상을 사용하고 있다면 깔끔하게 정리가 가능하지만 자바8의 문법을 적용하기 전에 우리의 힘으로 코드를 작성하여 `Callback Hell`을 해결해본다.

---

### Refactoring

우리가 작성한 코드의 근본적인 문제는 우리가 결과값을 바로 쓸 수 있는 구조가 아니라 결과값을 가져올 수 있는 중개자 역할의 객체를 가져와서 `Callback 메서드`를 장착하는 구조라는 점이다. 
이번에는 비동기 작업 실행의 모든 과정을 담고 있는 클래스를 하나 만들어서 `Callback` 형식이 아니라 `Chaining` 형식으로 바뀔 수 있도록 구현해본다.










---

**참고한 강의**

- https://www.youtube.com/watch?v=Tb43EyWTSlQ&ab_channel=TobyLee