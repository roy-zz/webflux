package com.roy.webflux.reactivestream.asyncspring;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
class AsyncJavaSpringTest1 {

    // 아래의 코드와 같이 실행하게 되면 HELLO와 EXIT는 어떻게 출력될까
    // 애플리케이션이 실행되고 2초 후에 한번에 HELLO와 EXIT가 출력될 것이다.
    static class OnlyMainThreadTest {
        public static void main(String[] args) throws InterruptedException {
            log.info("Start Time: {}", LocalDateTime.now());
            TimeUnit.SECONDS.sleep(2);
            log.info("HELLO, Time: {}", LocalDateTime.now());
            log.info("EXIT, Time: {}", LocalDateTime.now());
            log.info("End Time: {}", LocalDateTime.now());
        }
    }

    // 우리는 "EXIT"는 바로 출력되고 "HELLO"만 2초 후에 출력되기를 바란다.
    // 아래와 같이 스레드를 "sleep"하는 코드와 `HELLO`를 출력하는 코드를 ExecutorService를 통해서 다른 스레드에서 실행시킨다.
    // 출력된 결과를 확인해보면 애플리케이션 실행시점에 `EXIT`가 출력되었고 2초 후에 `HELLO`가 출력되었다.
    // 스레드의 `sleep`을 사용할 때 InterruptedException을 따로 처리해야하는
    // 이유는 스레드가 `sleep`인 상태에서 Owner 스레드가 실행 중인 스레드를 종료 및 인터럽트 하는 경우 `InterruptedException`이 발생하기 때문이다.
    static class UseOtherThreadTest {
        public static void main(String[] args) throws InterruptedException {
            ExecutorService es = Executors.newCachedThreadPool();
            log.info("Start Time: {}", LocalDateTime.now());
            es.execute(() -> {
                try {
                    TimeUnit.SECONDS.sleep(2);
                    log.info("HELLO, Time: {}", LocalDateTime.now());
                } catch (InterruptedException e) {}
            });
            log.info("EXIT, Time: {}", LocalDateTime.now());
            log.info("End Time: {}", LocalDateTime.now());
        }
    }

    // 이번에는 다른 스레드에서 수행되는 로직이 2초 후에 `HELLO`라는 문자열을 출력하도록 구현해본다.
    // Callable은 리턴 값이 있지만 Runnable은 리턴 값이 없다.
    // Return 값이 있다는 것은 메인 스레드에서 다른 스레드의 Return값을 사용하겠다는 의미가 되며 예외가 발생하는 경우 예외를 다른 스레드로 throw 할 수도 있다.
    // Return 값을 바로 사용할 수 있는 것은 아니고 우리가 계속 언급하였던 Future 객체를 사용하여 받을 수 있다.
    // 이러한 경우 `execute`메서드가 아닌 `submit`메서드를 사용한다.

    // 출력되는 순서를 보면 이전에 다른 스레드에서 HELLO를 출력한 것과는 결과가 다르다는 점이다.
    // 결국 메인 스레드는 Future의 get 메서드를 호출하면서 다른 스레드의 결과를 가져올 때까지 **블로킹**되어 있었다는 의미가 된다.
    // 만약 Future의 get 메서드가 메인 스레드를 중지시키지 않고 null과 같은 값을 일단 반환한다면 **넌블러킹** 방식이 된다.
    static class UseOtherThreadWithReturnTest {
        public static void main(String[] args) throws ExecutionException, InterruptedException {
            ExecutorService es = Executors.newCachedThreadPool();
            log.info("Start Time: {}", LocalDateTime.now());
            Future<String> future = es.submit(() -> {
                TimeUnit.SECONDS.sleep(2);
                log.info("Async, Time: {}", LocalDateTime.now());
                return "HELLO";
            });
            log.info("Future: {}, Time: {}", future.get(), LocalDateTime.now());
            log.info("EXIT, Time: {}", LocalDateTime.now());
        }
    }

    // 만약 우리의 메인 스레드가 HELLO를 출력하는 스레드와 동시에 작동하는 상황이라고 가정하고 `EXIT`를 출력하는 코드와 Futere를 get하는 코드의 위치를 바꿔본다.
    // 결과는 애플리케이션 실행과 동시에 `EXIT`가 실행되고 2초 후에 `HELLO`가 출력되었다.
    // 다른 스레드에서 2초 동안 진행되는 작업과 메인 스레드에서의 작업이 동시에 진행되었음을 알 수 있다.
    static class ManyThreadParallelTest {
        public static void main(String[] args) throws ExecutionException, InterruptedException {
            log.info("Start");
            ExecutorService es = Executors.newCachedThreadPool();
            Future<String> future1 = es.submit(() -> {
                TimeUnit.SECONDS.sleep(2);
                log.info("Async-1");
                return "HELLO-1";
            });
            Future<String> future2 = es.submit(() -> {
                TimeUnit.SECONDS.sleep(2);
                log.info("Async-2");
                return "HELLO-2";
            });
            Future<String> future3 = es.submit(() -> {
                TimeUnit.SECONDS.sleep(2);
                log.info("Async-3");
                return "HELLO-3";
            });
            log.info("EXIT");
            log.info("Future-1: {}", future1.get());
            log.info("Future-2: {}", future2.get());
            log.info("Future-3: {}", future3.get());
            log.info("End");
        }
    }

    // Future의 `isDone()` 메서드는 작업 진행여부를 확인한다.
    // `get()` 메서드와는 다르게 블로킹 시키지 않기 때문에 작업이 끝났는지 확인을 해보고 작업이 끝나지 않은 경우 다른 작업을 진행하고 돌아와서 다시 작업 진행유무를 확인할 수 있다.
    // 예시의 출력을 보면 첫번째 isDone은 작업이 끝나지 않았으므로 false를 반환하고 두번째 isDone은 작업을 끝났음을 의미하는 true를 반환한다.
    // 결국 Future는 결과를 얻어올 수 있는 핸들러와 같은 역할을 하는 것이지 결과 자체는 아니다.
    static class IsDoneTest {
        public static void main(String[] args) throws InterruptedException, ExecutionException {
            ExecutorService es = Executors.newCachedThreadPool();
            Future<String> future = es.submit(() -> {
                TimeUnit.SECONDS.sleep(2);
                log.info("Async");
                return "HELLO";
            });
            log.info("Future.isDone - 1: {}", future.isDone());
            TimeUnit.SECONDS.sleep(3);
            log.info("EXIT");
            log.info("Future.isDone - 2: {}", future.isDone());
            log.info("Future.get: {}", future.get());
        }
    }

    // Future와 비슷한 방식으로 Javascript에서 많이 사용하는 Callback이 있으며 비동기 프로그래밍을 구현할 때 많이 사용되는 방식이다.
    // 자바에서는 가장 기본적으로 Future를 사용하거나 Callback을 사용한다.
    // 자바 1.8 이전에는 Future가 대표적으로 사용되었으나 결과를 가져올 때(get을 호출할 때) 블로킹된다는 사실에 많은 개발자들이 당황했다.
    // 차라리 객체 자체를 전달하고 작업이 완료되면 메서드를 호출해달라고 요청하는 것이 훨씬 익숙한 패턴(커맨드패턴)이다.
    // 지금부터 Future 자체를 Object로 만들어서 Callback과 유사하게 만들어본다.
    // FutureTask는 Future를 구현체며 위에서 살펴본 것과 동일한 결과를 출력한다.

    static class FutureTaskTest {
        public static void main(String[] args) throws InterruptedException, ExecutionException {
            ExecutorService es = Executors.newCachedThreadPool();
            FutureTask<String> futureTask = new FutureTask<String>(() -> {
                TimeUnit.SECONDS.sleep(2);
                log.info("Async");
                return "HELLO";
            });
            es.execute(futureTask);
            log.info("Future.isDone - 1: {}", futureTask.isDone());
            TimeUnit.SECONDS.sleep(3);
            log.info("EXIT");
            log.info("Future.isDone - 2: {}", futureTask.isDone());
            log.info("Future.get: {}", futureTask.get());
        }
    }

    // ExecutorService.shutdown은 진행 중인 작업이 끝나면 스레드를 종료(Graceful하게)하라는 의미이며 비동기 작업 자체가 종료되지는 않는다.
    // 이번에는 FutureTask를 익명 클래스로 만들어본다.
    // FutureTask의 done 메서드는 작업이 모두 완료되면 호출되는 hook같은 존재다.
    // 우리는 결과를 따로 출력하지 않았지만 done에 의해 HELLO가 출력되는 것을 볼 수 있다.
    // FutureTask 또한 Future와 동일하게 자바 1.5부터 추가되어 있었으며 Callback과 같이 사용할 수 있는 기법들이 이미 구현되어 있다.
    // 물론 매번 오버라이딩하여 구현해야한다는 점에서 자연스러운 Callback이라고 보기는 어렵다.
    static class AnonymousFutureTaskTest {
        public static void main(String[] args) {
            ExecutorService es = Executors.newCachedThreadPool();
            FutureTask<String> futureTask = new FutureTask<String>(() -> {
                TimeUnit.SECONDS.sleep(2);
                log.info("Async");
                return "HELLO";
            }) {
                @Override
                protected void done() {
                    try {
                        log.info("Call done: {}", get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            };
            es.execute(futureTask);
            es.shutdown();
        }
    }

    // 이번에는 FutureTask를 사용하여 자연스러운 Callback이 되도록 CallbackFutureTask 메서드를 작성해본다.
    // Objects의 requireNonNull은 검증하는 대상이 null인 경우 즉시 NullPointerException이 발생하기 때문에 null을 방어하기 위해 많이 사용된다.
    // Callback 인터페이스인 SuccessCallback을 생성한다.
    // 이렇게 생성하는 방식은 자바 1.8 이전까지는 불가능했다.
    interface SuccessCallback {
        void onSuccess(String result);
    }

    static class CallbackFutureTask extends FutureTask<String> {
        private final SuccessCallback successCallback;
        public CallbackFutureTask(Callable<String> callable, SuccessCallback successCallback) {
            super(callable);
            this.successCallback = Objects.requireNonNull(successCallback);
        }
        @Override
        protected void done() {
            try {
                successCallback.onSuccess(get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    static class CallbackFutureTaskTest {
        public static void main(String[] args) {
            ExecutorService es = Executors.newCachedThreadPool();
            CallbackFutureTask callbackFutureTask = new CallbackFutureTask(() -> {
                TimeUnit.SECONDS.sleep(2);
                log.info("Async");
                return "HELLO";
            }, result -> {
                log.info("SuccessCallback.onSuccess: {}", result);
            });
            es.execute(callbackFutureTask);
            es.shutdown();
        }
    }

    // 우리는 이전 예제에서 CallbackFutureTask를 생성하여 Callback 형식으로 비동기로 작업을 처리하는 방법에 대해서 알아보았다.
    // 하지만 원하는 결과만 얻어올 뿐 예외가 발생했을 때에 대한 예외처리는 따로 작성하지 않았다. 다른 스레드에서 발생한 예외를 우리는 메인 스레드로 끌어와야한다.
    // 최근에는 try catch를 사용하는 것보다 Exception Object를 Exception을 처리하는 Callback에 던져주는 방식으로 많이 사용된다.
    // 이번에는 예외를 처리하는 ExceptionCallback을 만들고 CallbackFutureTask가 ExceptionCallback을 생성자 파라미터로 받을 수 있도록 변경해본다.
    interface ExceptionCallback {
        void onError(Throwable t);
    }

    // FutureTask의 get을 호출할 때 InterruptedException과 ExecutionException을 처리해야한다.
    // InterruptedException의 경우 다른 Exception들과는 성향이 다르다.
    // 단순히 Interrupt 메시지를 받아도 발생하는 예외이기 때문에 스레드에 다시 인터럽트를 걸어서 처리해도 무관하다.
    // ExecutionException은 비동기 작업을 수행하다가 예외 상황이 생겼을 때 발생하며 우리가 처리해줘야한다.
    // 발생한 예외를 ExceptionCallback에게 전달할 때 바로 ExecutionException은 한 번 가공된 예외이기 때문에 getCause를 사용하여 정확한 원인을 전달해야한다.
    static class CallbackFutureHandleExceptionTask extends FutureTask<String> {
        private final SuccessCallback successCallback;
        private final ExceptionCallback exceptionCallback;
        public CallbackFutureHandleExceptionTask(Callable<String> callable,
                                                 SuccessCallback successCallback,
                                                 ExceptionCallback exceptionCallback) {
            super(callable);
            this.successCallback = Objects.requireNonNull(successCallback);
            this.exceptionCallback = Objects.requireNonNull(exceptionCallback);
        }
        @Override
        protected void done() {
            try {
                successCallback.onSuccess(get());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ee) {
                exceptionCallback.onError(ee.getCause());
            }
        }
    }

    static class CallbackFutureHandleExceptionTaskTest {
        public static void main(String[] args) {
            ExecutorService es = Executors.newCachedThreadPool();
            CallbackFutureHandleExceptionTask task = new CallbackFutureHandleExceptionTask(() -> {
                TimeUnit.SECONDS.sleep(2);
                if (true) throw new RuntimeException("Async Error Occurred");
                log.info("Async");
                return "HELLO";
            },
                    result -> log.info("onSuccess: {}", result),
                    error -> log.error("onError: {}", error.getMessage()));
            es.execute(task);
            es.shutdown();
        }
    }

    // 지금까지 우리는 Future에 대해서 알아보았다.
    // 정리해보면 비동기 작업의 결과를 가져오는 방법은 두가지가 있다.
    // 첫번째로 Future와 같이 작업의 결과를 담고 있는 핸들러를 리턴받아서 처리하는 방식이 있다. get메서드를 호출할 때 블로킹이 발생하며 예외가 발생하는 경우는 try catch를 사용하여 해결해야한다.
    // 두번째로 Callback을 구현하여 사용하는 방법이 있다.

    // 자바에서는 어떻게 비동기를 구현하고 있는지 AsyncchronousByChannel코드를 확인해 본다.
    // read 메서드를 확인해보면 CompletionHandler를 전달하게 해준다.
    // CompletionHandler 내부에는 completed와 failed 두 개의 메서드가 있으며 지금까지 우리가 구현한 방식과 동일하다.

    // AsyncchronousByChannel 메서드 중 결과값이 Future<Integer>인 메서드도 있다.
    // 위에서 살펴본 것과 같이 Future는 바로 반환이 되지만 get을 호출하는 순간 스레드는 블로킹된다.

    // 지금까지 우리가 만들었던 로직을 살펴보면 비동기를 위한 로직과 서비스를 위한 로직이 동일한 위치에 존재하며 스프링이 지향하는 AOP와는 거리가 먼 코드가 작성되는 문제가 있다.
    // 다음 장에서는 이러한 문제를 스프링의 옛 기술(15년 전)을 사용하여 해결해본다.
    // 스프링은 이러한 문제를 @Async라는 애노테이션을 사용하여 해결한다.

}
