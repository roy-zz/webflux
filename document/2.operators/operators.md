### Spring WebFlux Series - 2

[이전 장](https://imprint.tistory.com/186) 에서는 Reactive Streams의 핵심 기술인 Observer 패턴과 Pub, Sub 구조에 대해서 알아보았다.
이번 장에서는 Reactive Streams의 Operators에 대해서 알아본다.
모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/webflux)의 테스트 코드에 있으므로 필요하다면 참고하도록 한다.

---

이전 장에서는 java.util.concurrent.Flow.Subscriber와 java.util.concurretn.Flow.Publisher를 사용하여 간단한 Pub, Sub 구조에 대해서 알아보았다.
이번 장에서는 java.reactivestreams의 Subscriber와 java reactivestreams의 Publisher를 사용하여 조금 복잡한 구조를 만들어본다.

java reactivestrams의 Subscriber와 Publisher를 build.gradle에 아래의 의존성을 추가해야 사용가능하다.
버전의 경우 작성일 기준 최신 버전이며 이 글을 보고 따라하는 시점의 최신버전으로 설치하는 것이 좋다.

```bash
implementation("org.springframework:spring-webflux:5.3.18")
```

만약 스프링 부트를 사용하고 있다면 아래와 같은 방식으로 필요한 의존성을 모두 가져올 수 있다.

```bash
implementation("org.springframework.boot:spring-boot-starter-webflux")
```

---

###










Publisher -> [Data1] -> Operation1 -> [Data2] -> Operation2 -> [Data3] -> Subscriber

---

### Map 사용

Publisher -> [Data1] -> mapPublisher -> [Data2] -> Subscriber와 같은 구조로 만들어본다.






---

참고 강의:
- https://www.youtube.com/watch?v=8fenTR3KOJo&ab_channel=TobyLee

참고 문서:
- https://projectreactor.io/docs/core/release/api/ (All Classes -> Flux)
- http://www.reactive-streams.org/
- http://www.reactive-streams.org/reactive-streams-1.0.3-javadoc/org/reactivestreams/package-summary.html
- https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.3/README.md#specification
- https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html
- https://reactivex.io/
- https://grpc.io/
- https://bgpark.tistory.com/160
- https://gunju-ko.github.io/reactive/2018/07/18/Reactive-Streams.html