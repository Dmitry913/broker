package org.wasend.broker.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wasend.broker.dto.ProducerMessage;
import org.springframework.http.MediaType;
import org.wasend.broker.service.interfaces.MessageService;
import org.wasend.broker.service.mapper.MapperFactory;
import org.wasend.broker.service.model.MessageModel;

@RestController
@RequestMapping(value = "/v1/producer",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
public class ProducerController {

    private final MessageService service;
    private final MapperFactory mapperFactory;

    @PostMapping("/send")
    // todo нужно ли тут что-то возвращать??
    // todo добавить проверку на то, что указанный дедлайн у сообщения должен быть больше n секунд с текущего момента
    public void sendMessage(@RequestBody ProducerMessage message) {
        service.addMessage(mapperFactory.mapTo(message, MessageModel.class));
    }

}
