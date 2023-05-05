package org.wasend.broker.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wasend.broker.dto.ProducerMessage;
import org.wasend.broker.service.interfaces.MessageService;
import org.wasend.broker.service.mapper.MapperFactory;
import org.wasend.broker.service.model.MessageModel;

@RestController
@RequestMapping(value = "/v1/broker",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class BrokerController {

    private final MessageService messageService;
    private final MapperFactory mapperFactory;

    @PostMapping("/updateReplica")
    // todo тут нужно реализовать механизм принятия сообщений
    public void addReplica(@RequestBody ProducerMessage message) {
        MessageModel model = mapperFactory.mapTo(message, MessageModel.class);
        model.setReplica(true);
        messageService.addMessage(model);
    }
}
