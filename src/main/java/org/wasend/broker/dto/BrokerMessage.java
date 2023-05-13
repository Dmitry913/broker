package org.wasend.broker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.wasend.broker.service.model.Message;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BrokerMessage extends Message {
    private ProducerMessage producerMessage;
    private String partitionId;
}
