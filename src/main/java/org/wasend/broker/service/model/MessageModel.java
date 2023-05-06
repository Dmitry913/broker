package org.wasend.broker.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageModel extends Message {
    private String masterNode;
    private String topicName;
    private boolean isReplica = false;
    /**
     * передаваемая информация
     */
    // todo изменить string в byte[]
    private String payload;
    private LocalDateTime deadLine;
    /**
     * при ask = 0, producer не ждёт подтверждения
     * при ask = 1, producer ждёт подтверждения от leader-replica
     * при ask = 2, producer ждёт подтверждения от всех insync-replica
     */
    private int ask;
    /**
     * Получатели сообщений. Содержит в себе id существующих получателей
     */
    // todo организовать механизм синхронизации на всех нодах, при добавлении нового получателя
    private List<String> sendTo;
}
