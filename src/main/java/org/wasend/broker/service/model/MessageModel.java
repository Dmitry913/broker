package org.wasend.broker.service.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class MessageModel extends Message {
    private String partitionId;
    private String topicName;
    private boolean isReplica = false;
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
//    private int ask;
    /**
     * Получатели сообщений. Содержит в себе id существующих получателей
     */
    // todo организовать механизм синхронизации на всех нодах, при добавлении нового получателя
    private List<String> sendTo;

    @Override
    public String toString() {
        System.out.println(deadline);
        return String.format("{" +
                "\"partitionId\":\"%s\"," +
                " \"topicName\":\"%s\"," +
                " \"isReplica\":\"%b\"," +
                " \"payload\":\"%s\"," +
                " \"deadline\":\"%s\"," +
                " \"sendTo\":\"%s\"}",
                partitionId,
                topicName,
                isReplica,
                payload,
                deadline.toString(),
                sendTo.toString());
    }

}
