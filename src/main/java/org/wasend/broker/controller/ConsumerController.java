package org.wasend.broker.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wasend.broker.dto.ConsumerMessage;
import org.wasend.broker.service.interfaces.MessageService;

@RestController
@RequestMapping("/v1/consumer")
@RequiredArgsConstructor
public class ConsumerController {

    private final MessageService service;

    @PostMapping("/registry")
    public void registry(@RequestBody ConsumerMessage message) {
        service.registry(message);
    }
}
