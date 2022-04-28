package com.roy.webflux.mvc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mvc")
public class MvcController {
    @GetMapping(value = "/rest")
    public String rest() {
        return "HELLO";
    }
}
