package org.wasend.broker.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Информация, располагающаяся в главной директории.
 */
@Getter
@AllArgsConstructor
@Setter
@Builder
@NoArgsConstructor
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

    public MetaInfoZK(MetaInfoZK otherMetaInfoZk) {
        this.topicNameToInfo = new HashMap<>(otherMetaInfoZk.getTopicNameToInfo());
        this.countPartition = otherMetaInfoZk.getCountPartition();
        this.partitionIdToNodeDirectoryName = new HashMap<>(otherMetaInfoZk.getPartitionIdToNodeDirectoryName());
    }
}
