package org.wasend.broker.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wasend.broker.dto.ProducerMessage;

@RestController
@RequestMapping(value = "/v1/producer",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public class BrokerController {

    @PostMapping("/updateReplica")
    // todo тут нужно реализовать механизм принятия сообщений
    public void addReplica(@RequestBody ProducerMessage message) {

    }
}
