이번 장부터 자바의 Concurrency Utilities의 하나인 BlockingQueue에 대해서 알아보도록 한다.  
예시에서 사용되는 모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/webflux)의 테스트 코드로 첨부하였으므로 필요한 경우 참고하도록 한다.

---

### Blocking Queue 소개

BlockingQueue의 경우 java.util.concurrent 패키지에 위치하고 있는 인터페이스이다.
인터페이스이기 때문에 아래의 구현체들 중에서 자신의 상황에 맞는 구현체를 찾아서 사용해야한다.

- ArrayBlockingQueue
- DelayQueue
- LinkedBlockingQueue
- LinkedBlockingDeque
- LinkedTransferQueue
- PriorityBlockingQueue
- SynchronousQueue

BlockingQueue는 일반적으로 아래의 그림처럼 한 스레드가 다른 스레드가 소비하는 객체를 생성하도록 하는 데 사용된다.

![](image/blocking-queue.png)

데이터를 BlockingQueue로 넣는 생성 스레드(이하 스레드1)는 BlockingQueue가 더 이상 데이터를 가질 수 없을 때까지 데이터를 집어넣는다.
BlockingQueue가 꽉차서 더 이상 데이터를 넣을 수 없는 상황이 되면 BlockingQueue에서 데이터를 소비하는 스레드(이하 스레드2)가 데이터를 소비할 때까지 차단된다.
스레드2는 BlockingQueue에서 데이터를 처리하기 위해 계속 데이터를 꺼내온다.
스레드2가 빈 큐에서 데이터를 가져오려고 하면 스레드1이 큐에 데이터를 넣을 때까지 스레드2는 차단된다.

---

### Blocking Queue 주요 메서드

BlockingQueue의 구현체들은 아래와 같은 네 가지 세트의 주요 메서드를 가지고 있다.

| | Throws Exception | Special Value | Blocks | Times Out |
| -- | -- | -- | -- |
| Insert | add(o) | offer(o) | put(o) | offer(o, timeout, timeunit) |
| Remove | remove(o) | poll() | take() | poll(timeout, timeunit) |
| Examine | element() | peek() | | |

네 가지 다른 세트는 아래와 같은 역할을 한다.

**1. Throws Exception**: 시도한 작업이 즉시 가능하지 않은 경우 예외가 발생한다.

**2. Special Value**: 시도한 작업이 즉시 가능하지 않은 경우 특수 값이 반환된다.

**3. Blocks**: 시도한 작업이 즉시 가능하지 않은 경우 메서드 호출이 가능해질 때까지 차단된다.

**4. Times Out**: 시도한 작업이 즉시 가능하지 않은 경우 호출이 가능해질 때까지 기다리지만 지정된 타임아웃 시간보다 오래 기다리지 않는다. 작업의 성공 여부를 나타내는 특수 값을 반환한다.

---

### ArrayBlockingQueue

BlockingQueue의 가장 기본적인 쉬운 사용법을 가진 구현체다. 
Blocking Queue의 기본 문법에 대한 기본적인 사용법을 알아보는데 사용할 것이다.

ArrayBlockingQueue의 경우 자바의 Array와 같이 경계가 존재하여 데이터를 무한대로 저장할 수 없고 객체 생성 당시에 지정한 크기만큼만 저장할 수 있다. 







**참고 자료**

- https://jenkov.com/tutorials/java-util-concurrent/blockingqueue.html