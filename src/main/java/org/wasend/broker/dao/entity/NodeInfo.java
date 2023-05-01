package org.wasend.broker.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Информация об эфемерном узле. Используется для отслеживания статуса брокеров в системе.
 */
@Getter
@Setter
@NoArgsConstructor
public class NodeInfo {
    private String nodeId;
    private String host;
    private int port;
}
