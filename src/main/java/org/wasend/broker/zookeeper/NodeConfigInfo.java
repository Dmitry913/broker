package org.wasend.broker.zookeeper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Данный класс представляет собой информацию, которая хранится в каждой ноде zooKeeper.
 * Нода - это экземпляр брокера
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class NodeConfigInfo implements Serializable {
    private String host;
    private Integer port;
    /**
     * Идентификаторы брокеров, где хранятся реплики, для которых я мастер.
     * Здесь key - название топика; value - названия нод, на которых хранятся реплики
     */
    private Map<String, List<String>> topicToNodeIdOfReplicas;
    /**
     * идентификаторы брокеров, где хранятся реплики, для которых я мастер
     */
    private List<String> nodeIdOfStoredReplicas;

    @Override
    public String toString() {
        return "Host - " + host + "\n" +
                "Port - " + port + "\n" +
                "TopicToNodeIdOfReplicas - " + topicToNodeIdOfReplicas + "\n" +
                "NodeIdOfStoredReplicas - " + nodeIdOfStoredReplicas + "\n";
    }

}