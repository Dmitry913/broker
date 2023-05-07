package org.wasend.broker.service.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public class Message implements Serializable {
    private final String id;

    public Message() {
        id = UUID.randomUUID().toString();
    }
}
