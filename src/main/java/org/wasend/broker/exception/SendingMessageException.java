package org.wasend.broker.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SendingMessageException extends RuntimeException {
    private final String messageId;
    private final String url;
}
