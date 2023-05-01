package org.wasend.broker.service.model;

import lombok.Builder;

/**
 * Сообщение для синхронизации с другими брокерами.
 */
@Builder
public class SyncMessage extends Message {
    private SyncMessageType type;
    private byte[] payloadMessage;
}
