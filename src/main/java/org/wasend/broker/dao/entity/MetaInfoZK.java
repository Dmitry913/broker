package org.wasend.broker.dao.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Информация, располагающаяся в главной директории.
 */
public class MetaInfoZK {
    private Map<String, TopicInfo> topicNameToInfo;
    /**Количество реплик для каждого топика*/
    private int countReplicas;
}
