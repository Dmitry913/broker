package org.wasend.broker.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wasend.broker.dto.BrokerMessage;
import org.wasend.broker.dto.ConsumerMessageRegistry;
import org.wasend.broker.service.interfaces.MessageService;
import org.wasend.broker.service.mapper.MapperFactory;
import org.wasend.broker.service.model.MessageModel;
import org.wasend.broker.service.model.RegistryModel;

@RestController
@RequestMapping(value = "/v1/broker",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class BrokerController {

    private final MessageService messageService;
    private final MapperFactory mapperFactory;

    @PostMapping("/addReplica")
    public void addReplicaMessage(@RequestBody BrokerMessage message) {
        MessageModel model = mapperFactory.mapTo(message, BrokerMessage.class, MessageModel.class);
        model.setReplica(true);
        messageService.addMessage(model);
    }

    @PostMapping("/addRegistry")
    public void addRegistry(@RequestBody ConsumerMessageRegistry messageRegistry) {
        RegistryModel model = mapperFactory.mapTo(messageRegistry, ConsumerMessageRegistry.class, RegistryModel.class);
        model.setReplica(true);
        messageService.registry(model);
    }

    @PostMapping("/addMessage")
    // Отдельный метод для брокера, т.к. возможно будет разное логирование + чтобы не загружать тот класс
    public void addMasterMessage(@RequestBody BrokerMessage message) {
        messageService.addMessage(mapperFactory.mapTo(message, BrokerMessage.class, MessageModel.class));
    }

    @GetMapping("/countMessages")
    public Integer countMessage() {
        return messageService.getCountMessage();
    }
}
