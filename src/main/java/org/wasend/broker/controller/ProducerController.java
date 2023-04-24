package org.wasend.broker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wasend.broker.dto.ProducerMessage;
import org.springframework.http.MediaType;

@RestController
@RequestMapping(value = "/v1/producer",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class ProducerController {

    @PostMapping("/send")
    public void sendMessage(@RequestBody ProducerMessage message) {

    }

}
