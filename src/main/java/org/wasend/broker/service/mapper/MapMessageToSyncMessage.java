package org.wasend.broker.service.mapper;

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.wasend.broker.service.model.Message;
import org.wasend.broker.service.model.SyncMessage;
import org.wasend.broker.service.model.SyncMessageType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

@Mapper(componentModel = "spring")
@Slf4j
public abstract class MapMessageToSyncMessage implements DefaultMapper<SyncMessage, Message> {

    @Override
    public SyncMessage mapTo(Message e) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        SyncMessage.SyncMessageBuilder builder = SyncMessage.builder()
                .type(SyncMessageType.valueOf(e.getClass().getName()));
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
            objectOutputStream.writeObject(e);
            builder.payloadMessage(byteStream.toByteArray());
        } catch (IOException exception) {
            log.error("Error occurred while {} serialized", e);
            throw new RuntimeException(exception);
        }
        return builder.build();
    }
}
