package org.wasend.broker.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wasend.broker.dto.ConsumerMessageRegistry;
import org.wasend.broker.service.interfaces.MessageService;
import org.wasend.broker.service.mapper.MapperFactory;
import org.wasend.broker.service.model.RegistryModel;

@RestController
@RequestMapping("/v1/consumer")
@RequiredArgsConstructor
public class ConsumerController {

    private final MessageService service;
    private final MapperFactory mapperFactory;

    @PostMapping("/registry")
    public void registry(@RequestBody ConsumerMessageRegistry message) {
        service.registry(mapperFactory.mapTo(message, RegistryModel.class));
    }
}
