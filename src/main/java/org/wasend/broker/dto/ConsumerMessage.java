package org.wasend.broker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.wasend.broker.service.model.Message;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class ConsumerMessage {
    private final String host;
    // TODO навесить валидацию на порт
    private final Integer port;
}
