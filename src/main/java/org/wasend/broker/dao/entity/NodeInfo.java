package org.wasend.broker.dao.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.Serializable;

/**
 * Информация об эфемерном узле. Используется для отслеживания статуса брокеров в системе.
 */
@Getter
@AllArgsConstructor
@Setter
@Builder
@NoArgsConstructor
public class NodeInfo implements Serializable {
    private String nodeId;
    private String host;
    private int port;

    @SneakyThrows
    @Override
    public String toString() {
        return new ObjectMapper().writeValueAsString(this);
    }
}
