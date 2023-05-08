package org.wasend.broker.dao.entity;

import lombok.Getter;

import java.util.Map;

/**
 * Информация, располагающаяся в главной директории.
 */
@Getter
public class MetaInfoZK {
    private Map<String, TopicInfo> topicNameToInfo;
//    /**
//     * Количество реплик для каждого топика (master-slave) - используется для согласованности данных
//     */
//    private int countReplicas;
    /**
     * Количество партиций для каждого топика (master-master) - используется для ускорения
     */
    private int countPartition;
    //  <PartitionId - MasterNodeDirectoryName>
    private Map<String, String> partitionIdToNodeDirectoryName;

    public void addNewTopic(TopicInfo info) {
        topicNameToInfo.put(info.getName(), info);
    }

    public void linkNewPartition(Map<String, String> partitionToNode) {
        partitionIdToNodeDirectoryName.putAll(partitionToNode);
    }
}
