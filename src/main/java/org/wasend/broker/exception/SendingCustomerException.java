package org.wasend.broker.exception;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SendingCustomerException extends RuntimeException {
    private final String messageId;
    private final String customerUrl;
}
