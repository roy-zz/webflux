package com.roy.webflux.flux.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RestController
public class FluxController {

    @GetMapping("/event/v1/{id}")
    private Mono<Event> fluxVersion1(@PathVariable long id) {
        return Mono.just(new Event(id, String.format("Event %s", id)));
    }

    @GetMapping("/event/v2")
    private Flux<Event> fluxVersion2() {
        return Flux.just(new Event(1L, "Event 1"), new Event(2L, "Event 2"));
    }

    @GetMapping("/event/v3")
    private Mono<List<Event>> fluxVersion3() {
        List<Event> response = List.of(
                new Event(1L,"Event 1"),
                new Event(2L, "Event 2"));
        response = response.stream().peek(i -> i.id++).collect(Collectors.toList());
        return Mono.just(response);
    }

    @GetMapping("/event/v4")
    private Flux<Event> fluxVersion4() {
        List<Event> response = List.of(
                new Event(1L,"Event 1"),
                new Event(2L, "Event 2"));
        return Flux.fromIterable(response).map(i -> {
            i.id++;
            return i;
        });
    }

    @GetMapping("/event/v5")
    private Mono<List<Event>> fluxVersion5() {
        List<Event> response = List.of(
                new Event(1L,"Event 1"),
                new Event(2L, "Event 2"));
//        return Mono.just(response).map(i -> {
//            i.id++;
//            return i;
//        });
        return null;
    }

    @GetMapping(value = "/event/v6", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    private Flux<Event> fluxVersion6() {
        return Flux.just(new Event(1L, "Event 1"), new Event(2L, "Event 2"));
    }

    @GetMapping(value = "/event/v7", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    private Flux<Event> fluxVersion7() {
        return Flux.fromStream(Stream.generate(() -> new Event(System.currentTimeMillis(), "Value")))
                .delayElements(Duration.ofSeconds(1))
                .take(10);
    }

    @GetMapping(value = "/event/v8", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    private Flux<Event> fluxVersion8() {
        return Flux
                .<Event> generate(sink -> sink.next(new Event(System.currentTimeMillis(), "Value")))
                .delayElements(Duration.ofSeconds(1))
                .take(10);
    }

    @GetMapping(value = "/event/v9", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    private Flux<Event> fluxVersion9() {
        return Flux
                .<Event, Long>generate(() -> 1L, (id, sink) -> {
                    sink.next(new Event(id, String.format("Value %s", id)));
                    return ++id;
                })
                .delayElements(Duration.ofSeconds(1))
                .take(10);
    }

    @GetMapping(value = "/event/v10", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    private Flux<Event> fluxVersion10() {
        Flux<Event> events = Flux.<Event, Long>generate(() -> 1L, (id, sink) -> {
            sink.next(new Event(id, String.format("Value %s", id)));
            return ++id;
        });
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));

        return Flux.zip(events, interval).map(Tuple2::getT1).take(10);
    }

    @Data
    @AllArgsConstructor
    public static class Event {
        long id;
        String value;
    }

}
