package org.wasend.broker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.wasend.broker.service.model.Message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProducerMessage extends Message {
    private String topicName;
    /**
     * передаваемая информация
     */
    // todo изменить string в byte[]
    private String payload;
    private LocalDateTime deadline;
    /**
     * при ask = 0, producer не ждёт подтверждения
     * при ask = 1, producer ждёт подтверждения от leader-replica
     * при ask = 2, producer ждёт подтверждения от всех insync-replica
     */
    private Integer ask;
    /**
     * Получатели сообщений. Содержит в себе id существующих получателей
     */
    // todo организовать механизм синхронизации на всех нодах, при добавлении нового получателя
    private List<String> sendTo;
}
