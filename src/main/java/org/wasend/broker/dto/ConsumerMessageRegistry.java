package org.wasend.broker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.wasend.broker.service.model.Message;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsumerMessageRegistry extends Message {
    private String host;
    // TODO навесить валидацию на порт
    private Integer port;
}
