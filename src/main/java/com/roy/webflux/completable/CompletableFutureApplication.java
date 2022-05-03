package com.roy.webflux.completable;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;


@Slf4j
public class CompletableFutureApplication {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // CompletableFutureApplication.completableFutureWithThread();
        // CompletableFutureApplication.completableFutureWithThenRunAsync();
        // CompletableFutureApplication.completableFutureWithSupply();
        // CompletableFutureApplication.completableFutureWithThenCompose();
        // CompletableFutureApplication.completableFutureWithExceptionally();
        CompletableFutureApplication.completableFutureWithMultiThread();
    }

    private static void completableFutureTest() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> cf = CompletableFuture.completedFuture(1);
        System.out.println("cf.get() = " + cf.get());
    }

    private static void completableFutureWithThread() throws InterruptedException {
        CompletableFuture.runAsync(() -> log.info("runAsync"));
        log.info("exit");
        ForkJoinPool.commonPool().shutdown();
        ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
    }

    private static void completableFutureWithThenRunAsync() {
        CompletableFuture
                .runAsync(() -> log.info("runAsync"))
                .thenRun(() -> log.info("thenRun"))
                .thenRun(() -> log.info("thenRun"));
        log.info("exit");
    }

    private static void completableFutureWithSupply() throws ExecutionException, InterruptedException {
        CompletableFuture
                .supplyAsync(() -> {
                    log.info("runAsync");
                    return 1;
                })
                .thenApply(s -> {
                    log.info("thenApply {}", s);
                    return s + 1;
                })
                .thenAccept(s -> log.info("thenAccept {}", s));
        log.info("exit");
        ForkJoinPool.commonPool().shutdown();
        ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
    }

    private static void completableFutureWithThenCompose() throws InterruptedException {
        CompletableFuture
                .supplyAsync(() -> {
                    log.info("runAsync");
                    return 1;
                })
                .thenCompose(s -> {
                    log.info("thenCompose {}", s);
                    return CompletableFuture.completedFuture(s + 1);
                })
                .thenAccept(s -> log.info("thenAccept {}", s));
        log.info("exit");
        ForkJoinPool.commonPool().shutdown();
        ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
    }

    private static void completableFutureWithExceptionally() throws InterruptedException {
        CompletableFuture
                .supplyAsync(() -> {
                    log.info("runAsync");
                    if (true) throw new RuntimeException();
                    return 1;
                })
                .thenCompose(s -> {
                    log.info("thenCompose {}", s);
                    return CompletableFuture.completedFuture(s + 1);
                })
                .thenApply(s -> {
                    log.info("thenApply {}", s);
                    return s + 2;
                })
                .exceptionally(e -> -10)
                .thenAccept(s -> log.info("thenAccept {}", s));
        log.info("exit");
        ForkJoinPool.commonPool().shutdown();
        ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
    }

    private static void completableFutureWithMultiThread() throws InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(10);
        CompletableFuture
                .supplyAsync(() -> {
                    log.info("runAsync");
                    return 1;
                }, es)
                .thenCompose(s -> {
                    log.info("thenCompose {}", s);
                    return CompletableFuture.completedFuture(s + 1);
                })
                .thenApplyAsync(s -> {
                    log.info("thenApply {}", s);
                    return s + 2;
                }, es)
                .exceptionally(e -> -10)
                .thenAcceptAsync(s -> log.info("thenAccept {}", s), es);
        log.info("exit");
        ForkJoinPool.commonPool().shutdown();
        ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
    }
}
