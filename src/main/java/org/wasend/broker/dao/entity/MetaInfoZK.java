package org.wasend.broker.dao.entity;

import lombok.Getter;

import java.util.Map;

/**
 * Информация, располагающаяся в главной директории.
 */
@Getter
public class MetaInfoZK {
    private Map<String, TopicInfo> topicNameToInfo;
    /**
     * Количество реплик для каждого топика
     */
    private int countReplicas;
}
