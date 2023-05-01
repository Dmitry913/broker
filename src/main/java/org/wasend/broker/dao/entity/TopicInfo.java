package org.wasend.broker.dao.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Builder
@Getter
@Setter
public class TopicInfo {
    /** Наименование данного топика*/
    private String name;
    /** Идентификаторы узлов, в которых хранятся, копии данной реплики */
    private Set<String> replicasPlace;
    /** Идентификатор мастер-узла*/
    private String masterNodeName;
}
