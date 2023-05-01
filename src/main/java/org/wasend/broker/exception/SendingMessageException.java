package org.wasend.broker.exception;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SendingMessageException extends RuntimeException {
    private final String messageId;
    private final String url;
}
