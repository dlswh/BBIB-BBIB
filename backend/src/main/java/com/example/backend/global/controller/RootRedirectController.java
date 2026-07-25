package com.example.backend.global.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;

@Controller
public class RootRedirectController {

    @GetMapping({"", "/"})
    public ResponseEntity<Void> redirectToHealth() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/api/actuator/health"))
                .build();
    }
}
